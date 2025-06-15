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
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.config.FilterAction;
import de.mschae23.grindenchantments.config.FilterConfig;
import de.mschae23.grindenchantments.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

public class DisenchantOperation implements Operation{
    @Override
    public boolean canInsert(ItemStack stack, ItemStack other, int slotId) {
        return stack.getItem() == Items.BOOK && !other.is(Items.BOOK);
    }

    @Override
    public @NotNull ItemStack onUpdateResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (!isOperation(input1, input2)) {
            return ItemStack.EMPTY;
        }

        ItemStack enchantedItemStack = input1.isEnchanted() ? input1 : input2;

        ServerConfig config = GrindEnchantmentsMod.getServerConfig();
        return transferEnchantmentsToBook(enchantedItemStack, config.filter());
    }

    @Override
    public boolean canTakeResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (isOperation(input1, input2)) {
            ServerConfig config = GrindEnchantmentsMod.getServerConfig();

            boolean stack1Book = input1.getItem() == Items.BOOK;
            ItemStack enchantedItemStack = stack1Book ? input2 : input1;

            return canTakeResult(input1, input2, () ->
                GrindEnchantments.getLevelCost(enchantedItemStack, config.disenchant().costFunction(), config.filter(), wrapperLookup), player);
        }

        return true;
    }

    @Override
    public boolean onTakeResult(ItemStack input1, ItemStack input2, ItemStack resultStack, Player player, Container input, HolderLookup.Provider wrapperLookup) {
        if (!isOperation(input1, input2)) {
            return false;
        }

        ServerConfig config = GrindEnchantmentsMod.getServerConfig();

        boolean stack1Book = input1.getItem() == Items.BOOK;
        ItemStack enchantedItemStack = stack1Book ? input2 : input1;
        ItemStack bookItemStack = stack1Book ? input1 : input2;

        if (!player.getAbilities().instabuild) {
            int cost = debugLevelCost("onTakeResult", GrindEnchantments.getLevelCost(enchantedItemStack, config.disenchant().costFunction(), config.filter(), wrapperLookup));
            GrindstoneEvents.applyLevelCost(cost, player);
        }

        input.setItem(stack1Book ? 1 : 0, config.disenchant().consumeItem() ?
            ItemStack.EMPTY : grind(enchantedItemStack, config.filter()));

        if (bookItemStack.getCount() == 1)
            input.setItem(stack1Book ? 0 : 1, ItemStack.EMPTY);
        else {
            ItemStack bookNew = bookItemStack.copy();
            bookNew.setCount(bookItemStack.getCount() - 1);
            input.setItem(stack1Book ? 0 : 1, bookNew);
        }

        return true;
    }

    @Override
    public int getLevelCost(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (isOperation(input1, input2)) {
            ServerConfig config = GrindEnchantmentsMod.getServerConfig();
            boolean stack1Book = input1.getItem() == Items.BOOK;
            ItemStack enchantedItemStack = stack1Book ? input2 : input1;

            return debugLevelCost("getLevelCost", GrindEnchantments.getLevelCost(enchantedItemStack, config.disenchant().costFunction(), config.filter(), wrapperLookup));
        }

        return -1;
    }

    private static int debugLevelCost(@SuppressWarnings("unused") String location, int cost) {
        // GrindEnchantmentsMod.log(Level.INFO, "Level cost (" + location + "): " + cost);
        return cost;
    }

    public  boolean isOperation(ItemStack input1, ItemStack input2) {
        if (!GrindEnchantmentsMod.getServerConfig().disenchant().enabled())
            return false;

        return input1.isEnchanted() && input2.getItem() == Items.BOOK
            || input2.isEnchanted() && input1.getItem() == Items.BOOK;
    }

    public static boolean canTakeResult(@SuppressWarnings("unused") ItemStack input1, @SuppressWarnings("unused") ItemStack input2,
                                        IntSupplier cost, Player player) {
        return player.getAbilities().instabuild || player.experienceLevel >= cost.getAsInt();
    }

    public static ItemStack transferEnchantmentsToBook(ItemStack source, FilterConfig filter) {
        if (filter.enabled() && filter.item().action() != FilterAction.IGNORE
            && (filter.item().action() == FilterAction.DENY) == source.getItemHolder().unwrapKey().map(key -> filter.item().items().contains(key.location())).orElse(false)) {
            return ItemStack.EMPTY;
        }

        ItemEnchantments enchantments = GrindEnchantments.getEnchantments(source, filter);

        if (enchantments.isEmpty())
            return ItemStack.EMPTY;

        int repairCost = 0;

        for(int i = 0; i < enchantments.size(); i++) {
            repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments);
        book.set(DataComponents.REPAIR_COST, repairCost);
        return book;
    }

    private static ItemStack grind(ItemStack stack, FilterConfig filter) {
        ItemStack stack2 = stack.copy();
        ItemEnchantments enchantments = filter.filterReversed(stack2.getOrDefault(EnchantmentHelper.getComponentType(stack2),
            ItemEnchantments.EMPTY));
        stack2.set(EnchantmentHelper.getComponentType(stack2), enchantments);

        if (stack2.is(Items.ENCHANTED_BOOK) && enchantments.isEmpty()) {
            stack2 = stack2.transmuteCopy(Items.BOOK, stack2.getCount());
        }

        int repairCost = 0;

        for(int j = 0; j < enchantments.size(); ++j) {
            repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }

        stack2.set(DataComponents.REPAIR_COST, repairCost);
        return stack2;
    }
}
