package io.github.opencubicchunks.cubicchunks.debug.light.events;

import java.io.DataInput;
import java.io.IOException;

import io.github.opencubicchunks.cubicchunks.debug.light.EventType;
import io.github.opencubicchunks.cubicchunks.debug.light.LightEngineEvent;

public class CheckEdge extends LightEngineEvent {
    public static final int SIZE = BASE_SIZE + 3 * 4 + 3 * 1 + 1;

    private final int toX, toY, toZ;
    private final byte updateLevel, currStoredLevel, currComputedLevel;
    private final boolean bl;

    public CheckEdge(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, byte updateLevel, byte currStoredLevel, byte currComputedLevel, boolean bl) {
        super(EventType.CHECK_EDGE, fromX, fromY, fromZ);

        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;

        this.updateLevel = updateLevel;
        this.currStoredLevel = currStoredLevel;
        this.currComputedLevel = currComputedLevel;

        this.bl = bl;
    }

    public static CheckEdge load(DataInput in) throws IOException {
        int fromX = in.readInt();
        int fromY = in.readInt();
        int fromZ = in.readInt();
        int toX = in.readInt();
        int toY = in.readInt();
        int toZ = in.readInt();
        byte updateLevel = in.readByte();
        byte currStoredLevel = in.readByte();
        byte currComputedLevel = in.readByte();
        boolean bl = in.readBoolean();

        return new CheckEdge(fromX, fromY, fromZ, toX, toY, toZ, updateLevel, currStoredLevel, currComputedLevel, bl);
    }
}
