package velzee.sylvarabot.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SylvaraSettingsScreen extends Screen {
    private final Screen parentScreen;
    private double scrollAmount = 0;
    private int panelY1, panelY2;
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();

    public SylvaraSettingsScreen(Screen parentScreen) {
        super(Component.literal("Sylvara Advanced Settings"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 260;
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 115;

        this.panelY1 = topY + 28;
        this.panelY2 = topY + 185;
        scrollableWidgets.clear();

        int currentY = panelY1 + 8;

        // ==========================================
        // CONTAINER 1: GENERAL TOGGLES
        // ==========================================
        Button backpackBtn = Button.builder(Component.literal(AutoLumberjackController.isHasBackpack() ? "Backpack: §aON" : "Backpack: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isHasBackpack();
            AutoLumberjackController.setHasBackpack(next);
            button.setMessage(Component.literal(next ? "Backpack: §aON" : "Backpack: §cOFF"));
        }).bounds(leftX + 12, currentY, panelWidth - 24, 18).build();
        this.addRenderableWidget(backpackBtn);
        scrollableWidgets.add(backpackBtn);
        currentY += 22;

        Button staffBtn = Button.builder(Component.literal(AutoLumberjackController.isAntiStaffPauseEnabled() ? "Staff Pause: §aON" : "Staff Pause: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isAntiStaffPauseEnabled();
            AutoLumberjackController.setAntiStaffPauseEnabled(next);
            button.setMessage(Component.literal(next ? "Staff Pause: §aON" : "Staff Pause: §cOFF"));
        }).bounds(leftX + 12, currentY, panelWidth - 24, 18).build();
        this.addRenderableWidget(staffBtn);
        scrollableWidgets.add(staffBtn);
        currentY += 22;

        Button safetyBtn = Button.builder(Component.literal(AutoLumberjackController.isSafetyDisconnectEnabled() ? "Safety Disconnect: §aON" : "Safety Disconnect: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isSafetyDisconnectEnabled();
            AutoLumberjackController.setSafetyDisconnectEnabled(next);
            button.setMessage(Component.literal(next ? "Safety Disconnect: §aON" : "Safety Disconnect: §cOFF"));
        }).bounds(leftX + 12, currentY, panelWidth - 24, 18).build();
        this.addRenderableWidget(safetyBtn);
        scrollableWidgets.add(safetyBtn);
        currentY += 30;


        // ==========================================
        // CONTAINER 2: FOODS & DRINKS (MULTI-SELECT)
        // ==========================================
        for (AutoLumberjackController.ConsumableItem food : AutoLumberjackController.getFoodList()) {
            Button foodBtn = Button.builder(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleFood(food.name);
                button.setMessage(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 12, currentY, panelWidth - 24, 18).build();
            this.addRenderableWidget(foodBtn);
            scrollableWidgets.add(foodBtn);
            currentY += 22;
        }

        for (AutoLumberjackController.ConsumableItem drink : AutoLumberjackController.getDrinkList()) {
            Button drinkBtn = Button.builder(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleDrink(drink.name);
                button.setMessage(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 12, currentY, panelWidth - 24, 18).build();
            this.addRenderableWidget(drinkBtn);
            scrollableWidgets.add(drinkBtn);
            currentY += 22;
        }
        currentY += 15;


        // ==========================================
        // CONTAINER 3: ADVANCED TIMERS & PRECISION (SLIDERS)
        // ==========================================

        // 1. Minigame Click Delay Slider (Range: 5 - 30 ticks)
        double clickDelayProgress = (double)(AutoLumberjackController.getMinigameClickDelay() - 5) / 25.0;
        CustomSlider clickDelaySlider = new CustomSlider(leftX + 12, currentY, panelWidth - 24, 18, clickDelayProgress, "Minigame Delay", " ticks", 5.0, 30.0, (Double val) -> {
            AutoLumberjackController.setMinigameClickDelay(val.intValue());
        });
        this.addRenderableWidget(clickDelaySlider);
        scrollableWidgets.add(clickDelaySlider);
        currentY += 22;

        // 2. Camera Align Speed Slider (Range: 0.05 - 0.50)
        double cameraSpeedProgress = (AutoLumberjackController.getCameraAlignSpeed() - 0.05f) / 0.45f;
        CustomSlider cameraSpeedSlider = new CustomSlider(leftX + 12, currentY, panelWidth - 24, 18, cameraSpeedProgress, "Camera Speed", "", 0.05, 0.50, (Double val) -> {
            AutoLumberjackController.setCameraAlignSpeed(val.floatValue());
        });
        this.addRenderableWidget(cameraSpeedSlider);
        scrollableWidgets.add(cameraSpeedSlider);
        currentY += 22;

        // 3. GUI Timeout Limit Slider (Range: 100 - 500 ticks)
        double timeoutProgress = (double)(AutoLumberjackController.getGuiTimeoutLimit() - 100) / 400.0;
        CustomSlider timeoutSlider = new CustomSlider(leftX + 12, currentY, panelWidth - 24, 18, timeoutProgress, "GUI Timeout", " ticks", 100.0, 500.0, (Double val) -> {
            AutoLumberjackController.setGuiTimeoutLimit(val.intValue());
        });
        this.addRenderableWidget(timeoutSlider);
        scrollableWidgets.add(timeoutSlider);
        currentY += 25;


        // ==========================================
        // TOMBOL KEMBALI (FIXED DI BAWAH)
        // ==========================================
        this.addRenderableWidget(Button.builder(Component.literal("§c← Kembali ke Menu Utama"), button -> {
            this.minecraft.setScreen(parentScreen);
        }).bounds(leftX + 12, topY + 192, panelWidth - 24, 20).build());

        updateScrollPositions();
    }

    // --- KELAS KUSTOM SLIDER ALA MINECRAFT 1.21.1 ---
    private static class CustomSlider extends AbstractSliderButton {
        private final String label;
        private final String unit;
        private final double minVal;
        private final double maxVal;
        private final Consumer<Double> onValueChanged;

        public CustomSlider(int x, int y, int width, int height, double progress, String label, String unit, double minVal, double maxVal, Consumer<Double> onValueChanged) {
            super(x, y, width, height, Component.empty(), progress);
            this.label = label;
            this.unit = unit;
            this.minVal = minVal;
            this.maxVal = maxVal;
            this.onValueChanged = onValueChanged;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double currentVal = minVal + (maxVal - minVal) * this.value;
            String formattedVal = unit.isEmpty() ? String.format("%.2f", currentVal) : String.valueOf((int)Math.round(currentVal));
            this.setMessage(Component.literal(label + ": §e" + formattedVal + unit));
        }

        @Override
        protected void applyValue() {
            double currentVal = minVal + (maxVal - minVal) * this.value;
            if (unit.isEmpty()) {
                onValueChanged.accept(Math.round(currentVal * 100.0) / 100.0);
            } else {
                onValueChanged.accept((double)Math.round(currentVal));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalHeight = scrollableWidgets.size() * 24;
        double maxScroll = Math.max(0, totalHeight - 130);
        if (maxScroll > 0) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 14.0), 0, maxScroll);
            updateScrollPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScrollPositions() {
        int baseY = panelY1 + 5;
        int index = 0;
        for (AbstractWidget widget : scrollableWidgets) {
            int targetY = baseY + (index * 24) - (int) this.scrollAmount;
            boolean isVisible = (targetY >= panelY1 && targetY <= panelY2 - 18);
            widget.visible = isVisible;
            widget.active = isVisible;
            widget.setY(targetY);
            index++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(0, 0, this.width, this.height, 0xBD090D16);

        int panelWidth = 260;
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 115;

        drawModernCard(guiGraphics, leftX, topY, leftX + panelWidth, topY + 220, "⚙ Konfigurasi Lanjutan & Toggles");

        guiGraphics.enableScissor(leftX + 6, panelY1, leftX + panelWidth - 6, panelY2);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
    }

    private void drawModernCard(GuiGraphics g, int x1, int y1, int x2, int y2, String title) {
        g.fill(x1, y1, x2, y2, 0xFF1E293B);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF0F172A);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 24, 0xFF1E293B);
        g.fill(x1 + 1, y1 + 24, x2 - 1, y1 + 25, 0xFF334155);

        int titleWidth = this.font.width(title);
        int titleX = x1 + ((x2 - x1) - titleWidth) / 2;
        g.drawString(this.font, title, titleX, y1 + 8, 0xFF38BDF8, true);
    }
}