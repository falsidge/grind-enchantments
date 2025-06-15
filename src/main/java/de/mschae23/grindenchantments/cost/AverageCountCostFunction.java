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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.mschae23.grindenchantments.config.FilterConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record AverageCountCostFunction(CostFunction function) implements CostFunction {
    public static final MapCodec<AverageCountCostFunction> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CostFunction.CODEC.fieldOf("function").forGetter(AverageCountCostFunction::function)
    ).apply(instance, instance.stable(AverageCountCostFunction::new)));
    public static final CostFunctionType.Impl<AverageCountCostFunction> TYPE = new CostFunctionType.Impl<>(TYPE_CODEC, AverageCountCostFunction::packetCodec);

    @Override
    public double getCost(ItemEnchantments enchantments, FilterConfig filter, HolderLookup.Provider wrapperLookup) {
        double cost = this.function.getCost(enchantments, filter, wrapperLookup);
        long count = enchantments.keySet().size();

        if (count == 0) {
            return cost;
        }

        return cost / (double) count;
    }

    @Override
    public CostFunctionType<?> getType() {
        return CostFunctionType.AVERAGE_COUNT.get();
    }

    public static StreamCodec<FriendlyByteBuf, AverageCountCostFunction> packetCodec(StreamCodec<FriendlyByteBuf, CostFunction> delegateCodec) {
        return StreamCodec.composite(delegateCodec, AverageCountCostFunction::function, AverageCountCostFunction::new);
    }

    @Override
    public String toString() {
        return "AverageCountCostFunction{" +
            "function=" + this.function +
            '}';
    }
}
