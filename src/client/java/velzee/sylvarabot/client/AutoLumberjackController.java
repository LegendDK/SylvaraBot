package velzee.sylvarabot.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoLumberjackController {

    public enum State {
        IDLE,
        WALKING_TO_BENCH,
        ALIGNING_CAMERA,
        RIGHT_CLICKING,
        SOLVING_NEBANG_GUI,
        SOLVING_MINIGAME_GUI,
        WAITING_PROGRESS,
        DRINKING,
        EATING,
        OPENING_RANSEL_WITHDRAW,
        WITHDRAWING_MATERIAL_FROM_RANSEL,
        OPENING_RANSEL_DRINK_WITHDRAW,
        WITHDRAWING_DRINK_FROM_RANSEL,
        OPENING_RANSEL_FOOD_WITHDRAW,
        WITHDRAWING_FOOD_FROM_RANSEL,
        AFK_TIMER
    }

    private static State currentState = State.IDLE;
    private static boolean wasWalkingBeforeConsuming = false;
    private static boolean wasTaskingBeforeConsuming = false;
    private static boolean wasStaticBeforeConsuming = false;

    private static boolean autoEatEnabled = true;
    private static boolean autoDrinkEnabled = true;

    private static BlockPos targetBlockPos = null;
    private static float targetYaw = 0.0f;
    private static float targetPitch = 0.0f;

    private static BlockPos sawBenchPos = null;
    private static float sawYaw = 0.0f, sawPitch = 0.0f;

    private static BlockPos craftBenchPos = null;
    private static float craftYaw = 0.0f, craftPitch = 0.0f;

    private static final File BENCH_CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("sylvarabot_benches.json")
            .toFile();

    private static class BenchConfigData {
        int sawX, sawY, sawZ;
        float sawYaw, sawPitch;
        boolean hasSaw;
        int craftX, craftY, craftZ;
        float craftYaw, craftPitch;
        boolean hasCraft;
    }

    public static void saveBenches() {
        try {
            BenchConfigData data = new BenchConfigData();
            if (sawBenchPos != null) {
                data.sawX = sawBenchPos.getX();
                data.sawY = sawBenchPos.getY();
                data.sawZ = sawBenchPos.getZ();
                data.sawYaw = sawYaw;
                data.sawPitch = sawPitch;
                data.hasSaw = true;
            }
            if (craftBenchPos != null) {
                data.craftX = craftBenchPos.getX();
                data.craftY = craftBenchPos.getY();
                data.craftZ = craftBenchPos.getZ();
                data.craftYaw = craftYaw;
                data.craftPitch = craftPitch;
                data.hasCraft = true;
            }
            try (FileWriter writer = new FileWriter(BENCH_CONFIG_FILE)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadBenches() {
        if (!BENCH_CONFIG_FILE.exists()) return;
        try (FileReader reader = new FileReader(BENCH_CONFIG_FILE)) {
            BenchConfigData data = new Gson().fromJson(reader, BenchConfigData.class);
            if (data != null) {
                if (data.hasSaw) {
                    sawBenchPos = new BlockPos(data.sawX, data.sawY, data.sawZ);
                    sawYaw = data.sawYaw;
                    sawPitch = data.sawPitch;
                }
                if (data.hasCraft) {
                    craftBenchPos = new BlockPos(data.craftX, data.craftY, data.craftZ);
                    craftYaw = data.craftYaw;
                    craftPitch = data.craftPitch;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TaskType currentTaskType = TaskType.NEBANG;
    private static boolean isStaticProcessing = false;

    private static int delayTicks = 0;
    private static int guiTimeoutTicks = 0;
    private static int retryCount = 0;
    private static int clicksDone = 0;

    private static int consumptionTicks = 0;
    private static int initialItemCount = 0;
    private static Item initialItemType = Items.AIR;

    private static int ranselTimeoutTicks = 0;
    private static int targetWithdrawCount = 0;
    private static int withdrawnSoFarCount = 0;

    private static int afkTicksRemaining = 0;
    private static final int THIRTY_MINUTES_TICKS = 36000;

    private static final RandomSource random = RandomSource.create();
    private static final Set<String> activeStaffOnDuty = new HashSet<>();
    private static boolean pausedByAdmin = false;

    public static boolean isAutoEatEnabled() { return autoEatEnabled; }
    public static void setAutoEatEnabled(boolean enabled) { autoEatEnabled = enabled; }

    public static boolean isAutoDrinkEnabled() { return autoDrinkEnabled; }
    public static void setAutoDrinkEnabled(boolean enabled) { autoDrinkEnabled = enabled; }

    public static State getCurrentState() { return currentState; }
    public static TaskType getCurrentTaskType() { return currentTaskType; }
    public static boolean isStaticProcessingActive() { return isStaticProcessing; }

    public static void setSawBenchWaypoint(BlockPos pos, float yaw, float pitch) {
        sawBenchPos = pos;
        sawYaw = yaw;
        sawPitch = pitch;
        saveBenches();
    }

    public static void setCraftBenchWaypoint(BlockPos pos, float yaw, float pitch) {
        craftBenchPos = pos;
        craftYaw = yaw;
        craftPitch = pitch;
        saveBenches();
    }

    public static boolean isFarmingActive() {
        return MovementController.isActive() && currentTaskType == TaskType.NEBANG;
    }

    public static boolean isBotActive() {
        return currentState != State.IDLE && currentState != State.AFK_TIMER;
    }

    public static void resetState() {
        currentState = State.IDLE;
        wasWalkingBeforeConsuming = false;
        wasTaskingBeforeConsuming = false;
        wasStaticBeforeConsuming = false;
        targetBlockPos = null;
        isStaticProcessing = false;
        delayTicks = 0;
        guiTimeoutTicks = 0;
        retryCount = 0;
        clicksDone = 0;
        consumptionTicks = 0;
        ranselTimeoutTicks = 0;
        targetWithdrawCount = 0;
        withdrawnSoFarCount = 0;
        afkTicksRemaining = 0;
        activeStaffOnDuty.clear();
        pausedByAdmin = false;
    }

    public static int getEmptyInventorySlots(net.minecraft.client.Minecraft client) {
        if (client.player == null) return 0;
        int emptyCount = 0;
        for (int i = 0; i < 36; i++) {
            if (client.player.getInventory().getItem(i).isEmpty()) {
                emptyCount++;
            }
        }
        return emptyCount;
    }

    public static boolean isLogItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String componentStr = stack.getComponents().toString();
        return componentStr.contains("LOGS") || componentStr.contains("1503.0") || stack.getHoverName().getString().toLowerCase().contains("pile of logs");
    }

    public static boolean isPlankItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String componentStr = stack.getComponents().toString();
        return componentStr.contains("WOOD_PLANKS") || componentStr.contains("1504.0") || stack.getHoverName().getString().toLowerCase().contains("wood planks");
    }

    public static boolean isBoxItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String componentStr = stack.getComponents().toString();
        return componentStr.contains("WOODEN_BOX") || componentStr.contains("1506.0") || stack.getHoverName().getString().toLowerCase().contains("wooden box");
    }

    public static boolean isDrinkItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String componentStr = stack.getComponents().toString();
        return componentStr.contains("animation=DRINK") || componentStr.contains("10053.0") || stack.getHoverName().getString().toLowerCase().contains("matcha milkshake");
    }

    public static boolean isFoodItem(ItemStack stack) {
        if (stack.isEmpty() || isDrinkItem(stack)) return false;
        String componentStr = stack.getComponents().toString();
        return componentStr.contains("animation=EAT") || componentStr.contains("10052.0") || stack.getHoverName().getString().toLowerCase().contains("roasted beef");
    }

    private static int countMaterialInInventory(net.minecraft.client.Minecraft client, String type) {
        if (client.player == null) return 0;
        int total = 0;
        int size = client.player.getInventory().getContainerSize();

        for (int i = 0; i < size; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (type.equalsIgnoreCase("LOG")) {
                if (isLogItem(stack)) total += stack.getCount();
            } else if (type.equalsIgnoreCase("PLANK")) {
                if (isPlankItem(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    private static int countMaterialTotal(net.minecraft.client.Minecraft client) {
        int total = countMaterialInInventory(client, "PLANK");
        if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
            int containerSlots = containerScreen.getMenu().slots.size() - 36;
            for (int i = 0; i < containerSlots; i++) {
                ItemStack stack = containerScreen.getMenu().slots.get(i).getItem();
                if (isPlankItem(stack)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    public static void selectAxeSlot(net.minecraft.client.Minecraft client) {
        if (client.player == null) return;

        for (int i = 0; i < 9; i++) {
            net.minecraft.world.item.ItemStack stack = client.player.getInventory().getItem(i);
            String itemName = stack.getItem().getDescriptionId().toLowerCase();
            if (itemName.contains("axe") || itemName.contains("kapak")) {
                client.player.getInventory().setSelectedSlot(1);
                return;
            }
        }

        client.player.getInventory().setSelectedSlot(1);
    }

    public static void startAutoChainSmart() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null) return;

        int plankCount = countMaterialInInventory(client, "PLANK");
        int logCount = countMaterialInInventory(client, "LOG");

        client.player.displayClientMessage(Component.literal("§b[SylvaraBot] AutoChain: Planks = " + plankCount + ", Logs = " + logCount), false);

        if (plankCount >= 576 && craftBenchPos != null) {
            client.player.displayClientMessage(Component.literal("§6[SylvaraBot] Planks >= 9 stack! Menuju Craft Bench..."), false);
            startStaticProcessing(TaskType.PLANK_TO_BOX);
        } else if (logCount > 0 && sawBenchPos != null) {
            client.player.displayClientMessage(Component.literal("§a[SylvaraBot] Planks < 9 stack. Memotong Log di Saw Bench sampai terkumpul 9 stack..."), false);
            startStaticProcessing(TaskType.LOG_TO_PLANK);
        } else {
            client.player.displayClientMessage(Component.literal("§e[SylvaraBot] Bahan kurang, mengambil dari ransel..."), false);
            saveCurrentBotState(client);
            openRanselCommand(client);
            currentState = State.OPENING_RANSEL_WITHDRAW;
            ranselTimeoutTicks = 60;
            currentTaskType = TaskType.AUTO_CHAIN;
            isStaticProcessing = true;
        }
    }

    private static boolean selectOrSwapItem(net.minecraft.client.Minecraft client, java.util.function.Predicate<ItemStack> matcher, int preferredHotbarSlot) {
        if (client.player == null || client.gameMode == null) return false;

        ItemStack current = client.player.getInventory().getItem(preferredHotbarSlot);
        if (matcher.test(current)) {
            client.player.getInventory().setSelectedSlot(preferredHotbarSlot);
            return true;
        }

        int size = client.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (matcher.test(stack)) {
                int slotInMenu = i < 9 ? 36 + i : i;
                client.gameMode.handleInventoryMouseClick(0, slotInMenu, preferredHotbarSlot, ClickType.SWAP, client.player);
                client.player.getInventory().setSelectedSlot(preferredHotbarSlot);
                return true;
            }
        }
        return false;
    }

    public static void startTask(BlockPos blockToInteract, float yaw, float pitch, TaskType taskType) {
        if (!activeStaffOnDuty.isEmpty() || currentState == State.DRINKING || currentState == State.EATING) return;

        isStaticProcessing = false;
        targetBlockPos = blockToInteract;
        targetYaw = yaw;
        targetPitch = pitch;
        currentTaskType = taskType;
        currentState = State.ALIGNING_CAMERA;
        clicksDone = 0;
        retryCount = 0;
        delayTicks = 0;
        guiTimeoutTicks = 0;
        afkTicksRemaining = 0;
    }

    public static void startStaticProcessing(TaskType taskType) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        isStaticProcessing = true;
        currentTaskType = taskType;

        if (taskType == TaskType.LOG_TO_PLANK && sawBenchPos != null) {
            targetBlockPos = sawBenchPos;
            targetYaw = sawYaw;
            targetPitch = sawPitch;
        } else if (taskType == TaskType.PLANK_TO_BOX && craftBenchPos != null) {
            targetBlockPos = craftBenchPos;
            targetYaw = craftYaw;
            targetPitch = craftPitch;
        } else if (taskType == TaskType.AUTO_CHAIN) {
            int plankCount = countMaterialInInventory(client, "PLANK");
            if (plankCount >= 576 && craftBenchPos != null) {
                targetBlockPos = craftBenchPos;
                targetYaw = craftYaw;
                targetPitch = craftPitch;
            } else if (sawBenchPos != null) {
                targetBlockPos = sawBenchPos;
                targetYaw = sawYaw;
                targetPitch = sawPitch;
            }
        }

        if (targetBlockPos == null) {
            client.player.displayClientMessage(Component.literal("§c[SylvaraBot] Waypoint Bench belum diset!"), false);
            isStaticProcessing = false;
            currentState = State.IDLE;
            return;
        }

        clicksDone = 0;
        retryCount = 0;
        delayTicks = 0;
        guiTimeoutTicks = 0;
        afkTicksRemaining = 0;

        double distanceSq = client.player.blockPosition().distSqr(targetBlockPos);
        if (distanceSq > 2.25) {
            client.player.displayClientMessage(Component.literal("§a[SylvaraBot] Berjalan menuju Bench..."), false);
            List<BlockPos> path = AStarPathfinder.findPath(client.level, client.player.blockPosition(), targetBlockPos);
            if (path != null && !path.isEmpty()) {
                MovementController.startPath(path, targetBlockPos);
            } else {
                currentState = State.ALIGNING_CAMERA;
            }
        } else {
            currentState = State.ALIGNING_CAMERA;
        }
    }

    public static void onBenchReached() {
        if (isStaticProcessing) {
            currentState = State.ALIGNING_CAMERA;
        }
    }

    public static void stopTask() {
        currentState = State.IDLE;
        isStaticProcessing = false;
        targetBlockPos = null;
        clicksDone = 0;
        retryCount = 0;
        delayTicks = 0;
        guiTimeoutTicks = 0;
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.options != null) {
            client.options.keyUse.setDown(false);
        }
    }

    private static void skipWaypoint(String reason) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && client.screen != null) client.player.closeContainer();
        boolean wasStatic = isStaticProcessing;
        stopTask();
        if (!wasStatic) MovementController.onTaskFinished();
    }

    private static void disconnectServer(net.minecraft.client.Minecraft client, String reason) {
        MovementController.stop();
        stopTask();
        if (client.getConnection() != null) {
            client.getConnection().getConnection().disconnect(Component.literal("Connection lost"));
        }
    }

    private static void stopPlayerMovement(net.minecraft.client.Minecraft client) {
        if (client.options != null) {
            client.options.keyUse.setDown(false);
            client.options.keyUp.setDown(false);
            client.options.keyDown.setDown(false);
            client.options.keyLeft.setDown(false);
            client.options.keyRight.setDown(false);
            client.options.keyJump.setDown(false);
            client.options.keySprint.setDown(false);
            if (client.player != null) client.player.setSprinting(false);
        }
    }

    private static void saveCurrentBotState(net.minecraft.client.Minecraft client) {
        wasWalkingBeforeConsuming = MovementController.isActive();
        wasStaticBeforeConsuming = (isStaticProcessing && currentState != State.IDLE);
        wasTaskingBeforeConsuming = (!MovementController.isActive() && !isStaticProcessing && currentState != State.IDLE && targetBlockPos != null);

        if (wasWalkingBeforeConsuming) MovementController.pause();
        stopPlayerMovement(client);
    }

    private static void startDrinking(net.minecraft.client.Minecraft client) {
        if (currentState == State.DRINKING) return;
        boolean hasDrink = selectOrSwapItem(client, AutoLumberjackController::isDrinkItem, 8);
        if (!hasDrink) {
            openRanselCommand(client);
            currentState = State.OPENING_RANSEL_DRINK_WITHDRAW;
            ranselTimeoutTicks = 60;
            return;
        }
        ItemStack drinkStack = client.player.getInventory().getItem(8);
        if (client.player != null && client.screen != null) client.player.closeContainer();
        saveCurrentBotState(client);
        currentState = State.DRINKING;
        initialItemCount = drinkStack.getCount();
        initialItemType = drinkStack.getItem();
        consumptionTicks = 0;
        delayTicks = 5;
    }

    private static void startEating(net.minecraft.client.Minecraft client) {
        if (currentState == State.EATING || currentState == State.DRINKING) return;
        boolean hasFood = selectOrSwapItem(client, AutoLumberjackController::isFoodItem, 7);
        if (!hasFood) {
            openRanselCommand(client);
            currentState = State.OPENING_RANSEL_FOOD_WITHDRAW;
            ranselTimeoutTicks = 60;
            return;
        }
        ItemStack foodStack = client.player.getInventory().getItem(7);
        if (client.player != null && client.screen != null) client.player.closeContainer();
        saveCurrentBotState(client);
        currentState = State.EATING;
        initialItemCount = foodStack.getCount();
        initialItemType = foodStack.getItem();
        consumptionTicks = 0;
        delayTicks = 5;
    }

    private static void openRanselCommand(net.minecraft.client.Minecraft client) {
        if (client.player != null && client.player.connection != null) {
            client.player.connection.sendCommand("ransel");
        }
    }

    private static void resumeBotOperationAfterRansel() {
        currentState = State.IDLE;
        delayTicks = 15;
        if (currentTaskType == TaskType.AUTO_CHAIN) {
            startAutoChainSmart();
        } else if (wasStaticBeforeConsuming) {
            wasStaticBeforeConsuming = false;
            wasWalkingBeforeConsuming = false;
            wasTaskingBeforeConsuming = false;
            startStaticProcessing(currentTaskType);
        } else if (wasWalkingBeforeConsuming) {
            wasWalkingBeforeConsuming = false;
            wasTaskingBeforeConsuming = false;
            wasStaticBeforeConsuming = false;
            MovementController.startRoute(true, false);
        } else if (wasTaskingBeforeConsuming) {
            wasTaskingBeforeConsuming = false;
            wasWalkingBeforeConsuming = false;
            wasStaticBeforeConsuming = false;
            startTask(targetBlockPos, targetYaw, targetPitch, currentTaskType);
        } else if (isStaticProcessing) {
            startStaticProcessing(currentTaskType);
        }
    }

    private static void finishTask() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && client.screen != null) client.player.closeContainer();

        TaskType taskTypeBackup = currentTaskType;
        stopTask();

        int plankCount = countMaterialInInventory(client, "PLANK");
        int logCount = countMaterialInInventory(client, "LOG");

        if (taskTypeBackup == TaskType.NEBANG) {
            // Jika tugasnya adalah nebang pohon, lanjutkan ke waypoint rute berikutnya!
            currentState = State.IDLE;
            MovementController.onTaskFinished();
        } else if (taskTypeBackup == TaskType.LOG_TO_PLANK) {
            if (plankCount >= 576 && craftBenchPos != null) {
                client.player.displayClientMessage(Component.literal("§6[SylvaraBot] Planks sudah mencapai 9 stack! Beralih ke Craft Bench..."), false);
                startStaticProcessing(TaskType.PLANK_TO_BOX);
            } else if (logCount > 0 && sawBenchPos != null) {
                startStaticProcessing(TaskType.LOG_TO_PLANK);
            } else {
                saveCurrentBotState(client);
                openRanselCommand(client);
                currentState = State.OPENING_RANSEL_WITHDRAW;
                currentTaskType = TaskType.LOG_TO_PLANK;
                ranselTimeoutTicks = 60;
            }
        } else if (taskTypeBackup == TaskType.PLANK_TO_BOX) {
            if (plankCount >= 32 && craftBenchPos != null) {
                startStaticProcessing(TaskType.PLANK_TO_BOX);
            } else {
                if (logCount > 0 && sawBenchPos != null) {
                    client.player.displayClientMessage(Component.literal("§a[SylvaraBot] Planks habis dikerjakan. Kembali ke Saw Bench untuk potong log baru..."), false);
                    startStaticProcessing(TaskType.LOG_TO_PLANK);
                } else {
                    saveCurrentBotState(client);
                    openRanselCommand(client);
                    currentState = State.OPENING_RANSEL_WITHDRAW;
                    currentTaskType = TaskType.AUTO_CHAIN;
                    ranselTimeoutTicks = 60;
                }
            }
        } else if (taskTypeBackup == TaskType.AUTO_CHAIN) {
            startAutoChainSmart();
        } else {
            isStaticProcessing = false;
            currentState = State.IDLE;
        }
    }

    private static float smoothAngle(float current, float target, float speedFactor) {
        float diff = Mth.wrapDegrees(target - current);
        if (Math.abs(diff) < 0.2F) return target;
        float ease = (float) Math.sin((Math.abs(diff) / 180.0F) * Math.PI) * 0.8F + 0.2F;
        return current + (diff * speedFactor * ease);
    }

    public static void registerEvents() {
        loadBenches();

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> handleServerChat(message.getString()));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleServerChat(message.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gameMode == null) return;

            if (currentState != State.IDLE) {
                String actionText = "§b[SylvaraBot] §fStatus: ";
                switch (currentState) {
                    case ALIGNING_CAMERA: actionText += "§eMenyesuaikan Kamera Bench..."; break;
                    case RIGHT_CLICKING: actionText += "§eMengakses Bench..."; break;
                    case SOLVING_NEBANG_GUI:
                    case SOLVING_MINIGAME_GUI: actionText += "§aMenyelesaikan Minigame/GUI..."; break;
                    case WAITING_PROGRESS: actionText += "§6Menunggu Progress Selesai..."; break;
                    case DRINKING: actionText += "§bSedang Minum..."; break;
                    case EATING: actionText += "§6Sedang Makan..."; break;
                    case OPENING_RANSEL_WITHDRAW:
                    case WITHDRAWING_MATERIAL_FROM_RANSEL: actionText += "§eMengambil Material dari Ransel..."; break;
                    case AFK_TIMER: actionText += "§cAFK Selesai (Menunggu Timer)"; break;
                    default:
                        if (isStaticProcessing) {
                            actionText += (currentTaskType == TaskType.PLANK_TO_BOX ? "§6Crafting Wood Box" : "§bMemotong Log (Saw)");
                        } else {
                            actionText += "§aAktif";
                        }
                        break;
                }
                client.player.displayClientMessage(Component.literal(actionText), true);
            }

            if ((isFarmingActive() || isStaticProcessing) && currentState != State.IDLE) {
                if (client.player.getHealth() <= 10.0F) {
                    disconnectServer(client, "Darah di bawah 5 Bar! Safety Disconnect.");
                    return;
                }
            }

            if (currentState == State.AFK_TIMER) {
                afkTicksRemaining--;
                if (afkTicksRemaining <= 0) disconnectServer(client, "AFK 30 Menit!");
                return;
            }

            if (currentState == State.OPENING_RANSEL_DRINK_WITHDRAW) {
                ranselTimeoutTicks--;
                if (client.screen instanceof AbstractContainerScreen<?>) {
                    currentState = State.WITHDRAWING_DRINK_FROM_RANSEL;
                    delayTicks = 5;
                } else if (ranselTimeoutTicks <= 0) disconnectServer(client, "Minuman Habis!");
                return;
            }

            if (currentState == State.WITHDRAWING_DRINK_FROM_RANSEL) {
                if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
                    int containerSlots = containerScreen.getMenu().slots.size() - 36;
                    boolean movedAny = false;
                    for (int i = 0; i < containerSlots; i++) {
                        ItemStack stack = containerScreen.getMenu().slots.get(i).getItem();
                        if (isDrinkItem(stack)) {
                            client.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, i, 0, ClickType.QUICK_MOVE, client.player);
                            movedAny = true;
                            break;
                        }
                    }
                    client.player.closeContainer();
                    if (movedAny) { currentState = State.IDLE; delayTicks = 10; startDrinking(client); }
                    else disconnectServer(client, "Minuman Habis!");
                }
                return;
            }

            if (currentState == State.OPENING_RANSEL_FOOD_WITHDRAW) {
                ranselTimeoutTicks--;
                if (client.screen instanceof AbstractContainerScreen<?>) {
                    currentState = State.WITHDRAWING_FOOD_FROM_RANSEL;
                    delayTicks = 5;
                } else if (ranselTimeoutTicks <= 0) disconnectServer(client, "Makanan Habis!");
                return;
            }

            if (currentState == State.WITHDRAWING_FOOD_FROM_RANSEL) {
                if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
                    int containerSlots = containerScreen.getMenu().slots.size() - 36;
                    boolean movedAny = false;
                    for (int i = 0; i < containerSlots; i++) {
                        ItemStack stack = containerScreen.getMenu().slots.get(i).getItem();
                        if (isFoodItem(stack)) {
                            client.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, i, 0, ClickType.QUICK_MOVE, client.player);
                            movedAny = true;
                            break;
                        }
                    }
                    client.player.closeContainer();
                    if (movedAny) { currentState = State.IDLE; delayTicks = 10; startEating(client); }
                    else disconnectServer(client, "Makanan Habis!");
                }
                return;
            }

            if (currentState == State.OPENING_RANSEL_WITHDRAW) {
                ranselTimeoutTicks--;
                if (client.screen instanceof AbstractContainerScreen<?>) {
                    int initialEmptySlots = getEmptyInventorySlots(client);
                    targetWithdrawCount = Math.max(1, initialEmptySlots - 9);
                    withdrawnSoFarCount = 0;
                    currentState = State.WITHDRAWING_MATERIAL_FROM_RANSEL;
                    delayTicks = 5;
                } else if (ranselTimeoutTicks <= 0) handleMaterialDepleted(client);
                return;
            }

            if (currentState == State.WITHDRAWING_MATERIAL_FROM_RANSEL) {
                if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
                    if (withdrawnSoFarCount >= targetWithdrawCount) {
                        client.player.closeContainer();
                        resumeBotOperationAfterRansel();
                        return;
                    }
                    int containerSlots = containerScreen.getMenu().slots.size() - 36;
                    boolean movedAny = false;

                    boolean lookingForLog = (currentTaskType == TaskType.LOG_TO_PLANK || currentTaskType == TaskType.AUTO_CHAIN) && countMaterialInInventory(client, "PLANK") < 576;
                    boolean lookingForPlank = (currentTaskType == TaskType.PLANK_TO_BOX);

                    for (int i = 0; i < containerSlots; i++) {
                        ItemStack stack = containerScreen.getMenu().slots.get(i).getItem();
                        if ((lookingForLog && isLogItem(stack)) || (lookingForPlank && isPlankItem(stack))) {
                            client.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, i, 0, ClickType.QUICK_MOVE, client.player);
                            movedAny = true;
                            withdrawnSoFarCount++;
                            break;
                        }
                    }
                    if (!movedAny) {
                        client.player.closeContainer();
                        resumeBotOperationAfterRansel();
                    } else delayTicks = 3;
                }
                return;
            }

            if (currentState == State.DRINKING || currentState == State.EATING) {
                stopPlayerMovement(client);
                consumptionTicks++;
                client.options.keyUse.setDown(true);
                int hotbarSlot = (currentState == State.DRINKING) ? 8 : 7;
                ItemStack currentConsumeItem = client.player.getInventory().getItem(hotbarSlot);
                boolean isItemConsumed = currentConsumeItem.isEmpty() || currentConsumeItem.getCount() < initialItemCount || currentConsumeItem.getItem() != initialItemType;

                if (isItemConsumed || consumptionTicks > 200) {
                    client.options.keyUse.setDown(false);
                    selectOrSwapItem(client, currentState == State.DRINKING ? AutoLumberjackController::isDrinkItem : AutoLumberjackController::isFoodItem, hotbarSlot);
                    currentState = State.IDLE;
                    delayTicks = 15;
                    if (!pausedByAdmin) {
                        resumeBotOperationAfterRansel();
                    }
                }
                return;
            }

            if (autoEatEnabled && (isBotActive() || MovementController.isActive()) && !pausedByAdmin && client.player.getFoodData().getFoodLevel() <= 6 && currentState != State.EATING && currentState != State.DRINKING) {
                startEating(client);
                return;
            }

            if (pausedByAdmin) {
                if (client.options != null) {
                    client.options.keyUse.setDown(false);
                    client.options.keyUp.setDown(false);
                }
                return;
            }

            if (currentState == State.IDLE) return;
            if (currentState != State.RIGHT_CLICKING && client.options != null) client.options.keyUse.setDown(false);

            if (delayTicks > 0) {
                delayTicks--;
                return;
            }

            switch (currentState) {
                case ALIGNING_CAMERA:
                    if (currentTaskType == TaskType.NEBANG) {
                        float nextYaw = smoothAngle(client.player.getYRot(), targetYaw, 0.60F);
                        float nextPitch = smoothAngle(client.player.getXRot(), targetPitch, 0.60F);
                        client.player.setYRot(nextYaw);
                        client.player.setXRot(nextPitch);

                        if (Math.abs(Mth.wrapDegrees(targetYaw - nextYaw)) < 1.0F && Math.abs(targetPitch - nextPitch) < 1.0F) {
                            client.player.setYRot(targetYaw);
                            client.player.setXRot(targetPitch);
                            currentState = State.RIGHT_CLICKING;
                            clicksDone = 0;
                            delayTicks = 4;
                        }
                        break;
                    }

                    boolean isCraftingPhase = (currentTaskType == TaskType.PLANK_TO_BOX) ||
                            (currentTaskType == TaskType.AUTO_CHAIN && countMaterialInInventory(client, "PLANK") >= 576);

                    if (isCraftingPhase) {
                        if (countMaterialInInventory(client, "PLANK") < 32) {
                            saveCurrentBotState(client);
                            openRanselCommand(client);
                            currentState = State.OPENING_RANSEL_WITHDRAW;
                            ranselTimeoutTicks = 60;
                            return;
                        }
                        if (!selectOrSwapItem(client, AutoLumberjackController::isPlankItem, 3)) {
                            skipWaypoint("Planks kosong!");
                            return;
                        }
                    } else {
                        if (countMaterialInInventory(client, "LOG") < 18) {
                            saveCurrentBotState(client);
                            openRanselCommand(client);
                            currentState = State.OPENING_RANSEL_WITHDRAW;
                            ranselTimeoutTicks = 60;
                            return;
                        }
                        selectOrSwapItem(client, stack -> isLogItem(stack) || true, 1);
                    }

                    float nextYaw = smoothAngle(client.player.getYRot(), targetYaw, 0.60F);
                    float nextPitch = smoothAngle(client.player.getXRot(), targetPitch, 0.60F);
                    client.player.setYRot(nextYaw);
                    client.player.setXRot(nextPitch);

                    if (Math.abs(Mth.wrapDegrees(targetYaw - nextYaw)) < 1.0F && Math.abs(targetPitch - nextPitch) < 1.0F) {
                        client.player.setYRot(targetYaw);
                        client.player.setXRot(targetPitch);
                        currentState = State.RIGHT_CLICKING;
                        clicksDone = 0;
                        delayTicks = isCraftingPhase ? 8 : 4;
                    }
                    break;

                case RIGHT_CLICKING:
                    if (clicksDone == 0) {
                        client.player.setYRot(targetYaw);
                        client.player.setXRot(targetPitch);
                        if (targetBlockPos != null) {
                            BlockHitResult blockHitResult = new BlockHitResult(Vec3.atCenterOf(targetBlockPos), Direction.UP, targetBlockPos, false);
                            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHitResult);
                        }
                    }
                    if (client.options != null) client.options.keyUse.setDown(true);
                    clicksDone++;
                    if (clicksDone >= 5) {
                        if (client.options != null) client.options.keyUse.setDown(false);
                        currentState = State.SOLVING_NEBANG_GUI;
                        clicksDone = 0;
                        guiTimeoutTicks = 240;
                        delayTicks = 10 + random.nextInt(4);
                    }
                    break;

                case SOLVING_NEBANG_GUI:
                    if (client.options != null) client.options.keyUse.setDown(false);
                    guiTimeoutTicks -= 5;
                    if (guiTimeoutTicks <= 0) { handleGuiTimeout(client); break; }

                    if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
                        boolean foundStartItem = false;
                        for (Slot slot : containerScreen.getMenu().slots) {
                            String itemId = BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).getPath();
                            if (itemId.contains("lime_dye") || itemId.contains("lime_stained_glass_pane")) {
                                client.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
                                currentState = State.SOLVING_MINIGAME_GUI;
                                clicksDone = itemId.contains("lime_dye") ? 0 : 1;
                                guiTimeoutTicks = 240;
                                delayTicks = 10 + random.nextInt(4);
                                foundStartItem = true;
                                break;
                            }
                        }
                        if (!foundStartItem) delayTicks = 3;
                    } else delayTicks = 3;
                    break;

                case SOLVING_MINIGAME_GUI:
                    if (clicksDone >= 3) {
                        currentState = State.WAITING_PROGRESS;
                        guiTimeoutTicks = 360;
                        delayTicks = 5;
                        break;
                    }
                    guiTimeoutTicks -= 5;
                    if (guiTimeoutTicks <= 0) { handleGuiTimeout(client); break; }

                    if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
                        boolean clickedInThisTick = false;
                        for (Slot slot : containerScreen.getMenu().slots) {
                            if (BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).getPath().contains("lime_stained_glass_pane")) {
                                client.gameMode.handleInventoryMouseClick(containerScreen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
                                clicksDone++;
                                guiTimeoutTicks = 240;
                                delayTicks = 10 + random.nextInt(4);
                                clickedInThisTick = true;
                                break;
                            }
                        }
                        if (!clickedInThisTick) delayTicks = 3;
                    } else {
                        if (clicksDone > 0) { currentState = State.WAITING_PROGRESS; guiTimeoutTicks = 360; }
                        else delayTicks = 3;
                    }
                    break;

                case WAITING_PROGRESS:
                    if (guiTimeoutTicks > 0) guiTimeoutTicks--;
                    else finishTask();
                    break;

                default:
                    break;
            }
        });
    }

    private static void handleMaterialDepleted(net.minecraft.client.Minecraft client) {
        isStaticProcessing = false;
        stopTask();
        currentState = State.AFK_TIMER;
        afkTicksRemaining = THIRTY_MINUTES_TICKS;
    }

    private static void handleServerChat(String chatText) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        String cleanChat = chatText.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
        if (cleanChat.contains("[SylvaraBot]")) return;

        String lowerChat = cleanChat.toLowerCase();
        if (lowerChat.contains("tunggu beberapa detik") || lowerChat.contains("agar kamu bisa menebang")) {
            if (isBotActive() || MovementController.isActive()) {
                client.player.displayClientMessage(Component.literal("§e[SylvaraBot] Pohon Cooldown! Melewati waypoint ini..."), false);
                skipWaypoint("Pohon Cooldown");
            }
            return;
        }

        if (lowerChat.contains("haus") || lowerChat.contains("air:")) {
            if (autoDrinkEnabled && (isBotActive() || MovementController.isActive()) && !pausedByAdmin) startDrinking(client);
            return;
        }

        if (cleanChat.contains("is now on duty!")) {
            pausedByAdmin = true;
            MovementController.stop();
            stopTask();
            return;
        }

        if (cleanChat.contains("is now off duty!") || cleanChat.contains("is now offduty!")) {
            pausedByAdmin = false;
            if (isStaticProcessing) startStaticProcessing(currentTaskType);
            else MovementController.startRoute(true, true);
            return;
        }

        if (currentState == State.WAITING_PROGRESS && (cleanChat.contains("Selesai") || cleanChat.contains("Pemotongan") || cleanChat.contains("Crafting"))) {
            finishTask();
        }
    }

    private static void handleGuiTimeout(net.minecraft.client.Minecraft client) {
        if (client.player != null && client.screen != null) client.player.closeContainer();
        retryCount++;
        if (retryCount >= 3) skipWaypoint("GUI Timeout");
        else { currentState = State.ALIGNING_CAMERA; delayTicks = 15; }
    }
}