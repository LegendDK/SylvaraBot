//package velzee.sylvarabot.client;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.tags.BlockTags;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Blocks;
//import net.minecraft.world.level.block.FenceBlock;
//import net.minecraft.world.level.block.IronBarsBlock;
//import net.minecraft.world.level.block.WallBlock;
//import net.minecraft.world.level.block.state.BlockState;
//
//import java.util.*;
//
//public class AStarPathfinder {
//
//    public static List<BlockPos> findPath(Level world, BlockPos start, BlockPos target) {
//        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
//        Set<BlockPos> closedSet = new HashSet<>();
//        Map<BlockPos, PathNode> allNodes = new HashMap<>();
//
//        PathNode startNode = new PathNode(start);
//        startNode.gCost = 0;
//        startNode.hCost = calculateHeuristic(start, target);
//
//        openSet.add(startNode);
//        allNodes.put(start, startNode);
//
//        int maxIterations = 8000;
//        int iterations = 0;
//
//        while (!openSet.isEmpty() && iterations < maxIterations) {
//            iterations++;
//            PathNode current = openSet.poll();
//
//            if (current.pos.equals(target) || current.pos.distManhattan(target) <= 1) {
//                List<BlockPos> rawPath = retracePath(current);
//                return smoothPath(world, rawPath);
//            }
//
//            closedSet.add(current.pos);
//
//            for (int dx = -1; dx <= 1; dx++) {
//                for (int dz = -1; dz <= 1; dz++) {
//                    if (dx == 0 && dz == 0) continue;
//
//                    for (int dy = -1; dy <= 1; dy++) {
//                        BlockPos neighborPos = current.pos.offset(dx, dy, dz);
//
//                        if (closedSet.contains(neighborPos)) continue;
//                        if (!isWalkable(world, neighborPos, dx, dz)) continue;
//
//                        double moveCost = (dx != 0 && dz != 0) ? 1.414 : 1.0;
//                        if (dy != 0) moveCost += 1.5;
//
//                        BlockState blockState = world.getBlockState(neighborPos);
//                        BlockState belowState = world.getBlockState(neighborPos.below());
//                        if (blockState.getBlock() == Blocks.WATER || belowState.getBlock() == Blocks.WATER) {
//                            moveCost += 5.0;
//                        }
//
//                        // Penalti tambahan jika terlalu dekat dengan tembok agar bot memilih jalur tengah
//                        if (isNearWall(world, neighborPos)) {
//                            moveCost += 2.5;
//                        }
//
//                        double newGCost = current.gCost + moveCost;
//                        PathNode neighbor = allNodes.getOrDefault(neighborPos, new PathNode(neighborPos));
//
//                        if (newGCost < neighbor.gCost || !openSet.contains(neighbor)) {
//                            neighbor.gCost = newGCost;
//                            neighbor.hCost = calculateHeuristic(neighborPos, target);
//                            neighbor.parent = current;
//                            allNodes.put(neighborPos, neighbor);
//
//                            if (openSet.contains(neighbor)) {
//                                openSet.remove(neighbor);
//                            }
//                            openSet.add(neighbor);
//                        }
//                    }
//                }
//            }
//        }
//        return null;
//    }
//
//    private static double calculateHeuristic(BlockPos a, BlockPos b) {
//        double dx = Math.abs(a.getX() - b.getX());
//        double dy = Math.abs(a.getY() - b.getY());
//        double dz = Math.abs(a.getZ() - b.getZ());
//        return (dx + dz) + (1.414 - 2) * Math.min(dx, dz) + dy * 1.5;
//    }
//
//    private static boolean isSolidCollision(Level world, BlockPos pos) {
//        BlockState state = world.getBlockState(pos);
//        if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FIRE) return true;
//
//        if (state.getBlock() instanceof FenceBlock
//                || state.getBlock() instanceof WallBlock
//                || state.getBlock() instanceof IronBarsBlock
//                || state.is(BlockTags.FENCES)
//                || state.is(BlockTags.WALLS)) {
//            return true;
//        }
//
//        return !state.getCollisionShape(world, pos).isEmpty();
//    }
//
//    // Mendeteksi apakah suatu posisi berada tepat di sebelah dinding/pagar/solid block
//    private static boolean isNearWall(Level world, BlockPos pos) {
//        for (int x = -1; x <= 1; x++) {
//            for (int z = -1; z <= 1; z++) {
//                if (x == 0 && z == 0) continue;
//                if (isSolidCollision(world, pos.offset(x, 0, z)) || isSolidCollision(world, pos.offset(x, 1, z))) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    private static boolean isWalkable(Level world, BlockPos pos, int dx, int dz) {
//        // Cek ruang badan bot (kaki dan kepala) serta blok di sekitarnya (padding lebar badan player ~0.6 blok)
//        if (isSolidCollision(world, pos) || isSolidCollision(world, pos.above())) {
//            return false;
//        }
//
//        // Pengecekan ekstra ketat pada sisi diagonal untuk mencegah badan menyangkut di sudut bangunan/pagar
//        if (dx != 0 && dz != 0) {
//            BlockPos side1 = pos.offset(dx, 0, 0);
//            BlockPos side2 = pos.offset(0, 0, dz);
//            if (isSolidCollision(world, side1) || isSolidCollision(world, side2) ||
//                    isSolidCollision(world, side1.above()) || isSolidCollision(world, side2.above())) {
//                return false;
//            }
//        }
//
//        BlockPos belowPos = pos.below();
//        BlockState belowState = world.getBlockState(belowPos);
//
//        if (isSolidCollision(world, belowPos) && (belowState.getBlock() instanceof FenceBlock || belowState.getBlock() instanceof WallBlock)) {
//            return false;
//        }
//
//        boolean hasGround = !belowState.getCollisionShape(world, belowPos).isEmpty() || belowState.getBlock() == Blocks.WATER;
//
//        if (!hasGround) {
//            boolean canFallSafely = false;
//            for (int drop = 1; drop <= 2; drop++) {
//                BlockPos checkDrop = pos.below(drop);
//                if (!world.getBlockState(checkDrop).getCollisionShape(world, checkDrop).isEmpty()) {
//                    canFallSafely = true;
//                    break;
//                }
//            }
//            if (!canFallSafely) return false;
//        }
//
//        return true;
//    }
//
//    private static List<BlockPos> retracePath(PathNode endNode) {
//        List<BlockPos> path = new ArrayList<>();
//        PathNode current = endNode;
//        while (current != null) {
//            path.add(current.pos);
//            current = current.parent;
//        }
//        Collections.reverse(path);
//        return path;
//    }
//
//    // Smooth path diperketat agar garis jalurnya tidak memotong terlalu dekat dengan sudut tembok/pagar
//    private static List<BlockPos> smoothPath(Level world, List<BlockPos> rawPath) {
//        if (rawPath == null || rawPath.size() <= 2) return rawPath;
//
//        List<BlockPos> smoothed = new ArrayList<>();
//        smoothed.add(rawPath.get(0));
//
//        int current = 0;
//        while (current < rawPath.size() - 1) {
//            int furthest = current + 1;
//            for (int next = current + 2; next < rawPath.size(); next++) {
//                if (hasClearPath(world, rawPath.get(current), rawPath.get(next))) {
//                    furthest = next;
//                } else {
//                    break;
//                }
//            }
//            smoothed.add(rawPath.get(furthest));
//            current = furthest;
//        }
//        return smoothed;
//    }
//
//    private static boolean hasClearPath(Level world, BlockPos start, BlockPos end) {
//        if (start.getY() != end.getY()) return false;
//
//        double x0 = start.getX() + 0.5;
//        double y0 = start.getY();
//        double z0 = start.getZ() + 0.5;
//
//        double x1 = end.getX() + 0.5;
//        double y1 = end.getY();
//        double z1 = end.getZ() + 0.5;
//
//        double distance = Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(z1 - z0, 2));
//        int steps = (int) Math.ceil(distance * 8);
//
//        for (int i = 0; i <= steps; i++) {
//            double t = (double) i / steps;
//            double currX = x0 + (x1 - x0) * t;
//            double currY = y0 + (y1 - y0) * t;
//            double currZ = z0 + (z1 - z0) * t;
//
//            BlockPos footPos = BlockPos.containing(currX, currY, currZ);
//            BlockPos headPos = footPos.above();
//
//            if (isSolidCollision(world, footPos) || isSolidCollision(world, headPos)) {
//                return false;
//            }
//
//            // Tambahan: Pastikan titik raycast smoothing tidak terlalu dekat dengan dinding samping
//            if (isNearWall(world, footPos)) {
//                return false;
//            }
//
//            BlockPos belowPos = footPos.below();
//            BlockState belowState = world.getBlockState(belowPos);
//            boolean hasGround = !belowState.getCollisionShape(world, belowPos).isEmpty() || belowState.getBlock() == Blocks.WATER;
//
//            if (!hasGround) return false;
//        }
//        return true;
//    }
//}
package velzee.sylvarabot.client;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class AStarPathfinder {

    public static List<BlockPos> findPath(Level world, BlockPos start, BlockPos target) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, PathNode> allNodes = new HashMap<>();

        PathNode startNode = new PathNode(start);
        startNode.gCost = 0;
        startNode.hCost = calculateHeuristic(start, target);

        openSet.add(startNode);
        allNodes.put(start, startNode);

        int maxIterations = 4000;
        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            PathNode current = openSet.poll();

            if (current.pos.equals(target) || current.pos.distManhattan(target) <= 1) {
                List<BlockPos> rawPath = retracePath(current);
                return smoothPath(world, rawPath);
            }

            closedSet.add(current.pos);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos neighborPos = current.pos.offset(dx, dy, dz);

                        if (closedSet.contains(neighborPos)) continue;
                        if (!isWalkable(world, neighborPos, dx, dz)) continue;

                        double moveCost = (dx != 0 && dz != 0) ? 1.414 : 1.0;
                        if (dy != 0) moveCost += 1.5;

                        BlockState blockState = world.getBlockState(neighborPos);
                        BlockState belowState = world.getBlockState(neighborPos.below());
                        if (blockState.getBlock() == Blocks.WATER || belowState.getBlock() == Blocks.WATER) {
                            moveCost += 5.0;
                        }

                        if (isNearWall(world, neighborPos)) {
                            moveCost += 2.5;
                        }

                        double newGCost = current.gCost + moveCost;
                        PathNode neighbor = allNodes.getOrDefault(neighborPos, new PathNode(neighborPos));

                        if (newGCost < neighbor.gCost || !openSet.contains(neighbor)) {
                            neighbor.gCost = newGCost;
                            neighbor.hCost = calculateHeuristic(neighborPos, target);
                            neighbor.parent = current;
                            allNodes.put(neighborPos, neighbor);

                            if (openSet.contains(neighbor)) {
                                openSet.remove(neighbor);
                            }
                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static double calculateHeuristic(BlockPos a, BlockPos b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        double dz = Math.abs(a.getZ() - b.getZ());
        return (dx + dz) + (1.414 - 2) * Math.min(dx, dz) + dy * 1.5;
    }

    private static boolean isSolidCollision(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FIRE) return true;

        if (state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof IronBarsBlock
                || state.is(BlockTags.FENCES)
                || state.is(BlockTags.WALLS)) {
            return true;
        }

        return !state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean isNearWall(Level world, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                if (isSolidCollision(world, pos.offset(x, 0, z)) || isSolidCollision(world, pos.offset(x, 1, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWalkable(Level world, BlockPos pos, int dx, int dz) {
        if (isSolidCollision(world, pos) || isSolidCollision(world, pos.above())) {
            return false;
        }

        if (dx != 0 && dz != 0) {
            BlockPos side1 = pos.offset(dx, 0, 0);
            BlockPos side2 = pos.offset(0, 0, dz);
            if (isSolidCollision(world, side1) || isSolidCollision(world, side2) ||
                    isSolidCollision(world, side1.above()) || isSolidCollision(world, side2.above())) {
                return false;
            }
        }

        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);

        if (isSolidCollision(world, belowPos) && (belowState.getBlock() instanceof FenceBlock || belowState.getBlock() instanceof WallBlock)) {
            return false;
        }

        boolean hasGround = !belowState.getCollisionShape(world, belowPos).isEmpty() || belowState.getBlock() == Blocks.WATER;

        if (!hasGround) {
            boolean canFallSafely = false;
            for (int drop = 1; drop <= 2; drop++) {
                BlockPos checkDrop = pos.below(drop);
                if (!world.getBlockState(checkDrop).getCollisionShape(world, checkDrop).isEmpty()) {
                    canFallSafely = true;
                    break;
                }
            }
            if (!canFallSafely) return false;
        }

        return true;
    }

    private static List<BlockPos> retracePath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = endNode;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static List<BlockPos> smoothPath(Level world, List<BlockPos> rawPath) {
        if (rawPath == null || rawPath.size() <= 2) return rawPath;

        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(rawPath.get(0));

        int current = 0;
        while (current < rawPath.size() - 1) {
            int furthest = current + 1;
            for (int next = current + 2; next < rawPath.size(); next++) {
                if (hasClearPath(world, rawPath.get(current), rawPath.get(next))) {
                    furthest = next;
                } else {
                    break;
                }
            }
            smoothed.add(rawPath.get(furthest));
            current = furthest;
        }
        return smoothed;
    }

    private static boolean hasClearPath(Level world, BlockPos start, BlockPos end) {
        if (start.getY() != end.getY()) return false;

        double x0 = start.getX() + 0.5;
        double y0 = start.getY();
        double z0 = start.getZ() + 0.5;

        double x1 = end.getX() + 0.5;
        double y1 = end.getY();
        double z1 = end.getZ() + 0.5;

        double distance = Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(z1 - z0, 2));
        int steps = (int) Math.ceil(distance * 8);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double currX = x0 + (x1 - x0) * t;
            double currY = y0 + (y1 - y0) * t;
            double currZ = z0 + (z1 - z0) * t;

            BlockPos footPos = BlockPos.containing(currX, currY, currZ);
            BlockPos headPos = footPos.above();

            if (isSolidCollision(world, footPos) || isSolidCollision(world, headPos)) {
                return false;
            }

            if (isNearWall(world, footPos)) {
                return false;
            }

            BlockPos belowPos = footPos.below();
            BlockState belowState = world.getBlockState(belowPos);
            boolean hasGround = !belowState.getCollisionShape(world, belowPos).isEmpty() || belowState.getBlock() == Blocks.WATER;

            if (!hasGround) return false;
        }
        return true;
    }
}