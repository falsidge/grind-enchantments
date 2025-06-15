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

package de.mschae23.grindenchantments.config.sync;

import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.config.ServerConfig;
import de.mschae23.grindenchantments.cost.CostFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerConfigS2CPayload(ServerConfig config) implements CustomPacketPayload {
    public static final ResourceLocation PACKET_ID = GrindEnchantmentsMod.id("server_config");
    public static final Type<ServerConfigS2CPayload> ID = new Type<>(PACKET_ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static StreamCodec<FriendlyByteBuf, ServerConfigS2CPayload> createPacketCodec(StreamCodec<FriendlyByteBuf, CostFunction> costFunctionCodec) {
        return StreamCodec.composite(
            // Version field for forward compatibility
            ByteBufCodecs.BYTE, payload -> (byte) 1,
            ServerConfig.createPacketCodec(costFunctionCodec), ServerConfigS2CPayload::config,
            (version, config) -> new ServerConfigS2CPayload(config)
        );
    }
}
