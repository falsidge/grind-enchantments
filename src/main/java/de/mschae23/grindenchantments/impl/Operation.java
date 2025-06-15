package de.mschae23.grindenchantments.impl;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Operation {
    public boolean canInsert(ItemStack stack, ItemStack other, int slotId);

    public @NotNull ItemStack onUpdateResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup);

    public boolean canTakeResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup);

    public boolean onTakeResult(ItemStack input1, ItemStack input2, ItemStack resultStack, Player player, Container input, HolderLookup.Provider wrapperLookup);

    public int getLevelCost(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup);

    public boolean isOperation(ItemStack input1, ItemStack input2);
}

