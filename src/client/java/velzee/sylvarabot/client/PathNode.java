package velzee.sylvarabot.client;

import net.minecraft.core.BlockPos;

public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public PathNode parent;
    public double gCost;
    public double hCost;

    public PathNode(BlockPos pos) {
        this.pos = pos;
    }

    public double getFCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.getFCost(), other.getFCost());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PathNode) {
            return this.pos.equals(((PathNode) obj).pos);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}