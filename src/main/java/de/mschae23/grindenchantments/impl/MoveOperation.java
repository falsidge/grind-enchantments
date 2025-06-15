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

import java.util.Optional;
import java.util.function.IntSupplier;

import de.mschae23.grindenchantments.GrindEnchantments;
import de.mschae23.grindenchantments.event.GrindstoneEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.config.FilterAction;
import de.mschae23.grindenchantments.config.FilterConfig;
import de.mschae23.grindenchantments.config.ServerConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MoveOperation implements Operation {
    @Override
    public boolean canInsert(ItemStack stack, ItemStack other, int slotId) {
        return stack.getItem() == Items.BOOK && !other.is(Items.BOOK);
    }

    @Override
    public @NotNull ItemStack onUpdateResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (!isOperation(input1, input2)) {
            return ItemStack.EMPTY;
        }

        FilterConfig filter = GrindEnchantmentsMod.getServerConfig().filter();

        if (filter.enabled() && filter.item().action() != FilterAction.IGNORE
            && (filter.item().action() == FilterAction.DENY) == input1.getItemHolder().unwrapKey().map(key -> filter.item().items().contains(key.location())).orElse(false)) {
            return ItemStack.EMPTY;
        }
        ItemEnchantments enchantments = GrindEnchantments.getEnchantments(input1, filter);
        ObjectIntPair<Holder<Enchantment>> firstEnchantment = getFirstEnchantment(enchantments, wrapperLookup);

        if (firstEnchantment == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = input2.copy();
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        int targetLevel = builder.getLevel(firstEnchantment.left());

        if (targetLevel > 0) {
            if (targetLevel != firstEnchantment.rightInt() || firstEnchantment.rightInt() == firstEnchantment.left().value().getMaxLevel()) {
                return ItemStack.EMPTY;
            } else {
                targetLevel += 1;
            }
        } else {
            targetLevel = firstEnchantment.rightInt();
        }

        builder.upgrade(firstEnchantment.left(), targetLevel);

        if (result.getItem() == Items.BOOK) {
            result = result.transmuteCopy(Items.ENCHANTED_BOOK, 1);
        }

        int repairCost = 0;

        for(int i = 0; i < builder.keySet().size(); i++) {
            repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }

        result.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
        result.set(DataComponents.REPAIR_COST, repairCost);
        return result;
    }

    @Override
    public boolean canTakeResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (isOperation(input1, input2)) {
            ServerConfig config = GrindEnchantmentsMod.getServerConfig();

            return canTakeResult(input1, input2, () ->
                GrindEnchantments.getLevelCost(input1, config.move().costFunction(), config.filter(), wrapperLookup), player);
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

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(input1);
        ObjectIntPair<Holder<Enchantment>> firstEnchantment = getFirstEnchantment(filter.filter(enchantments), wrapperLookup);

        if (firstEnchantment == null) {
            return false;
        }

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchantments);
        builder.removeIf(enchantment -> enchantment.value().equals(firstEnchantment.left().value()));

        ItemStack resultingInput1 = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.setEnchantments(resultingInput1, builder.toImmutable());
        resultingInput1.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(input1.getOrDefault(DataComponents.REPAIR_COST, 0)));

        input.setItem(0, resultingInput1);

        if (input2.getItem() == Items.ENCHANTED_BOOK || input2.getCount() == 1)
            input.setItem(1, ItemStack.EMPTY);
        else {
            ItemStack newBookStack = input2.copy();
            newBookStack.setCount(input2.getCount() - 1);
            input.setItem(1, newBookStack);
        }

        if (!player.getAbilities().instabuild) {
            int cost = GrindEnchantments.getLevelCost(input1, config.move().costFunction(), filter, wrapperLookup);
            GrindstoneEvents.applyLevelCost(cost, player);
        }

        return true;
    }

    @Override
    public int getLevelCost(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup) {
        if (isOperation(input1, input2)) {
            ServerConfig config = GrindEnchantmentsMod.getServerConfig();

            return GrindEnchantments.getLevelCost(input1, config.move().costFunction(), config.filter(), wrapperLookup);
        }

        return -1;
    }

    public boolean isOperation(ItemStack input1, ItemStack input2) {
        if (!GrindEnchantmentsMod.getServerConfig().move().enabled())
            return false;

        return input1.getItem() == Items.ENCHANTED_BOOK &&
            (input2.getItem() == Items.ENCHANTED_BOOK ||
                input2.getItem() == Items.BOOK);
    }

    public static boolean canTakeResult(@SuppressWarnings("unused") ItemStack input1, @SuppressWarnings("unused") ItemStack input2,
                                        IntSupplier cost, Player player) {
        return player.getAbilities().instabuild || player.experienceLevel >= cost.getAsInt();
    }

    @Nullable
    public static ObjectIntPair<Holder<Enchantment>> getFirstEnchantment(ItemEnchantments enchantments, HolderLookup.Provider wrapperLookup) {
        if (enchantments.size() < 2) {
            return null;
        }
        ObjectIntPair<Holder<Enchantment>> firstEnchantment = null;

        @Nullable
        Optional<HolderSet.Named<Enchantment>> optional = wrapperLookup.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.TOOLTIP_ORDER);
        if (optional.isEmpty()) {
            return null;
        }
        HolderSet<Enchantment> tooltipOrder = optional.get();

        for (Holder<Enchantment> entry : tooltipOrder) {
            int level = enchantments.getLevel(entry);

            if (level > 0) {
                firstEnchantment = ObjectIntPair.of(entry, level);
                break;
            }
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (firstEnchantment == null && !tooltipOrder.contains(entry.getKey())) {
                firstEnchantment = ObjectIntPair.of(entry.getKey(), entry.getIntValue());
                break;
            }
        }

        return firstEnchantment;
    }
}
