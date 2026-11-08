package velzee.sylvarabot.client;

import net.minecraft.core.BlockPos;

public class Waypoint {
    private String name;
    private int x;
    private int y;
    private int z;
    private float yaw;
    private float pitch;
    private TaskType taskType;

    public Waypoint(String name, BlockPos pos, float yaw, float pitch) {
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.yaw = yaw;
        this.pitch = pitch;
        this.taskType = TaskType.NEBANG;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BlockPos getPos() { return new BlockPos(x, y, z); }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public TaskType getTaskType() {
        return taskType == null ? TaskType.NEBANG : taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
}