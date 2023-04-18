package a7.itemSearcher.utils;

import a7.itemSearcher.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Timer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McUtils {
    private static final Matcher COLOR_MATCHER = Pattern.compile("§[0-9a-fk-orA-FK-OR]").matcher("");
    private static Minecraft mc = Minecraft.getMinecraft();
    private static Timer mcTimer = ((AccessorMinecraft) mc).getTimer();

    public static Minecraft getMc() {
        return mc;
    }

    public static FontRenderer getFontRenderer() {
        return mc.fontRendererObj;
    }

    public static EntityPlayerSP getPlayer() {
        return mc.thePlayer;
    }

    public static WorldClient getWorld() {
        return mc.theWorld;
    }

    public static float getPartialTicks() {
        return mcTimer.renderPartialTicks;
    }

    public static void sendChat(String s) {
        getPlayer().addChatMessage(new ChatComponentText(s));
    }

    public static void sendChatf(String s, Object... args) {
        getPlayer().addChatMessage(new ChatComponentText(String.format(s, args)));
    }

    public static String cleanColor(String s) {
        return COLOR_MATCHER.reset(s).replaceAll("");
    }
}
