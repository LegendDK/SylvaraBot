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
    private final List<GuiElementInfo> allElements = new ArrayList<>();
    private Button backButton;

    private static class GuiElementInfo {
        final AbstractWidget widget;
        final String categoryTitle;
        int renderY;

        public GuiElementInfo(AbstractWidget widget, String categoryTitle) {
            this.widget = widget;
            this.categoryTitle = categoryTitle;
        }
    }

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
        int topY = centerY - 125;

        // Batas area scroll
        this.panelY1 = topY + 22;
        this.panelY2 = topY + 215;
        allElements.clear();
        this.clearWidgets();

        int trackingY = panelY1 + 10;

        // ==========================================
        // CONTAINER 1: GENERAL TOGGLES
        // ==========================================
        trackingY += 16;

        Button backpackBtn = Button.builder(Component.literal(AutoLumberjackController.isHasBackpack() ? "Backpack: §aON" : "Backpack: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isHasBackpack();
            AutoLumberjackController.setHasBackpack(next);
            button.setMessage(Component.literal(next ? "Backpack: §aON" : "Backpack: §cOFF"));
        }).bounds(leftX + 12, trackingY, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(backpackBtn, "General Toggles"));
        trackingY += 22;

        Button staffBtn = Button.builder(Component.literal(AutoLumberjackController.isAntiStaffPauseEnabled() ? "Staff Pause: §aON" : "Staff Pause: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isAntiStaffPauseEnabled();
            AutoLumberjackController.setAntiStaffPauseEnabled(next);
            button.setMessage(Component.literal(next ? "Staff Pause: §aON" : "Staff Pause: §cOFF"));
        }).bounds(leftX + 12, trackingY, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(staffBtn, "General Toggles"));
        trackingY += 22;

        Button safetyBtn = Button.builder(Component.literal(AutoLumberjackController.isSafetyDisconnectEnabled() ? "Safety Disconnect: §aON" : "Safety Disconnect: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isSafetyDisconnectEnabled();
            AutoLumberjackController.setSafetyDisconnectEnabled(next);
            button.setMessage(Component.literal(next ? "Safety Disconnect: §aON" : "Safety Disconnect: §cOFF"));
        }).bounds(leftX + 12, trackingY, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(safetyBtn, "General Toggles"));
        trackingY += 30;


        // ==========================================
        // CONTAINER 2: FOOD ONLY
        // ==========================================
        trackingY += 16;
        for (AutoLumberjackController.ConsumableItem food : AutoLumberjackController.getFoodList()) {
            Button foodBtn = Button.builder(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleFood(food.name);
                button.setMessage(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 12, trackingY, panelWidth - 24, 18).build();
            allElements.add(new GuiElementInfo(foodBtn, "Food Toggles"));
            trackingY += 22;
        }
        trackingY += 10;


        // ==========================================
        // CONTAINER 3: DRINK ONLY
        // ==========================================
        trackingY += 16;
        for (AutoLumberjackController.ConsumableItem drink : AutoLumberjackController.getDrinkList()) {
            Button drinkBtn = Button.builder(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleDrink(drink.name);
                button.setMessage(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 12, trackingY, panelWidth - 24, 18).build();
            allElements.add(new GuiElementInfo(drinkBtn, "Drink Toggles"));
            trackingY += 22;
        }
        trackingY += 10;


        // ==========================================
        // CONTAINER 4: ADVANCED TIMERS & SLIDERS
        // ==========================================
        trackingY += 16;

        double clickDelayProgress = (double)(AutoLumberjackController.getMinigameClickDelay() - 5) / 25.0;
        CustomSlider clickDelaySlider = new CustomSlider(leftX + 12, trackingY, panelWidth - 24, 18, clickDelayProgress, "Minigame Delay", " ticks", 5.0, 30.0, (Double val) -> {
            AutoLumberjackController.setMinigameClickDelay(val.intValue());
        });
        allElements.add(new GuiElementInfo(clickDelaySlider, "Advanced Precision Sliders"));
        trackingY += 22;

        double cameraSpeedProgress = (AutoLumberjackController.getCameraAlignSpeed() - 0.05f) / 0.45f;
        CustomSlider cameraSpeedSlider = new CustomSlider(leftX + 12, trackingY, panelWidth - 24, 18, cameraSpeedProgress, "Camera Speed", "", 0.05, 0.50, (Double val) -> {
            AutoLumberjackController.setCameraAlignSpeed(val.floatValue());
        });
        allElements.add(new GuiElementInfo(cameraSpeedSlider, "Advanced Precision Sliders"));
        trackingY += 22;

        double timeoutProgress = (double)(AutoLumberjackController.getGuiTimeoutLimit() - 100) / 400.0;
        CustomSlider timeoutSlider = new CustomSlider(leftX + 12, trackingY, panelWidth - 24, 18, timeoutProgress, "GUI Timeout", " ticks", 100.0, 500.0, (Double val) -> {
            AutoLumberjackController.setGuiTimeoutLimit(val.intValue());
        });
        allElements.add(new GuiElementInfo(timeoutSlider, "Advanced Precision Sliders"));
        trackingY += 25;

        for (GuiElementInfo info : allElements) {
            this.addRenderableWidget(info.widget);
        }

        // ==========================================
        // TOMBOL KEMBALI DI TENGAH BAWAH (ALA MINECRAFT)
        // ==========================================
        int btnWidth = 160; // Lebar tombol standar ala menu Minecraft
        int btnHeight = 20;
        int btnX = centerX - (btnWidth / 2);
        int btnY = topY + 222;

        this.backButton = Button.builder(Component.literal("§cKembali ke Menu Utama"), button -> {
            this.minecraft.setScreen(parentScreen);
        }).bounds(btnX, btnY, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.backButton);

        updateScrollPositions();
    }

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
        int totalHeight = allElements.size() * 24 + 140;
        double maxScroll = Math.max(0, totalHeight - 160);
        if (maxScroll > 0) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 14.0), 0, maxScroll);
            updateScrollPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScrollPositions() {
        int baseY = panelY1 + 18;
        int index = 0;
        String lastCategory = "";

        for (GuiElementInfo info : allElements) {
            if (!info.categoryTitle.equals(lastCategory)) {
                if (index > 0) baseY += 24;
                lastCategory = info.categoryTitle;
            }

            int targetY = baseY + (index * 24) - (int) this.scrollAmount;
            boolean isVisible = (targetY >= panelY1 && targetY <= panelY2 - 16);
            info.widget.visible = isVisible;
            info.widget.active = isVisible;
            info.widget.setY(targetY);
            info.renderY = targetY;
            index++;
        }

        if (this.backButton != null) {
            this.backButton.visible = true;
            this.backButton.active = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(0, 0, this.width, this.height, 0xBD090D16);

        int panelWidth = 260;
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 125;

        // Render Panel Utama
        guiGraphics.fill(leftX, topY, leftX + panelWidth, topY + 250, 0xFF0F172A);
        guiGraphics.renderOutline(leftX, topY, panelWidth, 250, 0xFF334155);

        // Render Header Judul Utama
        guiGraphics.drawString(this.font, "§b⚙ Konfigurasi Lanjutan & Toggles", leftX + 14, topY + 8, 0xFF38BDF8, true);

        // Render Card Container Terpisah
        guiGraphics.enableScissor(leftX + 4, panelY1, leftX + panelWidth - 4, panelY2);

        String currentCat = "";
        int cardStartY = 0;
        for (int i = 0; i < allElements.size(); i++) {
            GuiElementInfo info = allElements.get(i);
            if (!info.categoryTitle.equals(currentCat)) {
                if (!currentCat.isEmpty()) {
                    drawCardBox(guiGraphics, leftX + 6, cardStartY - 16, leftX + panelWidth - 6, allElements.get(i - 1).renderY + 20, currentCat);
                }
                currentCat = info.categoryTitle;
                cardStartY = info.renderY;
            }
            if (i == allElements.size() - 1) {
                drawCardBox(guiGraphics, leftX + 6, cardStartY - 16, leftX + panelWidth - 6, info.renderY + 20, currentCat);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();

        // Render ulang tombol kembali di luar scissor agar selalu tampil jelas di tengah bawah
        this.backButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawCardBox(GuiGraphics g, int x1, int y1, int x2, int y2, String title) {
        g.fill(x1, y1, x2, y2, 0xFF1E293B);
        g.renderOutline(x1, y1, x2 - x1, y2 - y1, 0xFF475569);
        g.drawString(this.font, "§b" + title, x1 + 8, y1 + 4, 0xFF38BDF8, false);
    }
}