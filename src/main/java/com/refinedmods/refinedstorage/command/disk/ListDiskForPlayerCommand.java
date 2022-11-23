package com.refinedmods.refinedstorage.command.disk;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.UUIDArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import java.util.Map;
import java.util.UUID;

public class ListDiskForPlayerCommand implements Command<CommandSource> {
    public static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.argument("UUID", UUIDArgument.func_239194_a_()).executes(new ListDiskForPlayerCommand());
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        UUID player = UUIDArgument.func_239195_a_(context, "uuid");
        
        API.instance().getStorageDiskManager(context.getSource().getWorld())
            .getAll()
            .entrySet()
            .stream()
            .filter(entry -> player.equals(entry.getValue().getOwner().toString()))
            .map(Map.Entry::getKey)
            .forEach(id -> context.getSource().sendFeedback(new StringTextComponent(id.toString()), false));

        return 0;
    }
}
