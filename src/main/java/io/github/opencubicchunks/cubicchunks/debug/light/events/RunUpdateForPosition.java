package io.github.opencubicchunks.cubicchunks.debug.light.events;

import java.io.DataInput;
import java.io.IOException;

import io.github.opencubicchunks.cubicchunks.debug.light.EventType;
import io.github.opencubicchunks.cubicchunks.debug.light.LightEngineEvent;

public class RunUpdateForPosition extends LightEngineEvent {
    public static int SIZE = BASE_SIZE + 2 * 1;

    private final byte currentStoredLevel;
    private final byte newComputedLevel;

    public RunUpdateForPosition(int x, int y, int z, byte currentStoredLevel, byte newComputedLevel) {
        super(EventType.RUN_UPDATE, x, y, z);

        this.currentStoredLevel = currentStoredLevel;
        this.newComputedLevel = newComputedLevel;
    }

    public static RunUpdateForPosition load(DataInput in) throws IOException {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        byte currentStoredLevel = in.readByte();
        byte newComputedLevel = in.readByte();
        return new RunUpdateForPosition(x, y, z, currentStoredLevel, newComputedLevel);
    }
}
