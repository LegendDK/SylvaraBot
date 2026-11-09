package velzee.sylvarabot.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SylvaraSettingsScreen extends Screen {
    private final Screen parentScreen;
    private double scrollAmount = 0;
    private final List<GuiElementInfo> allElements = new ArrayList<>();
    private Button backButton;

    private static class GuiElementInfo {
        final AbstractWidget widget;
        final String categoryTitle;
        int renderX, renderY;

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
        int panelWidth = 210;

        int leftX = centerX - 225;
        int rightX = centerX + 15;
        int topY = centerY - 115;

        allElements.clear();
        this.clearWidgets();

        // ==========================================
        // KOTAK 1 (KIRI ATAS): GENERAL TOGGLES (Termasuk Auto Eat & Auto Drink)
        // ==========================================
        int c1Y = topY + 28;

        Button autoEatBtn = Button.builder(Component.literal(AutoLumberjackController.isAutoEatEnabled() ? "Auto Eat: §aON" : "Auto Eat: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isAutoEatEnabled();
            AutoLumberjackController.setAutoEatEnabled(next);
            button.setMessage(Component.literal(next ? "Auto Eat: §aON" : "Auto Eat: §cOFF"));
        }).bounds(leftX + 12, c1Y, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(autoEatBtn, "General Toggles"));
        c1Y += 22;

        Button autoDrinkBtn = Button.builder(Component.literal(AutoLumberjackController.isAutoDrinkEnabled() ? "Auto Drink: §aON" : "Auto Drink: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isAutoDrinkEnabled();
            AutoLumberjackController.setAutoDrinkEnabled(next);
            button.setMessage(Component.literal(next ? "Auto Drink: §aON" : "Auto Drink: §cOFF"));
        }).bounds(leftX + 12, c1Y, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(autoDrinkBtn, "General Toggles"));
        c1Y += 22;

        Button backpackBtn = Button.builder(Component.literal(AutoLumberjackController.isHasBackpack() ? "Backpack: §aON" : "Backpack: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isHasBackpack();
            AutoLumberjackController.setHasBackpack(next);
            button.setMessage(Component.literal(next ? "Backpack: §aON" : "Backpack: §cOFF"));
        }).bounds(leftX + 12, c1Y, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(backpackBtn, "General Toggles"));
        c1Y += 22;

        Button staffBtn = Button.builder(Component.literal(AutoLumberjackController.isAntiStaffPauseEnabled() ? "Staff Pause: §aON" : "Staff Pause: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isAntiStaffPauseEnabled();
            AutoLumberjackController.setAntiStaffPauseEnabled(next);
            button.setMessage(Component.literal(next ? "Staff Pause: §aON" : "Staff Pause: §cOFF"));
        }).bounds(leftX + 12, c1Y, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(staffBtn, "General Toggles"));
        c1Y += 22;

        Button safetyBtn = Button.builder(Component.literal(AutoLumberjackController.isSafetyDisconnectEnabled() ? "Safety Disconnect: §aON" : "Safety Disconnect: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isSafetyDisconnectEnabled();
            AutoLumberjackController.setSafetyDisconnectEnabled(next);
            button.setMessage(Component.literal(next ? "Safety Disconnect: §aON" : "Safety Disconnect: §cOFF"));
        }).bounds(leftX + 12, c1Y, panelWidth - 24, 18).build();
        allElements.add(new GuiElementInfo(safetyBtn, "General Toggles"));


        // ==========================================
        // KOTAK 2 (KIRI BAWAH): FOOD TOGGLES
        // ==========================================
        int c2Y = topY + 156;
        for (AutoLumberjackController.ConsumableItem food : AutoLumberjackController.getFoodList()) {
            Button foodBtn = Button.builder(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleFood(food.name);
                button.setMessage(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 12, c2Y, panelWidth - 24, 18).build();
            allElements.add(new GuiElementInfo(foodBtn, "Food Toggles"));
            c2Y += 22;
        }


        // ==========================================
        // KOTAK 3 (KANAN ATAS): DRINK TOGGLES
        // ==========================================
        int c3Y = topY + 28;
        for (AutoLumberjackController.ConsumableItem drink : AutoLumberjackController.getDrinkList()) {
            Button drinkBtn = Button.builder(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleDrink(drink.name);
                button.setMessage(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")));
            }).bounds(rightX + 12, c3Y, panelWidth - 24, 18).build();
            allElements.add(new GuiElementInfo(drinkBtn, "Drink Toggles"));
            c3Y += 22;
        }


        // ==========================================
        // KOTAK 4 (KANAN BAWAH): ADVANCED SLIDERS
        // ==========================================
        int c4Y = topY + 130;

        double clickDelayProgress = (double)(AutoLumberjackController.getMinigameClickDelay() - 5) / 25.0;
        CustomSlider clickDelaySlider = new CustomSlider(rightX + 12, c4Y, panelWidth - 24, 18, clickDelayProgress, "Minigame Delay", " ticks", 5.0, 30.0, (Double val) -> {
            AutoLumberjackController.setMinigameClickDelay(val.intValue());
        });
        allElements.add(new GuiElementInfo(clickDelaySlider, "Advanced Sliders"));
        c4Y += 22;

        double cameraSpeedProgress = (AutoLumberjackController.getCameraAlignSpeed() - 0.05f) / 0.45f;
        CustomSlider cameraSpeedSlider = new CustomSlider(rightX + 12, c4Y, panelWidth - 24, 18, cameraSpeedProgress, "Camera Speed", "", 0.05, 0.50, (Double val) -> {
            AutoLumberjackController.setCameraAlignSpeed(val.floatValue());
        });
        allElements.add(new GuiElementInfo(cameraSpeedSlider, "Advanced Sliders"));
        c4Y += 22;

        double timeoutProgress = (double)(AutoLumberjackController.getGuiTimeoutLimit() - 100) / 400.0;
        CustomSlider timeoutSlider = new CustomSlider(rightX + 12, c4Y, panelWidth - 24, 18, timeoutProgress, "GUI Timeout", " ticks", 100.0, 500.0, (Double val) -> {
            AutoLumberjackController.setGuiTimeoutLimit(val.intValue());
        });
        allElements.add(new GuiElementInfo(timeoutSlider, "Advanced Sliders"));

        for (GuiElementInfo info : allElements) {
            this.addRenderableWidget(info.widget);
        }

        // ==========================================
        // TOMBOL KEMBALI DI TENGAH BAWAH
        // ==========================================
        int btnWidth = 160;
        int btnHeight = 20;
        int btnX = centerX - (btnWidth / 2);
        int btnY = topY + 236;

        this.backButton = Button.builder(Component.literal("§cKembali ke Menu Utama"), button -> {
            this.minecraft.setScreen(parentScreen);
        }).bounds(btnX, btnY, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.backButton);
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
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(0, 0, this.width, this.height, 0xCE070A14);

        int topY = centerY - 115;
        int panelWidth = 210;
        int leftX = centerX - 225;
        int rightX = centerX + 15;

        String modVersion = FabricLoader.getInstance()
                .getModContainer("sylvarabot")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0");

        guiGraphics.drawString(this.font, "§lSylvara§bGUI §8| §7v" + modVersion, leftX, topY - 14, 0xFF94A3B8, false);

        drawModernCard(guiGraphics, leftX, topY, leftX + panelWidth, topY + 130, "⚙ General Toggles");
        drawModernCard(guiGraphics, leftX, topY + 136, leftX + panelWidth, topY + 230, "🍖 Food Toggles");

        drawModernCard(guiGraphics, rightX, topY, rightX + panelWidth, topY + 130, "🥤 Drink Toggles");
        drawModernCard(guiGraphics, rightX, topY + 136, rightX + panelWidth, topY + 230, "⚡ Advanced Sliders");

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawModernCard(GuiGraphics g, int x1, int y1, int x2, int y2, String title) {
        g.fill(x1, y1, x2, y2, 0xFF1E293B);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF0F172A);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 22, 0xFF1E293B);
        g.fill(x1 + 1, y1 + 22, x2 - 1, y1 + 23, 0xFF334155);

        int titleWidth = this.font.width(title);
        int titleX = x1 + ((x2 - x1) - titleWidth) / 2;
        g.drawString(this.font, title, titleX, y1 + 7, 0xFF38BDF8, true);
    }
}