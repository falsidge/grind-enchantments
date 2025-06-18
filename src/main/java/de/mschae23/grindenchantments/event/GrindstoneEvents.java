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

package de.mschae23.grindenchantments.event;

//import net.fabricmc.fabric.api.event.Event;
//import net.fabricmc.fabric.api.event.EventFactory;
import de.mschae23.grindenchantments.GrindEnchantmentsMod;
import de.mschae23.grindenchantments.impl.DisenchantOperation;
import de.mschae23.grindenchantments.impl.MoveOperation;
import de.mschae23.grindenchantments.impl.Operation;
import de.mschae23.grindenchantments.impl.ResetRepairCostOperation;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import io.github.fourmisain.taxfreelevels.TaxFreeLevels;

public final class GrindstoneEvents {
    static Operation[] operations = {new DisenchantOperation(), new MoveOperation(),new ResetRepairCostOperation()};
    static Operation getOperation(ItemStack input1, ItemStack input2)
    {
        for (Operation op : operations)
        {
            if (op.isOperation(input1, input2))
            {
                return op;
            }
        }
        return null;
    }
    static public boolean canInsert(ItemStack stack, ItemStack other, int slotId)
    {
        for (Operation op : operations)
        {
            if (op.canInsert(stack, other, slotId))
            {
                return true;
            }
        }
        return false;
    }

