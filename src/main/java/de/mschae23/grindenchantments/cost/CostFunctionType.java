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

package de.mschae23.grindenchantments.cost;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import com.mojang.serialization.MapCodec;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.registry.GrindEnchantmentsRegistries;
import io.netty.buffer.ByteBuf;

import static de.mschae23.grindenchantments.registry.GrindEnchantmentsRegistries.COST_FUNCTIONS;

public interface CostFunctionType<M extends CostFunction> {
//    CostFunctionType<CountEnchantmentsCostFunction> COUNT_ENCHANTMENTS = register("count_enchantments", CountEnchantmentsCostFunction.TYPE);
//    CostFunctionType<CountLevelsCostFunction> COUNT_LEVELS = register("count_levels", CountLevelsCostFunction.TYPE);
//    CostFunctionType<CountMinPowerCostFunction> COUNT_MIN_POWER = register("count_min_power", CountMinPowerCostFunction.TYPE);
//    CostFunctionType<AverageCountCostFunction> AVERAGE_COUNT = register("average_count", AverageCountCostFunction.TYPE);
//    CostFunctionType<FirstEnchantmentCostFunction> FIRST_ENCHANTMENT = register("first_enchantment", FirstEnchantmentCostFunction.TYPE);
//    CostFunctionType<TransformCostFunction> TRANSFORM = register("transform", TransformCostFunction.TYPE);
//    CostFunctionType<FilterCostFunction> FILTER = register("filter", FilterCostFunction.TYPE);


    Supplier<CostFunctionType<CountEnchantmentsCostFunction>> COUNT_ENCHANTMENTS = COST_FUNCTIONS.register("count_enchantments",()->CountEnchantmentsCostFunction.TYPE);
    Supplier<CostFunctionType<CountLevelsCostFunction>> COUNT_LEVELS = COST_FUNCTIONS.register("count_levels",()->CountLevelsCostFunction.TYPE);
    Supplier<CostFunctionType<CountMinPowerCostFunction>> COUNT_MIN_POWER = COST_FUNCTIONS.register("count_min_power",()->CountMinPowerCostFunction.TYPE);
    Supplier<CostFunctionType<AverageCountCostFunction>> AVERAGE_COUNT = COST_FUNCTIONS.register("average_count",()->AverageCountCostFunction.TYPE);
    Supplier<CostFunctionType<FirstEnchantmentCostFunction>> FIRST_ENCHANTMENT = COST_FUNCTIONS.register("first_enchantment",()->FirstEnchantmentCostFunction.TYPE);
    Supplier<CostFunctionType<TransformCostFunction>> TRANSFORM = COST_FUNCTIONS.register("transform",()->TransformCostFunction.TYPE);
    Supplier<CostFunctionType<FilterCostFunction>> FILTER = COST_FUNCTIONS.register("filter",()->FilterCostFunction.TYPE);

    MapCodec<M> codec();
    StreamCodec<FriendlyByteBuf, M> packetCodec(StreamCodec<FriendlyByteBuf, CostFunction> delegateCodec);

//    static <M extends CostFunction> CostFunctionType<M> register(String id, CostFunctionType<M> type) {
//        return Registry.register(GrindEnchantmentsRegistries.COST_FUNCTION_REGISTRY, GrindEnchantmentsMod.id(id), type);
//    }

    static StreamCodec<ByteBuf, CostFunctionType<?>> createPacketCodec() {
        return ResourceKey.streamCodec(GrindEnchantmentsRegistries.COST_FUNCTION_KEY).map(
            key -> GrindEnchantmentsRegistries.COST_FUNCTION_REGISTRY.getOptional(key).orElseThrow(
                () -> new IllegalStateException("Can't decode '" + key.location() + "', unregistered value")),
            type -> GrindEnchantmentsRegistries.COST_FUNCTION_REGISTRY.getResourceKey(type).orElseThrow(
                () -> new IllegalStateException("Can't encode '" + type + "', unregistered value"))
        );
    }

    static void init() {
    }

    record Impl<M extends CostFunction>(MapCodec<M> codec, Function<StreamCodec<FriendlyByteBuf, CostFunction>, StreamCodec<FriendlyByteBuf, M>> packetCodec) implements CostFunctionType<M> {
        @Override
        public MapCodec<M> codec() {
            return this.codec;
        }

        @Override
        public StreamCodec<FriendlyByteBuf, M> packetCodec(StreamCodec<FriendlyByteBuf, CostFunction> delegateCodec) {
            return this.packetCodec.apply(delegateCodec);
        }
    }
}
