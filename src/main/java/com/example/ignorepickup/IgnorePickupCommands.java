package com.example.ignorepickup;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class IgnorePickupCommands {

    private IgnorePickupCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(tree());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("ignorepickup")
                .requires(src -> src.hasPermission(2))
                // Bật chặn nhặt (active)
                .then(Commands.literal("block")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .executes(ctx -> {
                                    ResourceLocation rl = ResourceLocationArgument.getId(ctx, "id");
                                    Config.block(rl.toString());
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Blocked (won't pick up): " + rl), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // Tắt chặn nhặt (deactive)
                .then(Commands.literal("allow")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .executes(ctx -> {
                                    ResourceLocation rl = ResourceLocationArgument.getId(ctx, "id");
                                    Config.allow(rl.toString());
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Allowed (can pick up): " + rl), true);
                                    return Command.SINGLE_SUCCESS;
                                })))
                // In danh sách
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            String known = String.join(", ", Config.getKnown());
                            String active = String.join(", ", Config.getIgnoredActive());
                            ctx.getSource().sendSuccess(() -> Component.literal("Known: [" + known + "]"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Blocked: [" + active + "]"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                // Reload JSON từ đĩa (phòng khi user tự sửa file)
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            Config.load();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Reloaded ignorepickup.json"), true);
                            return Command.SINGLE_SUCCESS;
                        }));
    }
}
