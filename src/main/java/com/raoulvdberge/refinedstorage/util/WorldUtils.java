package com.raoulvdberge.refinedstorage.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;

public final class WorldUtils {
    public static void updateBlock(@Nullable World world, BlockPos pos) {
        if (world != null) {
            BlockState state = world.getBlockState(pos);

            world.notifyBlockUpdate(pos, state, state, 1 | 2);
        }
    }

    public static IItemHandler getItemHandler(@Nullable TileEntity tile, Direction side) {
        if (tile == null) {
            return null;
        }

        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
        if (handler == null) {
            if (side != null && tile instanceof ISidedInventory) {
                handler = new SidedInvWrapper((ISidedInventory) tile, side);
            } else if (tile instanceof IInventory) {
                handler = new InvWrapper((IInventory) tile);
            }
        }

        return handler;
    }

    public static IFluidHandler getFluidHandler(@Nullable TileEntity tile, Direction side) {
        if (tile != null) {
            return tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).orElse(null);
        }
        return null;
    }

    public static void sendNoPermissionMessage(PlayerEntity player) {
        player.sendMessage(new TranslationTextComponent("misc.refinedstorage:security.no_permission").setStyle(new Style().setColor(TextFormatting.RED)));
    }

    public static RayTraceResult rayTracePlayer(World world, PlayerEntity player) {
        double reachDistance = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();

        Vec3d base = player.getEyePosition(1.0F);
        Vec3d look = player.getLookVec();
        Vec3d target = base.add(look.x * reachDistance, look.y * reachDistance, look.z * reachDistance);

        return world.rayTraceBlocks(new RayTraceContext(base, target, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player));
    }
}
