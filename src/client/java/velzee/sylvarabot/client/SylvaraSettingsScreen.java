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

    // Menyimpan widget beserta kategori teks-nya agar bisa dirender dengan cantik
    private static class RenderElement {
        boolean isHeader;
        String text;
        AbstractWidget widget;

        // Konstruktor untuk Header Kategori
        public RenderElement(String text) {
            this.isHeader = true;
            this.text = text;
            this.widget = null;
        }

        // Konstruktor untuk Tombol Toggle
        public RenderElement(AbstractWidget widget) {
            this.isHeader = false;
            this.text = "";
            this.widget = widget;
        }
    }

    private final List<RenderElement> elements = new ArrayList<>();

    public SylvaraSettingsScreen(Screen parentScreen) {
        super(Component.literal("Sylvara Settings & Toggles"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 260; // Diperlebar sedikit agar lebih lega dan elegan
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 115;

        this.panelY1 = topY + 34;
        this.panelY2 = topY + 195;
        elements.clear();

        // --- KATEGORI 1: GENERAL SYSTEM ---
        elements.add(new RenderElement("⚙ PENGATURAN UMUM"));

        Button backpackBtn = Button.builder(Component.literal(AutoLumberjackController.isHasBackpack() ? "Backpack Integration: §a[AKTIF]" : "Backpack Integration: §c[MATI]"), button -> {
            boolean next = !AutoLumberjackController.isHasBackpack();
            AutoLumberjackController.setHasBackpack(next);
            button.setMessage(Component.literal(next ? "Backpack Integration: §a[AKTIF]" : "Backpack Integration: §c[MATI]"));
        }).bounds(leftX + 12, 0, panelWidth - 24, 20).build();
        this.addRenderableWidget(backpackBtn);
        elements.add(new RenderElement(backpackBtn));

        Button staffBtn = Button.builder(Component.literal(AutoLumberjackController.isAntiStaffPauseEnabled() ? "Anti-Staff Pause: §a[AKTIF]" : "Anti-Staff Pause: §c[MATI]"), button -> {
            boolean next = !AutoLumberjackController.isAntiStaffPauseEnabled();
            AutoLumberjackController.setAntiStaffPauseEnabled(next);
            button.setMessage(Component.literal(next ? "Anti-Staff Pause: §a[AKTIF]" : "Anti-Staff Pause: §c[MATI]"));
        }).bounds(leftX + 12, 0, panelWidth - 24, 20).build();
        this.addRenderableWidget(staffBtn);
        elements.add(new RenderElement(staffBtn));

        // --- KATEGORI 2: MAKANAN (FOODS) ---
        elements.add(new RenderElement("🍖 PILIHAN MAKANAN OTOMATIS"));
        for (AutoLumberjackController.ConsumableItem food : AutoLumberjackController.getFoodList()) {
            Button foodBtn = Button.builder(Component.literal("• " + food.name + " : " + (food.enabled ? "§a[YA]" : "§c[TIDAK]")), button -> {
                AutoLumberjackController.toggleFood(food.name);
                button.setMessage(Component.literal("• " + food.name + " : " + (food.enabled ? "§a[YA]" : "§c[TIDAK]")));
            }).bounds(leftX + 12, 0, panelWidth - 24, 18).build();
            this.addRenderableWidget(foodBtn);
            elements.add(new RenderElement(foodBtn));
        }

        // --- KATEGORI 3: MINUMAN (DRINKS) ---
        elements.add(new RenderElement("🥤 PILIHAN MINUMAN OTOMATIS"));
        for (AutoLumberjackController.ConsumableItem drink : AutoLumberjackController.getDrinkList()) {
            Button drinkBtn = Button.builder(Component.literal("• " + drink.name + " : " + (drink.enabled ? "§a[YA]" : "§c[TIDAK]")), button -> {
                AutoLumberjackController.toggleDrink(drink.name);
                button.setMessage(Component.literal("• " + drink.name + " : " + (drink.enabled ? "§a[YA]" : "§c[TIDAK]")));
            }).bounds(leftX + 12, 0, panelWidth - 24, 18).build();
            this.addRenderableWidget(drinkBtn);
            elements.add(new RenderElement(drinkBtn));
        }

        // Tombol Kembali ke Menu Utama (Di luar area scroll, fixed di bawah)
        this.addRenderableWidget(Button.builder(Component.literal("§c← Kembali ke Menu Utama"), button -> {
            this.minecraft.setScreen(parentScreen);
        }).bounds(leftX + 12, topY + 202, panelWidth - 24, 20).build());

        updateElementPositions();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalHeight = calculateTotalContentHeight();
        double maxScroll = Math.max(0, totalHeight - 150);
        if (maxScroll > 0) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 16.0), 0, maxScroll);
            updateElementPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int calculateTotalContentHeight() {
        int height = 0;
        for (RenderElement el : elements) {
            height += el.isHeader ? 22 : 24;
        }
        return height;
    }

    private void updateElementPositions() {
        int centerX = this.width / 2;
        int panelWidth = 260;
        int leftX = centerX - (panelWidth / 2);
        int currentY = panelY1 + 6 - (int) this.scrollAmount;

        for (RenderElement el : elements) {
            int elementHeight = el.isHeader ? 18 : 20;
            boolean isVisible = (currentY + elementHeight >= panelY1 && currentY <= panelY2);

            if (!el.isHeader && el.widget != null) {
                el.widget.visible = isVisible;
                el.widget.active = isVisible;
                el.widget.setY(currentY);
                el.widget.setX(leftX + 12);
            }
            currentY += el.isHeader ? 22 : 24;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Background gelap transparan berkelas
        guiGraphics.fill(0, 0, this.width, this.height, 0xCE070A14);

        int panelWidth = 260;
        int leftX = centerX - (panelWidth / 2);
        int topY = centerY - 115;

        // Render Bingkai Kartu Utama dengan Efek Estetik
        drawModernCard(guiGraphics, leftX, topY, leftX + panelWidth, topY + 230, "⚡ Manajemen Makanan, Minuman & Toggles");

        // Area Scissor untuk konten dalam kartu
        guiGraphics.enableScissor(leftX + 6, panelY1, leftX + panelWidth - 6, panelY2);

        int currentY = panelY1 + 6 - (int) this.scrollAmount;
        for (RenderElement el : elements) {
            if (el.isHeader) {
                // Render Header Kategori dengan aksen warna cyan dan garis bawah tipis
                if (currentY + 16 >= panelY1 && currentY <= panelY2) {
                    guiGraphics.drawString(this.font, "§l" + el.text, leftX + 14, currentY + 4, 0xFF38BDF8, true);
                    guiGraphics.fill(leftX + 14, currentY + 16, leftX + panelWidth - 14, currentY + 17, 0x3338BDF8);
                }
                currentY += 22;
            } else {
                currentY += 24;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
    }

    private void drawModernCard(GuiGraphics g, int x1, int y1, int x2, int y2, String title) {
        // Outer Shadow / Border halus
        g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF0A0F1D);
        // Background utama
        g.fill(x1, y1, x2, y2, 0xFF1E293B);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF0B132B);
        // Header Box Title Bar
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 26, 0xFF1C2541);
        g.fill(x1 + 1, y1 + 26, x2 - 1, y1 + 27, 0xFF48CAE4);

        int titleWidth = this.font.width(title);
        int titleX = x1 + ((x2 - x1) - titleWidth) / 2;
        g.drawString(this.font, title, titleX, y1 + 8, 0xFF90E0EF, true);
    }


}