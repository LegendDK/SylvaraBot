package velzee.sylvarabot.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovementController {

    private static final List<Waypoint> waypoints = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("sylvarabot_waypoints.json")
            .toFile();

    private static int currentWaypointIndex = 0;
    private static List<BlockPos> currentPath = null;
    private static int targetIndex = 0;
    private static boolean active = false;
    private static boolean isLooping = false;
    private static BlockPos finalTarget = null;
    private static int stuckTicks = 0;
    private static int cooldownTicks = 0;
    private static int humanPauseTicks = 0;
    private static Vec3 lastPos = Vec3.ZERO;

    public static void saveWaypoints() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(waypoints, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadWaypoints() {
        if (!CONFIG_FILE.exists()) return;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Type listType = new TypeToken<ArrayList<Waypoint>>() {}.getType();
            List<Waypoint> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                waypoints.clear();
                waypoints.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addWaypoint(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        float yaw = client.player != null ? client.player.getYRot() : 0.0f;
        float pitch = client.player != null ? client.player.getXRot() : 0.0f;
        waypoints.add(new Waypoint("WP " + (waypoints.size() + 1), pos, yaw, pitch));
        saveWaypoints();
    }

    public static void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            saveWaypoints();
        }
    }

    public static void moveUp(int index) {
        if (index > 0 && index < waypoints.size()) {
            Collections.swap(waypoints, index, index - 1);
            saveWaypoints();
        }
    }

    public static void moveDown(int index) {
        if (index >= 0 && index < waypoints.size() - 1) {
            Collections.swap(waypoints, index, index + 1);
            saveWaypoints();
        }
    }

    public static void renameWaypoint(int index, String newName) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.get(index).setName(newName);
            saveWaypoints();
        }
    }

    public static List<Waypoint> getWaypoints() { return waypoints; }

    public static void clearWaypoints() {
        waypoints.clear();
        currentWaypointIndex = 0;
        saveWaypoints();
    }

    public static int findNearestWaypointIndex() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || waypoints.isEmpty()) return 0;
        BlockPos playerPos = client.player.blockPosition();
        int nearestIdx = 0;
        double minDst = Double.MAX_VALUE;
        for (int i = 0; i < waypoints.size(); i++) {
            double dist = waypoints.get(i).getPos().distSqr(playerPos);
            if (dist < minDst) {
                minDst = dist;
                nearestIdx = i;
            }
        }
        return nearestIdx;
    }

    public static void startRoute(boolean loop) { startRoute(loop, true); }

    public static void startRoute(boolean loop, boolean startFromNearest) {
        if (waypoints.isEmpty()) return;
        isLooping = loop;
        currentWaypointIndex = startFromNearest ? findNearestWaypointIndex() : Math.min(currentWaypointIndex, waypoints.size() - 1);
        navigateToNextWaypoint();
    }

    public static void pause() {
        active = false;
        Minecraft client = Minecraft.getInstance();
        if (client.options != null && client.player != null) {
            client.options.keyUp.setDown(false);
            client.options.keyJump.setDown(false);
            client.options.keySprint.setDown(false);
            client.player.setSprinting(false);
        }
    }

    private static void navigateToNextWaypoint() {
        if (currentWaypointIndex >= waypoints.size()) {
            if (isLooping) currentWaypointIndex = 0;
            else { stop(); return; }
        }
        if (waypoints.isEmpty()) { stop(); return; }

        BlockPos target = waypoints.get(currentWaypointIndex).getPos();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        List<BlockPos> path = AStarPathfinder.findPath(client.level, client.player.blockPosition(), target);
        if (path != null && !path.isEmpty()) {
            startPath(path, target);
        } else {
            List<BlockPos> fallback = new ArrayList<>();
            fallback.add(client.player.blockPosition());
            fallback.add(target);
            startPath(fallback, target);
        }
    }

    public static void startPath(List<BlockPos> path, BlockPos target) {
        currentPath = path;
        finalTarget = target;
        targetIndex = 0;
        active = true;
        stuckTicks = 0;
        cooldownTicks = 10;
        humanPauseTicks = RandomSource.create().nextIntBetweenInclusive(1, 3);
    }

    public static void stop() { reset(); }

    public static void reset() {
        active = false;
        currentPath = null;
        finalTarget = null;
        stuckTicks = 0;
        cooldownTicks = 0;
        humanPauseTicks = 0;
        targetIndex = 0;
        lastPos = Vec3.ZERO;
        Minecraft client = Minecraft.getInstance();
        if (client.options != null && client.player != null) {
            client.options.keyUp.setDown(false);
            client.options.keyJump.setDown(false);
            client.options.keySprint.setDown(false);
            client.player.setSprinting(false);
        }
    }

    public static boolean isActive() { return active; }

    public static void registerEvents() {
        loadWaypoints();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!active || currentPath == null || client.player == null || client.level == null) return;

            if (humanPauseTicks > 0) {
                humanPauseTicks--;
                client.options.keyUp.setDown(false);
                return;
            }

            Vec3 playerPos = client.player.position();

            // 1. Cek apakah sudah sampai di target akhir waypoint
            if (finalTarget != null) {
                Vec3 finalTargetVec = new Vec3(finalTarget.getX() + 0.5, finalTarget.getY(), finalTarget.getZ() + 0.5);
                // Radius toleransi diperbesar sedikit ke 2.25 agar tidak muter-muter pas di titik akhir
                if (playerPos.distanceToSqr(finalTargetVec) <= 2.25 || targetIndex >= currentPath.size()) {
                    active = false;
                    client.options.keyUp.setDown(false);
                    client.options.keyJump.setDown(false);
                    client.options.keySprint.setDown(false);
                    client.player.setSprinting(false);

                    AutoLumberjackController.selectAxeSlot(client);

                    if (AutoLumberjackController.isStaticProcessingActive()) {
                        AutoLumberjackController.onBenchReached();
                    } else if (currentWaypointIndex >= 0 && currentWaypointIndex < waypoints.size()) {
                        Waypoint currentWp = waypoints.get(currentWaypointIndex);
                        AutoLumberjackController.startTask(finalTarget, currentWp.getYaw(), currentWp.getPitch(), currentWp.getTaskType());
                    }
                    return;
                }
            }

            if (cooldownTicks > 0) cooldownTicks--;

            // 2. Sistem Node Skipping / Lookahead:
            // Jika bot sudah mendekati atau melewati node saat ini, otomatis lanjut ke node berikutnya di path
            while (targetIndex < currentPath.size() - 1) {
                BlockPos nodePos = currentPath.get(targetIndex);
                Vec3 nodeVec = new Vec3(nodePos.getX() + 0.5, nodePos.getY(), nodePos.getZ() + 0.5);
                // Jika jarak ke node kurang dari 2 blok, atau node berada di belakang player, skip ke node depan
                if (playerPos.distanceToSqr(nodeVec.x, playerPos.y, nodeVec.z) < 3.0) {
                    targetIndex++;
                    continue;
                }
                break;
            }

            if (playerPos.distanceToSqr(lastPos) < 0.0009) stuckTicks++;
            else stuckTicks = 0;
            lastPos = playerPos;

            if (cooldownTicks == 0 && stuckTicks > 25 && finalTarget != null) {
                stuckTicks = 0;
                navigateToNextWaypoint();
                return;
            }

            // Ambil target node aktif saat ini
            int lookIndex = Math.min(targetIndex, currentPath.size() - 1);
            BlockPos targetNode = currentPath.get(lookIndex);
            Vec3 targetVec = new Vec3(targetNode.getX() + 0.5, targetNode.getY(), targetNode.getZ() + 0.5);

            double dx = targetVec.x - playerPos.x;
            double dz = targetVec.z - playerPos.z;

            float wantedYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float currentYaw = client.player.getYRot();
            float currentPitch = client.player.getXRot();

            // Cegah muter-muter: Jika sudah sangat dekat dengan node tujuan (kurang dari 1.5 blok),
            // kunci yaw agar tidak menghitung ulang sudut balik yang bikin pusing
            double distToNode = playerPos.distanceToSqr(targetVec.x, playerPos.y, targetVec.z);
            float smoothedYaw;
            if (distToNode < 2.25) {
                smoothedYaw = currentYaw; // Pertahankan arah hadap terakhir yang mulus
            } else {
                smoothedYaw = currentYaw + (Mth.wrapDegrees(wantedYaw - currentYaw) * 0.35F);
            }

            float smoothedPitch = Mth.rotLerp(0.25F, currentPitch, 0.0F);

            // Deteksi air untuk navigasi natural
            BlockPos frontPos = client.player.blockPosition().relative(client.player.getDirection());
            boolean isWaterAhead = client.level.getBlockState(frontPos).getBlock() == net.minecraft.world.level.block.Blocks.WATER ||
                    client.level.getBlockState(frontPos.below()).getBlock() == net.minecraft.world.level.block.Blocks.WATER;

            if (client.player.isInWater() || client.player.isInLava()) {
                // Berenang keluar air dengan mulus
                client.options.keyJump.setDown(true);
                client.options.keyUp.setDown(true);
                client.options.keySprint.setDown(true);
                client.player.setSprinting(true);
                client.player.setYRot(smoothedYaw);
                client.player.setXRot(Mth.rotLerp(0.25F, currentPitch, -20.0F));
            } else if (isWaterAhead) {
                // Lompat lewati air secara natural
                client.player.setYRot(smoothedYaw);
                client.player.setXRot(smoothedPitch);
                client.options.keyUp.setDown(true);
                client.options.keySprint.setDown(true);
                client.player.setSprinting(true);
                client.options.keyJump.setDown(true);
            } else {
                // Pergerakan normal di daratan
                client.player.setYRot(smoothedYaw);
                client.player.setXRot(smoothedPitch);
                client.options.keyUp.setDown(true);
                client.options.keySprint.setDown(true);
                client.player.setSprinting(true);

                boolean isNearTarget = false;
                if (finalTarget != null) {
                    Vec3 finalTargetVec = new Vec3(finalTarget.getX() + 0.5, finalTarget.getY(), finalTarget.getZ() + 0.5);
                    if (playerPos.distanceToSqr(finalTargetVec) <= 9.0) {
                        isNearTarget = true;
                    }
                }

                if (client.player.onGround() && !isNearTarget) {
                    client.options.keyJump.setDown(true);
                } else {
                    client.options.keyJump.setDown(false);
                }
            }
        });
    }

    public static void onTaskFinished() {
        currentWaypointIndex++;
        navigateToNextWaypoint();
    }
}