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

package de.mschae23.grindenchantments.config;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum FilterAction implements StringRepresentable {
    ALLOW("allow"),
    IGNORE("ignore"),
    DENY("deny");

    public static final Codec<FilterAction> CODEC = StringRepresentable.fromEnum(FilterAction::values);
    public static final Codec<FilterAction> NON_IGNORE_CODEC = StringRepresentable.fromEnum(() -> new FilterAction[] { ALLOW, DENY, });

    public static final StreamCodec<FriendlyByteBuf, FilterAction> PACKET_CODEC = ByteBufCodecs.idMapper(i -> values()[i], FilterAction::ordinal).cast();

    private final String name;

    FilterAction(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
