package com.example.reachdisplay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

public class ReachDisplayClient implements ClientModInitializer, ModInitializer {

    // Дистанция (в блоках), ближе которой пишется TOO CLOSE!
    public static final double TOO_CLOSE_DISTANCE = 1.20;
    // Время показа надписи на экране (3 секунды)
    public static final long SHOW_DURATION_MS = 3000;

    private static double hitDistance = -1.0;
    private static long showUntil = 0L;

    @Override
    public void onInitialize() {
        // Официальный эвент Fabric API при атаке сущности
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity) {
                // Вычисляем расстояние от глаз атакующего до глаз/позиции цели
                double dist = player.getEyePos().distanceTo(entity.getEyePos());
                hitDistance = Math.round(dist * 100.0) / 100.0;
                showUntil = System.currentTimeMillis() + SHOW_DURATION_MS;
            }
            return ActionResult.PASS;
        });
    }

    @Override
    public void onInitializeClient() {
        // Отрисовка на экране
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        // Если время показа истекло — не рисуем
        if (System.currentTimeMillis() > showUntil || hitDistance < 0) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int centerX = width / 2;
        int centerY = height / 2;

        // Если расстояние слишком близкое
        if (hitDistance <= TOO_CLOSE_DISTANCE) {
            // Красная надпись TOO CLOSE! ниже прицела
            Text closeText = Text.literal("TOO CLOSE!").formatted(Formatting.RED, Formatting.BOLD);

            context.getMatrices().push();
            float scale = 1.35f;
            context.getMatrices().scale(scale, scale, 1.0f);

            int textWidth = client.textRenderer.getWidth(closeText);
            int drawX = (int) ((centerX / scale) - (textWidth / 2.0f));
            int drawY = (int) ((centerY + 30) / scale);

            // Отрисовка с тенью
            context.drawTextWithShadow(client.textRenderer, closeText, drawX, drawY, 0xFFFF2222);
            context.getMatrices().pop();
        } else {
            // Обычное расстояние (например 2.85m)
            String distString = String.format("%.2fm", hitDistance);
            Text distText = Text.literal(distString).formatted(Formatting.GOLD, Formatting.BOLD);

            context.getMatrices().push();
            float scale = 1.25f;
            context.getMatrices().scale(scale, scale, 1.0f);

            int textWidth = client.textRenderer.getWidth(distText);
            int drawX = (int) ((centerX / scale) - (textWidth / 2.0f));
            int drawY = (int) ((centerY + 28) / scale);

            context.drawTextWithShadow(client.textRenderer, distText, drawX, drawY, 0xFFFFAA00);
            context.getMatrices().pop();
        }
    }
}
