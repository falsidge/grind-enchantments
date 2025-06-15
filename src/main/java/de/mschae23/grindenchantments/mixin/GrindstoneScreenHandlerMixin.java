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

package de.mschae23.grindenchantments.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.mschae23.grindenchantments.GrindEnchantments;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.config.ServerConfig;
import de.mschae23.grindenchantments.event.GrindstoneEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneScreenHandlerMixin extends AbstractContainerMenu {
    @Shadow
    @Final
    public Container repairSlots;
    @Final
    @Shadow
    public Container resultSlots;

    @Unique
    private Player grindenchantments_player;

    protected GrindstoneScreenHandlerMixin(MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(at = @At("RETURN"), method = "Lnet/minecraft/world/inventory/GrindstoneMenu;<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V")
    private void onReturnConstructor(int containerId, Inventory playerInventory, final ContainerLevelAccess access, CallbackInfo ci) {
        this.grindenchantments_player = playerInventory.player;
    }

    @Inject(at = @At("RETURN"), method = "Lnet/minecraft/world/inventory/GrindstoneMenu;computeResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", cancellable = true)
    private void onGetOutputStack(ItemStack input1, ItemStack input2, CallbackInfoReturnable<ItemStack> cir) {
        Player player = this.grindenchantments_player;

        if (player == null) {
            GrindEnchantmentsMod.LOGGER.warn("Player not found while accessing grindstone!");
            return;
        }
        if (cir.getReturnValue().isEmpty()) {
            ItemStack result = GrindstoneEvents.onUpdateResult(input1, input2, player, player.registryAccess());

            if (!result.isEmpty()) {
                cir.setReturnValue(result);
            }
        }
    }

    @Inject(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/GrindstoneMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z", ordinal = 0))
    private void onInsertResultItem(Player player, int index, CallbackInfoReturnable<ItemStack> cir, @Local(ordinal = 1) ItemStack itemStack2) {
        ServerConfig config = GrindEnchantmentsMod.getServerConfig();

        if (config.dedicatedServerConfig().alternativeCostDisplay()) {
            GrindEnchantments.removeLevelCostNbt(itemStack2);
        }
    }

    @Mixin(targets = "net/minecraft/world/inventory/GrindstoneMenu$2")
    public static class Anonymous2Mixin extends Slot {
        public Anonymous2Mixin(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Inject(method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
        private void canInsertBooks(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(cir.getReturnValueZ() || GrindstoneEvents.canInsert(stack, this.container.getItem(1), 0));
        }
    }

    @Mixin(targets = "net/minecraft/world/inventory/GrindstoneMenu$3")
    public static class Anonymous3Mixin extends Slot {
        public Anonymous3Mixin(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Inject(method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
        private void canInsertBooks(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(cir.getReturnValueZ() || GrindstoneEvents.canInsert(stack, this.container.getItem(0), 1));
        }
    }

    @Mixin(targets = "net/minecraft/world/inventory/GrindstoneMenu$4")
    public static abstract class Anonymous4Mixin extends Slot {
        @Final
        @Shadow
        ContainerLevelAccess val$access;
        @Shadow
        @Final
        GrindstoneMenu this$0;

        public Anonymous4Mixin(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
        private void onTakeResult(Player player, ItemStack stack, CallbackInfo ci) {
            Container input = this.this$0.repairSlots;

            ItemStack input1 = input.getItem(0);
            ItemStack input2 = input.getItem(1);

            boolean success = GrindstoneEvents.onTakeResult(input1, input2, stack, player, input, player.registryAccess());

            if (GrindEnchantmentsMod.getServerConfig().dedicatedServerConfig().alternativeCostDisplay()) {
                GrindEnchantments.removeLevelCostNbt(stack);
            }

            if (success) {
                this.val$access.execute((world, pos) -> world.levelEvent(LevelEvent.SOUND_GRINDSTONE_USED, pos, 0)); // Plays grindstone sound
                ci.cancel();
            }
        }

        /**
         * @author mschae23
         */
        @Override
        public boolean mayPickup(Player player) {
            Container input = this.this$0.repairSlots;

            ItemStack input1 = input.getItem(0);
            ItemStack input2 = input.getItem(1);

            return GrindstoneEvents.canTakeResult(input1, input2, player, player.registryAccess());
        }
    }
}
