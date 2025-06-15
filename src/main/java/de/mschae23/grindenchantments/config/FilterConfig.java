/*
 * Copyright (C) 2024  mschae23
 *
 * This file is part of Grind enchantments.
 *
 * Grind enchantments is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.mschae23.grindenchantments.config;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.mschae23.grindenchantments.CodecUtils;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import org.apache.logging.log4j.Level;

public record FilterConfig(boolean enabled, ItemConfig item, EnchantmentConfig enchantment, FilterAction curses) {
    public static final Codec<FilterConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(FilterConfig::enabled),
        ItemConfig.CODEC.fieldOf("item").forGetter(FilterConfig::item),
        EnchantmentConfig.CODEC.fieldOf("enchantment").forGetter(FilterConfig::enchantment),
        FilterAction.CODEC.fieldOf("cursed_enchantments").forGetter(FilterConfig::curses)
    ).apply(instance, instance.stable(FilterConfig::new)));

    public static final FilterConfig DEFAULT = new FilterConfig(true, ItemConfig.DEFAULT, EnchantmentConfig.DEFAULT, FilterAction.IGNORE);
    public static final FilterConfig DISABLED = new FilterConfig(false, ItemConfig.DEFAULT, EnchantmentConfig.DEFAULT, FilterAction.IGNORE);

    public static StreamCodec<FriendlyByteBuf, FilterConfig> createPacketCodec() {
        return StreamCodec.composite(
            ByteBufCodecs.BOOL, FilterConfig::enabled,
            ItemConfig.createPacketCodec(), FilterConfig::item,
            EnchantmentConfig.createPacketCodec(), FilterConfig::enchantment,
            FilterAction.PACKET_CODEC, FilterConfig::curses,
            FilterConfig::new
        );
    }

    private boolean shouldDeny(ItemEnchantments.Mutable builder) {
        if (this.curses == FilterAction.DENY) {
            for (Holder<Enchantment> entry : builder.keySet()) {
                if (entry.is(EnchantmentTags.CURSE)) {
                    return true;
                }
            }
        }

        if (this.enchantment.action == FilterAction.DENY) {
            for (Holder<Enchantment> entry : builder.keySet()) {
                if (entry.unwrapKey().map(key -> this.enchantment.enchantments.contains(key.location())).orElse(false)) {
                    return true;
                }
            }
        }

        return false;
    }

    public ItemEnchantments filter(ItemEnchantments enchantments) {
        if (!this.enabled) {
            return enchantments;
        }

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchantments);

        if (this.shouldDeny(builder)) {
            return ItemEnchantments.EMPTY;
        }

        builder.removeIf(enchantment ->
            ((this.curses == FilterAction.IGNORE) && enchantment.is(EnchantmentTags.CURSE))
            || ((this.enchantment.action == FilterAction.IGNORE) == enchantment.unwrapKey().map(key ->
                this.enchantment.enchantments.contains(key.location())).orElse(false)));

        return builder.toImmutable();
    }

    public ItemEnchantments filterReversed(ItemEnchantments enchantments) {
        if (!this.enabled) {
            return ItemEnchantments.EMPTY;
        }

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchantments);

        if (this.shouldDeny(builder)) {
            return ItemEnchantments.EMPTY;
        }

        builder.removeIf(enchantment ->
            ((this.curses == FilterAction.ALLOW) || !enchantment.is(EnchantmentTags.CURSE))
            && ((this.enchantment.action == FilterAction.ALLOW) == enchantment.unwrapKey().map(key ->
                this.enchantment.enchantments.contains(key.location())).orElse(false)));

        return builder.toImmutable();
    }

    public void validateRegistryEntries(HolderLookup.Provider wrapperLookup) {
        this.item.validateRegistryEntries(wrapperLookup);
        this.enchantment.validateRegistryEntries(wrapperLookup);
    }

    @Override
    public String toString() {
        return "FilterConfig{" +
            "enabled=" + this.enabled +
            ", item=" + this.item +
            ", enchantment=" + this.enchantment +
            ", curses=" + this.curses +
            '}';
    }

    public record ItemConfig(List<ResourceLocation> items, FilterAction action) {
        public static final Codec<ItemConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtils.listOrSingle(ResourceLocation.CODEC).fieldOf("enchantments").forGetter(ItemConfig::items),
            FilterAction.NON_IGNORE_CODEC.fieldOf("action").forGetter(ItemConfig::action)
        ).apply(instance, instance.stable(ItemConfig::new)));

        public static final ItemConfig DEFAULT = new ItemConfig(List.of(), FilterAction.DENY);

        public static StreamCodec<FriendlyByteBuf, ItemConfig> createPacketCodec() {
            return StreamCodec.composite(
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemConfig::items,
                FilterAction.PACKET_CODEC, ItemConfig::action,
                ItemConfig::new
            );
        }

        public void validateRegistryEntries(HolderLookup.Provider wrapperLookup) {
            Optional<? extends HolderLookup.RegistryLookup<Item>> registryWrapperOpt = wrapperLookup.lookup(Registries.ITEM);

            if (registryWrapperOpt.isEmpty()) {
                GrindEnchantmentsMod.LOGGER.warn("Item registry is not present");
                return;
            }

            HolderLookup.RegistryLookup<Item> registryWrapper = registryWrapperOpt.get();

            this.items.stream()
                .map(item -> Pair.of(item, registryWrapper.get(ResourceKey.create(Registries.ITEM, item))))
                .flatMap(result -> result.getSecond().isEmpty() ? Stream.of(result.getFirst()) : Stream.empty())
                .map(ResourceLocation::toString)
                .forEach(item -> GrindEnchantmentsMod.LOGGER.warn("Filter config contains unknown item: {}", item));
        }

        @Override
        public String toString() {
            return "ItemConfig{" +
                "items=" + this.items +
                ", action=" + this.action +
                '}';
        }
    }

    public record EnchantmentConfig(List<ResourceLocation> enchantments, FilterAction action) {
        public static final Codec<EnchantmentConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CodecUtils.listOrSingle(ResourceLocation.CODEC).fieldOf("enchantments").forGetter(EnchantmentConfig::enchantments),
            FilterAction.CODEC.fieldOf("action").forGetter(EnchantmentConfig::action)
        ).apply(instance, instance.stable(EnchantmentConfig::new)));

        public static final EnchantmentConfig DEFAULT = new EnchantmentConfig(List.of(), FilterAction.IGNORE);

        public static StreamCodec<FriendlyByteBuf, EnchantmentConfig> createPacketCodec() {
            return StreamCodec.composite(
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), EnchantmentConfig::enchantments,
                FilterAction.PACKET_CODEC, EnchantmentConfig::action,
                EnchantmentConfig::new
            );
        }

        public void validateRegistryEntries(HolderLookup.Provider wrapperLookup) {
            Optional<? extends HolderLookup.RegistryLookup<Enchantment>> registryWrapperOpt = wrapperLookup.lookup(Registries.ENCHANTMENT);

            if (registryWrapperOpt.isEmpty()) {
                GrindEnchantmentsMod.LOGGER.warn("Enchantment registry is not present");
                return;
            }

            HolderLookup.RegistryLookup<Enchantment> registryWrapper = registryWrapperOpt.get();

            this.enchantments.stream()
                .map(enchantment -> Pair.of(enchantment, registryWrapper.get(ResourceKey.create(Registries.ENCHANTMENT, enchantment))))
                .flatMap(result -> result.getSecond().isEmpty() ? Stream.of(result.getFirst()) : Stream.empty())
                .map(ResourceLocation::toString)
                .forEach(item -> GrindEnchantmentsMod.LOGGER.warn("Filter config contains unknown enchantment: {}", item));
        }

        @Override
        public String toString() {
            return "EnchantmentConfig{" +
                "enchantments=" + this.enchantments +
                ", action=" + this.action +
                '}';
        }
    }
}
