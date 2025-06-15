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
import de.mschae23.grindenchantments.config.FilterConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class CountEnchantmentsCostFunction implements CostFunction {
    public static final CountEnchantmentsCostFunction INSTANCE = new CountEnchantmentsCostFunction();
    public static final MapCodec<CountEnchantmentsCostFunction> TYPE_CODEC = MapCodec.unit(() -> INSTANCE);
    public static final CostFunctionType<CountEnchantmentsCostFunction> TYPE = new CostFunctionType.Impl<>(TYPE_CODEC, CountEnchantmentsCostFunction::packetCodec);

    @Override
    public double getCost(ItemEnchantments enchantments, FilterConfig filter, HolderLookup.Provider wrapperLookup) {
        return enchantments.keySet().size();
    }

    @Override
    public CostFunctionType<?> getType() {
        return CostFunctionType.COUNT_ENCHANTMENTS.get();
    }

    public static StreamCodec<FriendlyByteBuf, CountEnchantmentsCostFunction> packetCodec(StreamCodec<FriendlyByteBuf, CostFunction> delegateCodec) {
        return StreamCodec.unit(INSTANCE);
    }

    @Override
    public String toString() {
        return "CountEnchantmentsCostFunction{}";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CountEnchantmentsCostFunction;
    }
}
