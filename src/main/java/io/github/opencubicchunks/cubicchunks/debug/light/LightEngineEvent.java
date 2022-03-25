package io.github.opencubicchunks.cubicchunks.debug.light;

public abstract class LightEngineEvent {
    public static int BASE_SIZE = 1 + 3 * 4;

    protected final EventType type;
    protected final int x, y, z;

    public LightEngineEvent(EventType type, int x, int y, int z) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public EventType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
