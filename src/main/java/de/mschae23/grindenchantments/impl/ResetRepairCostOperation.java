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

package de.mschae23.grindenchantments.impl;

import java.util.function.IntSupplier;

import de.mschae23.grindenchantments.GrindEnchantments;
import de.mschae23.grindenchantments.event.GrindstoneEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.config.FilterAction;
import de.mschae23.grindenchantments.config.FilterConfig;
import de.mschae23.grindenchantments.config.ResetRepairCostConfig;
import de.mschae23.grindenchantments.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

public class ResetRepairCostOperation implements Operation{
    @Override
    public boolean canInsert(ItemStack stack, ItemStack other, int slotId) {
        ResetRepairCostConfig config = GrindEnchantmentsMod.getServerConfig().resetRepairCost();

        return config.enabled() && slotId == 1 && stack.getItemHolder().unwrapKey().map(key -> config.catalystItems().contains(key.location())).orElse(false);
    }

    @Override
    public @NotNull ItemStack onUpdateResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (!isOperation(input1, input2)) {
            return ItemStack.EMPTY;
        }

        ServerConfig config = GrindEnchantmentsMod.getServerConfig();
        FilterConfig filter = config.filter();

        if (filter.enabled() && filter.item().action() != FilterAction.IGNORE
            && (filter.item().action() == FilterAction.DENY) == input1.getItemHolder().unwrapKey().map(key -> filter.item().items().contains(key.location())).orElse(false)) {
            return ItemStack.EMPTY;
        }

        if (input1.getOrDefault(DataComponents.REPAIR_COST, 0) <= 0) {
            return ItemStack.EMPTY;
        } else if (config.resetRepairCost().requiresEnchantment() && !EnchantmentHelper.hasAnyEnchantments(input1)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = input1.copy();
        result.remove(DataComponents.REPAIR_COST);

        return result;
    }

    @Override
    public boolean canTakeResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (isOperation(input1, input2)) {
            ServerConfig config = GrindEnchantmentsMod.getServerConfig();

            return canTakeResult(input1, input2, () ->
                GrindEnchantments.getLevelCost(input1, config.resetRepairCost().costFunction(), config.filter(), wrapperLookup), player);
        }

        return true;
    }

    @Override
    public boolean onTakeResult(ItemStack input1, ItemStack input2, ItemStack resultStack, Player player, Container input, HolderLookup.Provider wrapperLookup) {
        if (!isOperation(input1, input2)) {
            return false;
        }

        ServerConfig config = GrindEnchantmentsMod.getServerConfig();
        FilterConfig filter = config.filter();

        input.setItem(0, ItemStack.EMPTY);

        if (input2.getCount() == 1)
            input.setItem(1, ItemStack.EMPTY);
        else {
            ItemStack newCatalystStack = input2.copy();
            newCatalystStack.setCount(input2.getCount() - 1);
            input.setItem(1, newCatalystStack);
        }

        if (!player.getAbilities().instabuild) {
            int cost = GrindEnchantments.getLevelCost(input1, config.resetRepairCost().costFunction(), filter, wrapperLookup);
            GrindstoneEvents.applyLevelCost(cost, player);
        }
        return true;
    }

    @Override
    public int getLevelCost(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (isOperation(input1, input2)) {
            ServerConfig config = GrindEnchantmentsMod.getServerConfig();

            return GrindEnchantments.getLevelCost(input1, config.resetRepairCost().costFunction(), config.filter(), wrapperLookup);
        }

        return -1;
    }

    public boolean isOperation(ItemStack input1, ItemStack input2) {
        ResetRepairCostConfig config = GrindEnchantmentsMod.getServerConfig().resetRepairCost();

        if (!config.enabled())
            return false;

        return (input1.isDamageableItem() || EnchantmentHelper.canStoreEnchantments(input1))
            && !input2.is(Items.BOOK) && !input2.is(Items.ENCHANTED_BOOK) && !input2.isDamageableItem() && !input2.is(input1.getItem())
            && input2.getItemHolder().unwrapKey().map(key -> config.catalystItems().contains(key.location())).orElse(false);
    }

    public static boolean canTakeResult(@SuppressWarnings("unused") ItemStack input1, @SuppressWarnings("unused") ItemStack input2,
                                        IntSupplier cost, Player player) {
        return player.getAbilities().instabuild || player.experienceLevel >= cost.getAsInt();
    }
}
