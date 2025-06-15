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

package de.mschae23.grindenchantments.registry;

import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.cost.CostFunctionType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

//import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
//import net.minecraft.registry.Registry;
//import net.minecraft.registry.RegistryKey;

public final class GrindEnchantmentsRegistries {
    public static final ResourceKey<Registry<CostFunctionType<?>>> COST_FUNCTION_KEY = ResourceKey.createRegistryKey(GrindEnchantmentsMod.id("cost_function_type"));
    public static final Registry<CostFunctionType<?>> COST_FUNCTION_REGISTRY = new RegistryBuilder<>(COST_FUNCTION_KEY)
                        .sync(true)
                        .create();
    public static final DeferredRegister<CostFunctionType<?>> COST_FUNCTIONS = DeferredRegister.create(COST_FUNCTION_REGISTRY, GrindEnchantmentsMod.MODID);

    private GrindEnchantmentsRegistries() {
    }

    public static void init() {}

    public static void registerRegistries(NewRegistryEvent event) {
        event.register(COST_FUNCTION_REGISTRY);
    }
}
