package com.titanmod.client;

import com.titanmod.entity.TitanEntity;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** HUD simples do piloto: escudo, vida e cargas de dash. Só aparece quando o jogador está num Titã. */
public class TitanHud {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    private static final int COLOR_BG = 0xAA000000;
    private static final int COLOR_SHIELD = 0xFF33CCFF;
    private static final int COLOR_HEALTH = 0xFFE04040;
    private static final int COLOR_HEAT = 0xFFFF8C1A;
    private static final int COLOR_HEAT_ALERT_A = 0xFFFF3030;
    private static final int COLOR_HEAT_ALERT_B = 0xFFFFFFFF;
    private static final int COLOR_DASH_ON = 0xFFFFC832;
    private static final int COLOR_DASH_OFF = 0xFF444444;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !(mc.player.getVehicle() instanceof TitanEntity titan)) {
            return;
        }

        int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
        int shieldY = graphics.guiHeight() - 78;
        int healthY = shieldY + 9;
        int dashY = healthY + 10;

        // Calor da arma: laranja; piscando em vermelho/branco quando superaquece (arma travada).
        int heatY = shieldY - 9;
        boolean overheated = titan.isOverheated();
        long time = mc.level != null ? mc.level.getGameTime() : 0L;
        int heatColor = overheated ? ((time / 3) % 2 == 0 ? COLOR_HEAT_ALERT_A : COLOR_HEAT_ALERT_B) : COLOR_HEAT;
        drawBar(graphics, x, heatY, titan.getHeat() / (float) TitanEntity.MAX_HEAT, heatColor);
        if (overheated) {
            graphics.drawString(mc.font, Component.translatable("hud.titanmod.overheat"),
                    x + BAR_WIDTH + 4, heatY - 2, COLOR_HEAT_ALERT_A);
        }

        drawBar(graphics, x, shieldY, titan.getShield() / (float) TitanEntity.MAX_SHIELD, COLOR_SHIELD);
        drawBar(graphics, x, healthY, titan.getHealth() / titan.getMaxHealth(), COLOR_HEALTH);

        graphics.drawString(mc.font, String.valueOf(titan.getShield()), x + BAR_WIDTH + 4, shieldY - 2, COLOR_SHIELD);
        graphics.drawString(mc.font, String.valueOf((int) titan.getHealth()), x + BAR_WIDTH + 4, healthY - 2, COLOR_HEALTH);

        for (int i = 0; i < TitanEntity.MAX_DASH_CHARGES; i++) {
            int px = x + i * 16;
            int color = i < titan.getDashCharges() ? COLOR_DASH_ON : COLOR_DASH_OFF;
            graphics.fill(px, dashY, px + 14, dashY + 5, color);
        }
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, float fraction, int color) {
        fraction = Math.max(0.0F, Math.min(1.0F, fraction));
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BG);
        graphics.fill(x, y, x + (int) (BAR_WIDTH * fraction), y + BAR_HEIGHT, color);
    }
}
