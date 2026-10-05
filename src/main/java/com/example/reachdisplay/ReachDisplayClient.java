package com.example.reachdisplay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

public class ReachDisplayClient implements ClientModInitializer, ModInitializer {

    // Дистанция (в блоках), ближе которой пишется TOO CLOSE!
    public static final double TOO_CLOSE_DISTANCE = 1.30;
    // Время показа надписи (2.5 секунды)
    public static final long SHOW_DURATION_MS = 2500;

    private static boolean isTooClose = false;
    private static long showUntil = 0L;

    @Override
    public void onInitialize() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity) {
                double dist = player.getEyePos().distanceTo(entity.getEyePos());
                if (dist <= TOO_CLOSE_DISTANCE) {
                    isTooClose = true;
                    showUntil = System.currentTimeMillis() + SHOW_DURATION_MS;
                } else {
                    isTooClose = false;
                }
            }
            return ActionResult.PASS;
        });
    }

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        if (!isTooClose || System.currentTimeMillis() > showUntil) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int centerX = width / 2;
        int centerY = height / 2;

        // Компактный красный текст TOO CLOSE!
        Text closeText = Text.literal("TOO CLOSE!").formatted(Formatting.RED);

        // Масштаб 1.0 (компактный размер шрифта)
        context.getMatrices().push();
        float scale = 1.0f;
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = client.textRenderer.getWidth(closeText);
        int drawX = (int) ((centerX / scale) - (textWidth / 2.0f));
        int drawY = (int) ((centerY + 24) / scale);

        context.drawTextWithShadow(client.textRenderer, closeText, drawX, drawY, 0xFFFF3333);
        context.getMatrices().pop();
    }
}
