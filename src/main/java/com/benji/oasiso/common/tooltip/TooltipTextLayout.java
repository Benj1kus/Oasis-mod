package com.benji.oasiso.common.tooltip;

import java.util.*;
import java.util.function.Function;

import static com.benji.oasiso.common.tooltip.TooltipData.*;

public final class TooltipTextLayout {
    public record Span(String text, Fx style) {
    }

    private record Match(int start, int end, String style) {
    }

    public static List<Span> build(Definition definition, Line line, Function<Text, String> resolve) {
        List<Span> spans = new ArrayList<>();
        if (!line.parts().isEmpty()) {
            for (Part p : line.parts())
                spans.add(new Span(resolve.apply(p.text()), definition.styles().get(p.style())));
            return List.copyOf(spans);
        }
        String full = resolve.apply(line.text());
        List<Match> matches = new ArrayList<>();
        for (Highlight h : line.highlights()) {
            String selected = resolve.apply(h.match());
            if (selected.isEmpty()) selected = h.emptyFallback();
            int start = selected.isEmpty() ? -1 : full.indexOf(selected);
            if (start < 0 && !h.fallback().isEmpty()) {
                selected = h.fallback();
                start = full.indexOf(selected);
            }
            if (start < 0) {
                if (line.requireAll()) return List.of(new Span(full, definition.styles().get(line.missingStyle())));
                continue;
            }
            do {
                matches.add(new Match(start, start + selected.length(), h.style()));
                start = h.all() ? full.indexOf(selected, start + selected.length()) : -1;
            } while (start >= 0 && matches.size() < 256);
        }
        matches.sort(Comparator.comparingInt(Match::start));
        int cursor = 0;
        for (Match match : matches) {
            if (match.start() < cursor) {
                if (line.requireAll()) return List.of(new Span(full, definition.styles().get(line.missingStyle())));
                continue;
            }
            if (match.start() > cursor)
                spans.add(new Span(full.substring(cursor, match.start()), definition.styles().get(line.textStyle())));
            spans.add(new Span(full.substring(match.start(), match.end()), definition.styles().get(match.style())));
            cursor = match.end();
        }
        if (cursor < full.length() || spans.isEmpty())
            spans.add(new Span(full.substring(cursor), definition.styles().get(line.textStyle())));
        return List.copyOf(spans);
    }

    private TooltipTextLayout() {
    }
}
