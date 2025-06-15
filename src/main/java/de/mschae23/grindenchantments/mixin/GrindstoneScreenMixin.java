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

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.event.GrindstoneEvents;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GrindstoneScreen.class)
public abstract class GrindstoneScreenMixin extends AbstractContainerScreen<GrindstoneMenu> {
    public GrindstoneScreenMixin(GrindstoneMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics context, int mouseX, int mouseY) {super.renderLabels(context, mouseX, mouseY);
        if (!GrindEnchantmentsMod.getClientConfig().showLevelCost())
            return; // Don't show the enchantment cost

        Minecraft client = Objects.requireNonNull(this.minecraft);
        LocalPlayer player = Objects.requireNonNull(client.player);
        ClientLevel world = Objects.requireNonNull(client.level);
        RegistryAccess registryManager = world.registryAccess();

        int cost = GrindstoneEvents.getLevelCost(this.menu.getSlot(0).getItem(), this.menu.getSlot(1).getItem(),
            player, registryManager);

        if (cost > 0) {
            int j = 8453920;
            Component text;

            if (!this.menu.getSlot(2).hasItem()) {
                text = null;
            } else {
                text = Component.translatable("container.repair.cost", cost);
                if (!this.menu.getSlot(2).mayPickup(this.minecraft.player)) {
                    j = 16736352;
                }
            }

            if (text != null) {
                int k = this.imageWidth - 8 - this.font.width(text) - 2;
                context.fill(k - 2, 67, this.imageWidth - 8, 79, 1325400064);
                context.drawString(this.font, text, k, 69, j);
            }
        }
    }
}
