package com.benji.oasiso.client.configscroll;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ConfigScrollModel {
    final ForgeConfigSpec spec;
    final List<Category> categories = new ArrayList<>();

    ConfigScrollModel(ForgeConfigSpec spec) {
        this.spec = spec;
        if (!spec.isLoaded()) throw new IllegalStateException("Oops The config has not loaded yet:P");
        Map<String, Category> groups = new LinkedHashMap<>();
        collect(spec.getValues(), groups);
        categories.addAll(groups.values());
        categories.sort(Comparator.comparing(c -> c.name));
    }

    private void collect(UnmodifiableConfig tree, Map<String, Category> groups) {
        List<UnmodifiableConfig.Entry> children = new ArrayList<>(tree.entrySet());
        children.sort(Comparator.comparing(UnmodifiableConfig.Entry::getKey));
        for (UnmodifiableConfig.Entry child : children) {
            Object value = child.getValue();
            if (value instanceof UnmodifiableConfig nested) {
                collect(nested, groups);
            } else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                List<String> path = configValue.getPath();
                String section = path.size() > 1 ? String.join(" / ", path.subList(0, path.size() - 1)) : "General";
                ForgeConfigSpec.ValueSpec definition = spec.getSpec().get(path);
                groups.computeIfAbsent(section, Category::new).entries.add(new Entry(configValue, definition));
            }
        }
    }

    boolean dirty() {
        return categories.stream().flatMap(c -> c.entries.stream()).anyMatch(Entry::dirty);
    }

    Entry firstInvalid() {
        return categories.stream().flatMap(c -> c.entries.stream()).filter(e -> e.error != null).findFirst().orElse(null);
    }

    void save() {
        Entry invalid = firstInvalid();
        if (invalid != null) throw new IllegalStateException(invalid.label + ": " + invalid.error);
        List<Entry> changed = categories.stream().flatMap(c -> c.entries.stream()).filter(Entry::dirty).toList();
        for (Entry entry : changed) {
            if (!Objects.equals(entry.original, entry.value.get())) {
                throw new IllegalStateException("Config changed outside this screen. Discard and reopen it before saving.");
            }
        }
        try {
            for (Entry entry : changed) entry.write(entry.draft);
            if (!changed.isEmpty()) spec.save();
        } catch (RuntimeException failure) {
            for (Entry entry : changed) {
                try { entry.write(entry.original); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            }
            try { spec.save(); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
        for (Entry entry : changed) entry.original = copy(entry.draft);
    }

    static Object copy(Object value) {
        return value instanceof List<?> list ? new ArrayList<>(list) : value;
    }

    static String label(String key) {
        String text = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
        return text.isEmpty() ? key : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    static final class Category {
        final String name;
        final List<Entry> entries = new ArrayList<>();
        Category(String name) { this.name = name; }
    }

    static final class Entry {
        final ForgeConfigSpec.ConfigValue<?> value;
        final ForgeConfigSpec.ValueSpec definition;
        final String label;
        final String comment;
        final boolean numeric;
        final boolean list;
        final boolean bool;
        final double min;
        final double max;
        final boolean curved;
        Object original;
        Object draft;
        String text;
        String error;

        Entry(ForgeConfigSpec.ConfigValue<?> value, ForgeConfigSpec.ValueSpec definition) {
            this.value = value;
            this.definition = definition;
            this.original = copy(value.get());
            this.draft = copy(original);
            this.label = label(value.getPath().get(value.getPath().size() - 1));
            this.comment = definition.getComment() == null ? "" : definition.getComment();
            this.numeric = draft instanceof Number;
            this.list = draft instanceof List<?>;
            this.bool = draft instanceof Boolean;
            var range = definition.getRange();
            this.min = range != null && range.getMin() instanceof Number n ? n.doubleValue() : 0;
            this.max = range != null && range.getMax() instanceof Number n ? n.doubleValue() : 100;
            this.curved = numeric && max - min > 1000;
            this.text = format(draft);
        }

        boolean dirty() { return !Objects.equals(original, draft) || error != null; }
        String format(Object object) {
            if (object instanceof Integer || object instanceof Long) return object.toString();
            return object instanceof Number n ? number(n.doubleValue()) : String.valueOf(object);
        }
        boolean integral() { return original instanceof Integer || original instanceof Long; }

        void setText(String input) {
            text = input;
            try {
                Object candidate;
                if (original instanceof Integer) candidate = new BigDecimal(input.trim().replace(',', '.')).intValueExact();
                else if (original instanceof Long) candidate = new BigDecimal(input.trim().replace(',', '.')).longValueExact();
                else if (original instanceof Number) {
                    double n = Double.parseDouble(input.trim().replace(',', '.'));
                    if (!Double.isFinite(n)) throw new IllegalArgumentException();
                    candidate = n;
                } else candidate = input;
                if (!accepts(candidate)) throw new IllegalArgumentException();
                draft = candidate;
                error = null;
            } catch (RuntimeException ex) {
                error = numeric ? (integral() ? "Whole number: " : "Number: ") + number(min) + " ... " + number(max) : "Invalid value";
            }
        }

        boolean set(Object candidate) {
            if (!accepts(candidate)) return false;
            draft = copy(candidate);
            text = format(draft);
            error = null;
            return true;
        }

        boolean accepts(Object candidate) {
            return definition.test(candidate) && Objects.equals(candidate, definition.correct(copy(candidate)));
        }

        void reset() { set(definition.getDefault()); }

        double transform(double n) { return curved ? Math.copySign(Math.log1p(Math.abs(n)), n) : n; }
        double inverse(double n) { return curved ? Math.copySign(Math.expm1(Math.abs(n)), n) : n; }
        double fraction() {
            double span = transform(max) - transform(min);
            return span <= 0 ? 0 : Math.max(0, Math.min(1, (transform(((Number) draft).doubleValue()) - transform(min)) / span));
        }
        void fromSlider(double position) {
            double p = Math.max(0, Math.min(1, position));
            double n = inverse(transform(min) + p * (transform(max) - transform(min)));
            n = Math.max(min, Math.min(max, integral() ? Math.round(n) : Math.round(n * 1000) / 1000.0));
            setText(integral() ? Long.toString(Math.round(n)) : number(n));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void write(Object candidate) { ((ForgeConfigSpec.ConfigValue) value).set(copy(candidate)); }
    }
}
