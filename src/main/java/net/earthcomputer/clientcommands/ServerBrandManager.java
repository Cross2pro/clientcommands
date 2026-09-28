package net.earthcomputer.clientcommands;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Minimal RNG-only server brand tracking. Warns when RNG manipulation features
 * are enabled on a non-vanilla server, where the RNG may not behave as expected.
 */
public class ServerBrandManager {

    private static String serverBrand = "vanilla";
    private static boolean hasWarnedRng = false;

    public static void setServerBrand(String brand) {
        serverBrand = brand;
    }

    public static String getServerBrand() {
        return serverBrand;
    }

    public static boolean isVanilla() {
        return "vanilla".equals(serverBrand);
    }

    public static void onDisconnect() {
        hasWarnedRng = false;
    }

    public static void rngWarning() {
        if (!isVanilla() && !hasWarnedRng && !Minecraft.getInstance().isSingleplayer()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui != null && mc.gui.getChat() != null) {
                mc.gui.getChat().addMessage(
                        Component.translatable("playerManip.serverBrandWarning").withStyle(ChatFormatting.YELLOW));
            }
            hasWarnedRng = true;
        }
    }
}