    static public @NotNull ItemStack onUpdateResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup)
    {
        Operation op = getOperation(input1, input2);
        if (op == null)
        {
            return ItemStack.EMPTY;
        }
        return op.onUpdateResult(input1, input2, player, wrapperLookup);
    }

    static public boolean canTakeResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup)
    {
        for (Operation op : operations)
        {
            if (!op.canTakeResult(input1, input2, player, wrapperLookup))
            {
                return false;
            }
        }
        return  true;
    }

    static public boolean onTakeResult(ItemStack input1, ItemStack input2, ItemStack resultStack, Player player, Container input, HolderLookup.Provider wrapperLookup)
    {
        Operation op = getOperation(input1, input2);
        return op != null && op.onTakeResult(input1, input2, resultStack, player, input, wrapperLookup);
    }

    static public int getLevelCost(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup)
    {
        Operation op = getOperation(input1, input2);
        if (op == null)
        {
            return -1;
        }
        return op.getLevelCost(input1, input2, player, wrapperLookup);
    }
    public static boolean applyLevelCost(int cost, Player player)
    {
        if (ModList.get().isLoaded("taxfreelevels"))
        {
            TaxFreeLevels.applyFlattenedXpCost(player, cost);
            return true;
        }
        player.giveExperienceLevels(-cost);

        return true;
    }
}
//
//public final class GrindstoneEvents {
//    public static final Event<CanInsert> CAN_INSERT = EventFactory.createArrayBacked(CanInsert.class, callbacks -> (stack, other, slotId) -> {
//        for (CanInsert callback : callbacks) {
//            if (callback.canInsert(stack, other, slotId)) {
//                return true;
//            }
//        }
//
//        return false;
//    });
//
//    public static final Event<UpdateResult> UPDATE_RESULT = EventFactory.createArrayBacked(UpdateResult.class, callbacks -> (input1, input2, player, wrapperLookup) -> {
//        for (UpdateResult callback : callbacks) {
//            ItemStack result = callback.onUpdateResult(input1, input2, player, wrapperLookup);
//
//            if (!result.isEmpty()) {
//                return result;
//            }
//        }
//
//        return ItemStack.EMPTY;
//    });
//
//    public static final Event<CanTakeResult> CAN_TAKE_RESULT = EventFactory.createArrayBacked(CanTakeResult.class, callbacks -> (input1, input2, player, wrapperLookup) -> {
//        for (CanTakeResult callback : callbacks) {
//            if (!callback.canTakeResult(input1, input2, player, wrapperLookup)) {
//                return false;
//            }
//        }
//
//        return true;
//    });
//
//    public static final Event<TakeResult> TAKE_RESULT = EventFactory.createArrayBacked(TakeResult.class, callbacks -> (input1, input2, resultStack, player, input, wrapperLookup) -> {
//        for (TakeResult callback : callbacks) {
//            if (callback.onTakeResult(input1, input2, resultStack, player, input, wrapperLookup)) {
//                return true;
//            }
//        }
//
//        return false;
//    });
//
//    public static final Event<LevelCost> LEVEL_COST = EventFactory.createArrayBacked(LevelCost.class, callbacks -> (input1, input2, player, wrapperLookup) -> {
//        for (LevelCost callback : callbacks) {
//            int cost = callback.getLevelCost(input1, input2, player, wrapperLookup);
//
//            if (cost != -1) {
//                return cost;
//            }
//        }
//
//        return -1;
//    });
//
//    private GrindstoneEvents() {
//    }
//
//    public static <T extends CanInsert & UpdateResult & CanTakeResult & TakeResult & LevelCost> void registerAll(T listener) {
//        GrindstoneEvents.CAN_INSERT.register(listener);
//        GrindstoneEvents.UPDATE_RESULT.register(listener);
//        GrindstoneEvents.CAN_TAKE_RESULT.register(listener);
//        GrindstoneEvents.TAKE_RESULT.register(listener);
//        GrindstoneEvents.LEVEL_COST.register(listener);
//    }
//
//    public interface CanInsert {
//        boolean canInsert(ItemStack stack, ItemStack other, int slotId);
//    }
//
//    public abstract class CustomGrindstoneEvent extends Event
//    {
//        ItemStack input1;
//        ItemStack input2;
//        public CustomGrindstoneEvent(ItemStack stack, ItemStack other)
//        {
//            this.input1 = stack;
//            this.input2 = other;
//        }
//    }
//
//    public class CanInsertEvent extends CustomGrindstoneEvent {
//
//        int slotId;
//        public CanInsertEvent(ItemStack stack, ItemStack other, int slotId)
//        {
//            super(stack, other);
//            this.slotId = slotId;
//        }
//    }
//    public class UpdateResultEvent extends CustomGrindstoneEvent {
//
//        Player player;
//        HolderLookup.Provider wrapperLookup;
//        public UpdateResultEvent(ItemStack stack, ItemStack other, Player player,  HolderLookup.Provider wrapperLookup)
//        {
//            super(stack, other);
//            this.player = player;
//            this.wrapperLookup = wrapperLookup;
//        }
//    }
//    public class CanTakeResultEvent extends CustomGrindstoneEvent {
//
//        Player player;
//        HolderLookup.Provider wrapperLookup;
//        public CanTakeResultEvent(ItemStack stack, ItemStack other, Player player,  HolderLookup.Provider wrapperLookup)
//        {
//            super(stack, other);
//            this.player = player;
//            this.wrapperLookup = wrapperLookup;
//        }
//    }
//    public class onTakeResultEvent extends CustomGrindstoneEvent {
//
//        ItemStack resultStack;
//        Player player;
//        Container input;
//        HolderLookup.Provider wrapperLookup;
//        public onTakeResultEvent(ItemStack stack, ItemStack other, ItemStack resultStack, Player player, Container input, HolderLookup.Provider wrapperLookup)
//        {
//            super(stack, other);
//            this.resultStack = resultStack;
//            this.player = player;
//            this.input = input;
//            this.wrapperLookup = wrapperLookup;
//        }
//    }
//    public class getLevelCostEvent extends CustomGrindstoneEvent {
//
//        Player player;
//        HolderLookup.Provider wrapperLookup;
//        public getLevelCostEvent(ItemStack stack, ItemStack other, Player player, HolderLookup.Provider wrapperLookup)
//        {
//            super(stack, other);
//            this.player = player;
//            this.wrapperLookup = wrapperLookup;
//        }
//    }
//
//    public interface UpdateResult {
//        @NotNull
//        ItemStack onUpdateResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup);
//    }
//
//    public interface CanTakeResult {
//        boolean canTakeResult(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup);
//    }
//
//    public interface TakeResult {
//        boolean onTakeResult(ItemStack input1, ItemStack input2, ItemStack resultStack, Player player, Container input, HolderLookup.Provider wrapperLookup);
//    }
//
//    public interface LevelCost {
//        int getLevelCost(ItemStack input1, ItemStack input2, Player player, HolderLookup.Provider wrapperLookup);
//    }
//}
