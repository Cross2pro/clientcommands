package net.earthcomputer.clientcommands.command;

import net.earthcomputer.clientcommands.mixin.InGameHudAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

public class ClientCommandHelper {

    public static void sendError(Component error) {
        sendFeedback(Component.literal("").append(error).withStyle(ChatFormatting.RED));
    }

    public static void sendHelp(Component help) {
        sendFeedback(Component.literal("").append(help).withStyle(ChatFormatting.AQUA));
    }

    public static void sendFeedback(String message, Object... args) {
        sendFeedback(Component.translatable(message, args));
    }

    public static void sendFeedback(Component message) {
        Minecraft.getInstance().gui.getChat().addMessage(message);
    }

    public static void sendRequiresRestart() {
        sendFeedback(Component.translatable("commands.client.requiresRestart").withStyle(ChatFormatting.YELLOW));
    }

    public static void addOverlayMessage(Component message, int time) {
        Gui gui = Minecraft.getInstance().gui;
        gui.setOverlayMessage(message, false);
        ((InGameHudAccessor) gui).setOverlayRemaining(time);
    }

    public static Component getLookCoordsTextComponent(BlockPos pos) {
        return getCommandTextComponent(Component.translatable("commands.client.blockpos", pos.getX(), pos.getY(), pos.getZ()),
                String.format("/clook block %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
    }

    public static Component getLookCoordsTextComponent(MutableComponent translatableText, BlockPos pos) {
        return getCommandTextComponent(translatableText, String.format("/clook block %d %d %d", pos.getX(), pos.getY(), pos.getZ()));
    }

    public static Component getGlowCoordsTextComponent(BlockPos pos) {
        return getCommandTextComponent(Component.translatable("commands.client.blockpos", pos.getX(), pos.getY(), pos.getZ()),
                String.format("/cglow block %d %d %d 10", pos.getX(), pos.getY(), pos.getZ()));
    }

    public static Component getGlowCoordsTextComponent(MutableComponent translatableText, BlockPos pos) {
        return getCommandTextComponent(translatableText, String.format("/cglow block %d %d %d 10", pos.getX(), pos.getY(), pos.getZ()));
    }

    public static Component getCommandTextComponent(String translationKey, String command) {
        return getCommandTextComponent(Component.translatable(translationKey), command);
    }

    public static Component getCommandTextComponent(MutableComponent translatableText, String command) {
        return translatableText.withStyle(style -> style.withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

}
