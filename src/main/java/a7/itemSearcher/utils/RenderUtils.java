package a7.itemSearcher.utils;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
    public static void drawGradientRect(int left, int top, int right, int bottom, int z,
                                        int topColor, int bottomColor, int lineWidth) {
        float r = (float) (topColor >> 24 & 0xFF) / 255.0F;
        float g = (float) (topColor >> 16 & 0xFF) / 255.0F;
        float b = (float) (topColor >> 8 & 0xFF) / 255.0F;
        float a = (float) (topColor & 0xFF) / 255.0F;
        float r2 = (float) (bottomColor >> 24 & 0xFF) / 255.0F;
        float g2 = (float) (bottomColor >> 16 & 0xFF) / 255.0F;
        float b2 = (float) (bottomColor >> 8 & 0xFF) / 255.0F;
        float a2 = (float) (bottomColor & 0xFF) / 255.0F;
        GlStateManager.disableTexture2D();
//        GlStateManager.enableBlend();
//        GlStateManager.disableAlpha();
//        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
//        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GL11.glLineWidth(lineWidth);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
//        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(left, top, z).color(r, g, b, a).endVertex();
        worldRenderer.pos(right, top, z).color(r, g, b, a).endVertex();
        worldRenderer.pos(left, top, z).color(r, g, b, a).endVertex();
        worldRenderer.pos(left, bottom, z).color(r2, g2, b2, a2).endVertex();
        worldRenderer.pos(right, top, z).color(r, g, b, a).endVertex();
        worldRenderer.pos(right, bottom, z).color(r2, g2, b2, a2).endVertex();
        worldRenderer.pos(left, bottom, z).color(r2, g2, b2, a2).endVertex();
        worldRenderer.pos(right, bottom, z).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();
//        GlStateManager.shadeModel(GL11.GL_SMOOTH);
//        GlStateManager.disableBlend();
//        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}