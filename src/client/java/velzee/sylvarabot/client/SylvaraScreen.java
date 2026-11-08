package velzee.sylvarabot.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class SylvaraScreen extends Screen {

    private static final Identifier MOD_LOGO = Identifier.fromNamespaceAndPath("sylvarabot", "icon.png");
    private int editingIndex = -1;
    private EditBox nameInputBox;
    private double scrollAmount = 0;
    private int rightPanelY1;
    private int rightPanelY2;

    private static class WaypointRowHolder {
        int originalY;
        List<AbstractWidget> widgets = new ArrayList<>();
    }

    private final List<WaypointRowHolder> rowHolders = new ArrayList<>();

    public SylvaraScreen() {
        super(Component.literal("Sylvara GUI"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        rowHolders.clear();

        int leftX = centerX - 215;
        int rightX = centerX + 15;
        int panelWidth = 200;

        int topY = centerY - 105;

        // --- PANEL 1: CONTROL PANEL (Kiri Atas) ---
        int p1Y = topY;
        this.addRenderableWidget(Button.builder(Component.literal("§a▶ Start Farm"), button -> {
            MovementController.startRoute(true, true);
            this.onClose();
        }).bounds(leftX + 12, p1Y + 28, panelWidth - 24, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§b⚡ Auto Chain"), button -> {
            MovementController.stop();
            AutoLumberjackController.startAutoChainSmart();
            this.onClose();
        }).bounds(leftX + 12, p1Y + 52, panelWidth - 24, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c■ Stop Bot"), button -> {
            MovementController.stop();
            AutoLumberjackController.stopTask();
            this.onClose();
        }).bounds(leftX + 12, p1Y + 76, panelWidth - 24, 20).build());

        // --- PANEL 2: BENCH & SETTINGS (Kiri Bawah) ---
        int p2Y = topY + 105;

        // Tombol Set Bench
        this.addRenderableWidget(Button.builder(Component.literal("Set Saw"), button -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                AutoLumberjackController.setSawBenchWaypoint(
                        this.minecraft.player.blockPosition().relative(this.minecraft.player.getDirection()),
                        this.minecraft.player.getYRot(),
                        this.minecraft.player.getXRot()
                );
            }
        }).bounds(leftX + 12, p2Y + 28, 85, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Set Craft"), button -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                AutoLumberjackController.setCraftBenchWaypoint(
                        this.minecraft.player.blockPosition().relative(this.minecraft.player.getDirection()),
                        this.minecraft.player.getYRot(),
                        this.minecraft.player.getXRot()
                );
            }
        }).bounds(leftX + 103, p2Y + 28, 85, 18).build());

        // Toggle Chain Mode (Full / Saw Only / Craft Only)
        this.addRenderableWidget(Button.builder(Component.literal("Mode: §e" + AutoLumberjackController.getAutoChainMode().getName()), button -> {
            AutoLumberjackController.AutoChainMode nextMode;
            switch (AutoLumberjackController.getAutoChainMode()) {
                case FULL_CHAIN: nextMode = AutoLumberjackController.AutoChainMode.SAW_ONLY; break;
                case SAW_ONLY: nextMode = AutoLumberjackController.AutoChainMode.CRAFT_ONLY; break;
                default: nextMode = AutoLumberjackController.AutoChainMode.FULL_CHAIN; break;
            }
            AutoLumberjackController.setAutoChainMode(nextMode);
            button.setMessage(Component.literal("Mode: §e" + nextMode.getName()));
        }).bounds(leftX + 12, p2Y + 52, 176, 20).build());

        // Tombol untuk Membuka Container Pengaturan Makanan & Toggles Terpisah
        this.addRenderableWidget(Button.builder(Component.literal("⚙ Atur Makanan & Toggles"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new SylvaraSettingsScreen(this));
            }
        }).bounds(leftX + 12, p2Y + 78, 176, 22).build());


        // --- PANEL 3: WAYPOINT LIST (Kanan Full Tinggi) ---
        this.rightPanelY1 = topY;
        this.rightPanelY2 = topY + 205;
        int rightContentX = rightX + 12;

        this.addRenderableWidget(Button.builder(Component.literal("+ Add Position"), button -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                MovementController.addWaypoint(this.minecraft.player.blockPosition());
                this.rebuildWidgets();
            }
        }).bounds(rightContentX, rightPanelY1 + 26, 100, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("§cReset All"), button -> {
            MovementController.clearWaypoints();
            this.rebuildWidgets();
        }).bounds(rightContentX + 104, rightPanelY1 + 26, 72, 17).build());

        List<Waypoint> waypoints = MovementController.getWaypoints();
        int rowY = rightPanelY1 + 52;

        for (int i = 0; i < waypoints.size(); i++) {
            final int index = i;
            Waypoint wp = waypoints.get(i);
            WaypointRowHolder holder = new WaypointRowHolder();
            holder.originalY = rowY;

            // Tombol Naik (▲)
            Button upBtn = Button.builder(Component.literal("▲"), button -> {
                if (index > 0) {
                    MovementController.moveUp(index);
                    if (editingIndex == index) editingIndex = index - 1;
                    else if (editingIndex == index - 1) editingIndex = index;
                    this.rebuildWidgets();
                }
            }).bounds(rightContentX + 82, rowY - 1, 16, 18).build();
            this.addRenderableWidget(upBtn);
            holder.widgets.add(upBtn);

            // Tombol Turun (▼)
            Button downBtn = Button.builder(Component.literal("▼"), button -> {
                if (index < waypoints.size() - 1) {
                    MovementController.moveDown(index);
                    if (editingIndex == index) editingIndex = index + 1;
                    else if (editingIndex == index + 1) editingIndex = index;
                    this.rebuildWidgets();
                }
            }).bounds(rightContentX + 100, rowY - 1, 16, 18).build();
            this.addRenderableWidget(downBtn);
            holder.widgets.add(downBtn);

            if (editingIndex == index) {
                nameInputBox = new EditBox(this.font, rightContentX, rowY, 78, 16, Component.literal("Nama"));
                nameInputBox.setValue(wp.getName());
                this.addRenderableWidget(nameInputBox);
                holder.widgets.add(nameInputBox);

                Button okBtn = Button.builder(Component.literal("§aOK"), button -> {
                    MovementController.renameWaypoint(index, nameInputBox.getValue());
                    editingIndex = -1;
                    this.rebuildWidgets();
                }).bounds(rightContentX + 118, rowY - 1, 28, 18).build();
                this.addRenderableWidget(okBtn);
                holder.widgets.add(okBtn);
            } else {
                Button renameBtn = Button.builder(Component.literal("Edit"), button -> {
                    editingIndex = index;
                    this.rebuildWidgets();
                }).bounds(rightContentX + 118, rowY - 1, 30, 18).build();
                this.addRenderableWidget(renameBtn);
                holder.widgets.add(renameBtn);
            }

            Button xBtn = Button.builder(Component.literal("§cX"), button -> {
                MovementController.removeWaypoint(index);
                editingIndex = -1;
                this.rebuildWidgets();
            }).bounds(rightContentX + 150, rowY - 1, 18, 18).build();
            this.addRenderableWidget(xBtn);
            holder.widgets.add(xBtn);

            rowHolders.add(holder);
            rowY += 22;
        }
        updateScrollPositions();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalListHeight = MovementController.getWaypoints().size() * 22;
        double maxScroll = Math.max(0, totalListHeight - 145);
        if (maxScroll > 0) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - (scrollY * 14.0), 0, maxScroll);
            updateScrollPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void updateScrollPositions() {
        int minY = this.rightPanelY1 + 50;
        int maxY = this.rightPanelY2 - 15;
        for (WaypointRowHolder holder : rowHolders) {
            int currentY = holder.originalY - (int) this.scrollAmount;
            boolean isVisible = (currentY >= minY && currentY <= maxY);
            for (AbstractWidget widget : holder.widgets) {
                widget.visible = isVisible;
                widget.active = isVisible;
                widget.setY(currentY - 1);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(0, 0, this.width, this.height, 0xBD090D16);

        int topY = centerY - 105;
        guiGraphics.blit(MOD_LOGO, centerX - 85, topY - 24, 0, 0, 18, 18, 18, 18);
        guiGraphics.drawString(this.font, "§lSylvara§bGUI §8| §7v1.0", centerX - 62, topY - 19, 0xFFFFFFFF, true);

        int leftX = centerX - 215;
        int rightX = centerX + 15;
        int panelWidth = 200;

        drawModernCard(guiGraphics, leftX, topY, leftX + panelWidth, topY + 98, "Control Panel");
        drawModernCard(guiGraphics, leftX, topY + 105, leftX + panelWidth, topY + 205, "Bench & Settings");
        drawModernCard(guiGraphics, rightX, topY, rightX + panelWidth, topY + 205, "Waypoint Management");

        int scissorY1 = topY + 48;
        int scissorY2 = topY + 198;
        guiGraphics.enableScissor(rightX + 6, scissorY1, rightX + panelWidth - 6, scissorY2);

        List<Waypoint> waypoints = MovementController.getWaypoints();
        int rightContentX = rightX + 12;
        int rowY = this.rightPanelY1 + 52 - (int) this.scrollAmount;

        for (int i = 0; i < waypoints.size(); i++) {
            if (editingIndex != i) {
                String label = (i + 1) + ". " + waypoints.get(i).getName();
                if (label.length() > 11) label = label.substring(0, 9) + "..";
                guiGraphics.drawString(this.font, label, rightContentX + 4, rowY + 4, 0xFFE2E8F0, true);
            }
            rowY += 22;
        }
        guiGraphics.disableScissor();

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