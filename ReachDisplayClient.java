package com.example.reachdisplay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class ReachDisplayClient implements ClientModInitializer {

    // Порог расстояния в блоках для предупреждения
    public static final double TOO_CLOSE_THRESHOLD = 1.2;
    // Время показа на экране (2.5 секунды)
    public static final long DISPLAY_DURATION_MS = 2500;

    private static double lastDistance = -1.0;
    private static long displayUntilTime = 0L;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    public static void onPlayerAttack(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            double distance = client.player.getCameraPosVec(1.0F).distanceTo(entityHit.getPos());
            lastDistance = distance;
            displayUntilTime = System.currentTimeMillis() + DISPLAY_DURATION_MS;
        }
    }

    private void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        if (System.currentTimeMillis() > displayUntilTime || lastDistance < 0) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2 + 35;

        Text displayText;
        int color;

        if (lastDistance <= TOO_CLOSE_THRESHOLD) {
            displayText = Text.literal("TOO CLOSE!");
            color = 0xFFFF2222; // Красный
        } else {
            displayText = Text.literal(String.format("%.2fm", lastDistance));
            color = 0xFFFFAA00; // Оранжево-желтый
        }

        context.getMatrices().push();
        float scale = 1.4f;
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = client.textRenderer.getWidth(displayText);
        int drawX = (int) ((centerX / scale) - (textWidth / 2.0f));
        int drawY = (int) (centerY / scale);

        context.drawTextWithShadow(client.textRenderer, displayText, drawX, drawY, color);
        context.getMatrices().pop();
    }
}
