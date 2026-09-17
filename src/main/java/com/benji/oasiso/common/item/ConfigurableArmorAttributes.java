package com.benji.oasiso.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ConfigurableArmorAttributes {

    private static final Map<ArmorItem.Type, UUID> MODIFIER_UUIDS = Map.of(
            ArmorItem.Type.BOOTS, UUID.fromString("32d98cf8-6e09-43c5-b97e-f4f85da928c7"),
            ArmorItem.Type.LEGGINGS, UUID.fromString("665c03a9-4c90-494b-9439-e797af3c579f"),
            ArmorItem.Type.CHESTPLATE, UUID.fromString("faf12d33-ea18-4c9e-94bc-d813f746e634"),
            ArmorItem.Type.HELMET, UUID.fromString("01b4bd80-eac0-4d81-a646-485bdaedbf0a")
    );

    private static final Map<ArmorStats, Multimap<Attribute, AttributeModifier>> CACHE = new ConcurrentHashMap<>();

    private ConfigurableArmorAttributes() {
    }

    static Multimap<Attribute, AttributeModifier> create(ArmorItem.Type type, int defense, double toughness, double knockbackResistance) {
        ArmorStats stats = new ArmorStats(type, defense, toughness, knockbackResistance);
        return CACHE.computeIfAbsent(stats, ConfigurableArmorAttributes::build);
    }

    private static Multimap<Attribute, AttributeModifier> build(ArmorStats stats) {
        UUID uuid = MODIFIER_UUIDS.get(stats.type());
        ImmutableMultimap.Builder<Attribute, AttributeModifier> attributes = ImmutableMultimap.builder();

        attributes.put(Attributes.ARMOR, new AttributeModifier(
                uuid,
                "Oasiso configurable armor",
                stats.defense(),
                AttributeModifier.Operation.ADDITION
        ));
        attributes.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                uuid,
                "Oasiso configurable armor toughness",
                stats.toughness(),
                AttributeModifier.Operation.ADDITION
        ));

        if (stats.knockbackResistance() > 0.0D) {
            attributes.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    uuid,
                    "Oasiso armor knockback resistance",
                    stats.knockbackResistance(),
                    AttributeModifier.Operation.ADDITION
            ));
        }

        return attributes.build();
    }

    private record ArmorStats(ArmorItem.Type type, int defense, double toughness, double knockbackResistance) {
    }
}
