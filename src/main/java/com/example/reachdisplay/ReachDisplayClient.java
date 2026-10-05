package com.example.reachdisplay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class ReachDisplayClient implements ClientModInitializer {

    // Порог дистанции для срабатывания надписи "TOO CLOSE!"
    public static final double TOO_CLOSE_THRESHOLD = 1.2;
    // Время показа на экране (3 секунды)
    public static final long DISPLAY_DURATION_MS = 3000;

    private static double lastDistance = -1.0;
    private static long displayUntilTime = 0L;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::renderHud);
    }

    public static void onEntityHit(PlayerEntity attacker, Entity target) {
        // Проверяем, что цель является игроком
        if (!(target instanceof PlayerEntity)) {
            return;
        }

        // Дистанция от глаз атакующего до центра хитбокса цели
        double dist = attacker.getEyePos().distanceTo(target.getEyePos());
        lastDistance = Math.round(dist * 100.0) / 100.0;
        displayUntilTime = System.currentTimeMillis() + DISPLAY_DURATION_MS;
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
        int centerY = screenHeight / 2 + 35; // Смещение ниже прицела

        Text text;
        int color;

        if (lastDistance <= TOO_CLOSE_THRESHOLD) {
            text = Text.literal("TOO CLOSE!");
            color = 0xFFFF3333; // Красный
        } else {
            text = Text.literal(String.format("%.2fm", lastDistance));
            color = 0xFFFFAA00; // Оранжево-желтый
        }

        context.getMatrices().push();
        float scale = 1.35f;
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = client.textRenderer.getWidth(text);
        int drawX = (int) ((centerX / scale) - (textWidth / 2.0f));
        int drawY = (int) (centerY / scale);

        context.drawTextWithShadow(client.textRenderer, text, drawX, drawY, color);
        context.getMatrices().pop();
    }
}
