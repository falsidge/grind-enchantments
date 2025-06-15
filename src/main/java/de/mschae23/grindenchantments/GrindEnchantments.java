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

package de.mschae23.grindenchantments;

import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import de.mschae23.grindenchantments.config.DedicatedServerConfig;
import de.mschae23.grindenchantments.config.FilterConfig;
import de.mschae23.grindenchantments.cost.CostFunction;

public class GrindEnchantments {
    public static int getLevelCost(ItemStack stack, CostFunction costFunction, FilterConfig filter, HolderLookup.Provider wrapperLookup) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        double cost = costFunction.getCost(enchantments, filter, wrapperLookup);

        return (int) Math.ceil(cost);
    }

    public static ItemEnchantments getEnchantments(ItemStack stack, FilterConfig filter) {
        return filter.filter(EnchantmentHelper.getEnchantmentsForCrafting(stack));
    }

    public static ItemStack addLevelCostComponent(ItemStack stack, IntSupplier cost, boolean canTakeItem, DedicatedServerConfig config) {
        if (!config.alternativeCostDisplay())
            return stack;

        ItemStack changed = stack.copy();
        changed.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, nbt -> nbt.update(compound -> {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Cost", cost.getAsInt());
            tag.putBoolean("CanTake", canTakeItem);

            compound.put(GrindEnchantmentsMod.MODID, tag);
        }));
        return changed;
    }

    public static ItemStack addLevelCostLore(ItemStack stack, IntSupplier cost, boolean canTakeItem) {
        MutableComponent text = Component.literal("Enchantment cost: " + cost.getAsInt())
            .withStyle(canTakeItem ? ChatFormatting.GREEN : ChatFormatting.RED);

        stack.update(DataComponents.LORE, ItemLore.EMPTY, lore -> lore.withLineAdded(text));
        return stack;
    }

    /**
     * @param stack mutable; same instance will be returned
     * @return the {@code stack} argument
     */
    @SuppressWarnings("UnusedReturnValue")
    public static ItemStack removeLevelCostNbt(ItemStack stack) {
        // Relies on ItemStacks being mutable AND the stack not being copied into the player inventory before calling this method

        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, nbt -> nbt.update(compound ->
            compound.remove(GrindEnchantmentsMod.MODID)));
        return stack;
    }
}
