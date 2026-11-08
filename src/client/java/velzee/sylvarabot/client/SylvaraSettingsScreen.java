package velzee.sylvarabot.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class SylvaraSettingsScreen extends Screen {
    private final Screen parentScreen;
    private double scrollAmount = 0;
    private int panelY1, panelY2;
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();

    public SylvaraSettingsScreen(Screen parentScreen) {
        super(Component.literal("Sylvara Settings & Toggles"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 240;
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 110;

        this.panelY1 = topY + 35;
        this.panelY2 = topY + 205;
        scrollableWidgets.clear();

        // Tombol Kembali ke Main GUI
        this.addRenderableWidget(Button.builder(Component.literal("§c← Kembali ke Menu Utama"), button -> {
            this.minecraft.setScreen(parentScreen);
        }).bounds(leftX, topY + 8, panelWidth, 20).build());

        int currentY = panelY1 + 10;

        // --- BAGIAN 1: GENERAL TOGGLES ---
        // Toggle Backpack
        Button backpackBtn = Button.builder(Component.literal(AutoLumberjackController.isHasBackpack() ? "Backpack: §aON" : "Backpack: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isHasBackpack();
            AutoLumberjackController.setHasBackpack(next);
            button.setMessage(Component.literal(next ? "Backpack: §aON" : "Backpack: §cOFF"));
        }).bounds(leftX + 10, currentY, panelWidth - 20, 18).build();
        this.addRenderableWidget(backpackBtn);
        scrollableWidgets.add(backpackBtn);
        currentY += 22;

        // Toggle Staff Pause
        Button staffBtn = Button.builder(Component.literal(AutoLumberjackController.isAntiStaffPauseEnabled() ? "Staff Pause: §aON" : "Staff Pause: §cOFF"), button -> {
            boolean next = !AutoLumberjackController.isAntiStaffPauseEnabled();
            AutoLumberjackController.setAntiStaffPauseEnabled(next);
            button.setMessage(Component.literal(next ? "Staff Pause: §aON" : "Staff Pause: §cOFF"));
        }).bounds(leftX + 10, currentY, panelWidth - 20, 18).build();
        this.addRenderableWidget(staffBtn);
        scrollableWidgets.add(staffBtn);
        currentY += 28;

        // --- BAGIAN 2: MAKANAN (FOODS) ---
        // Header Makanan (Label teks statis lewat render)
        currentY += 15;

        for (AutoLumberjackController.ConsumableItem food : AutoLumberjackController.getFoodList()) {
            Button foodBtn = Button.builder(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleFood(food.name);
                button.setMessage(Component.literal("Food: " + food.name + " " + (food.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 10, currentY, panelWidth - 20, 18).build();
            this.addRenderableWidget(foodBtn);
            scrollableWidgets.add(foodBtn);
            currentY += 22;
        }

        // --- BAGIAN 3: MINUMAN (DRINKS) ---
        currentY += 15;

        for (AutoLumberjackController.ConsumableItem drink : AutoLumberjackController.getDrinkList()) {
            Button drinkBtn = Button.builder(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")), button -> {
                AutoLumberjackController.toggleDrink(drink.name);
                button.setMessage(Component.literal("Drink: " + drink.name + " " + (drink.enabled ? "§aON" : "§cOFF")));
            }).bounds(leftX + 10, currentY, panelWidth - 20, 18).build();
            this.addRenderableWidget(drinkBtn);
            scrollableWidgets.add(drinkBtn);
            currentY += 22;
        }

        updateScrollPositions();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double maxScroll = Math.max(0, 180); // Rentang scroll disesuaikan dengan isi list
        if (maxScroll > 0) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 14.0), 0, maxScroll);
            updateScrollPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScrollPositions() {
        int baseY = panelY1 + 10;
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

        int panelWidth = 240;
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 110;

        // Render Background Container Utama Pengaturan
        guiGraphics.fill(leftX, topY, leftX + panelWidth, topY + 215, 0xFF1E293B);
        guiGraphics.fill(leftX + 1, topY + 1, leftX + panelWidth - 1, topY + 214, 0xFF0F172A);

        guiGraphics.enableScissor(leftX + 5, panelY1, leftX + panelWidth - 5, panelY2);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
    }
}