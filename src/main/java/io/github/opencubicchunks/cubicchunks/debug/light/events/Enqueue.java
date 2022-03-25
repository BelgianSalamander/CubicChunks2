package io.github.opencubicchunks.cubicchunks.debug.light.events;

import java.io.DataInput;
import java.io.IOException;

import io.github.opencubicchunks.cubicchunks.debug.light.EventType;
import io.github.opencubicchunks.cubicchunks.debug.light.LightEngineEvent;

public class Enqueue extends LightEngineEvent {
    public static final int SIZE = BASE_SIZE + 1;

    private final byte level;

    public Enqueue(int x, int y, int z, byte level) {
        super(EventType.ENQUEUE, x, y, z);

        this.level = level;
    }

    public static Enqueue load(DataInput in) throws IOException {
        int x = in.readInt();
        int y = in.readInt();
        int z = in.readInt();
        byte level = in.readByte();
        return new Enqueue(x, y, z, level);
    }
}
