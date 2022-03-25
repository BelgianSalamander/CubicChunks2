package io.github.opencubicchunks.cubicchunks.debug.light;

import java.io.DataInput;
import java.io.IOException;
import java.io.UncheckedIOException;

import io.github.opencubicchunks.cubicchunks.debug.light.events.CheckEdge;
import io.github.opencubicchunks.cubicchunks.debug.light.events.CheckNeighborCall;
import io.github.opencubicchunks.cubicchunks.debug.light.events.Enqueue;
import io.github.opencubicchunks.cubicchunks.debug.light.events.RunUpdateForPosition;

public enum EventType {
    RUN_UPDATE(RunUpdateForPosition.SIZE, RunUpdateForPosition::load),
    CHECK_NEIGHBOR(CheckNeighborCall.SIZE, CheckNeighborCall::load),
    CHECK_EDGE(CheckEdge.SIZE, CheckEdge::load),
    ENQUEUE(Enqueue.SIZE, Enqueue::load)
    ;

    private static int maxSize;

    private final int size;
    private final Reader reader;

    EventType(int size, Reader reader) {
        this.size = size;
        this.reader = reader;
    }

    public int getSize() {
        return size;
    }

    public byte getId() {
        return (byte) ordinal();
    }

    public LightEngineEvent loadEvent(DataInput in) {
        try {
            return reader.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int getMaxSize() {
        return maxSize;
    }

    static {
        int maxSize = 0;
        for (EventType type : values()) {
            maxSize = Math.max(maxSize, type.size);
        }
        EventType.maxSize = maxSize;

        System.out.println(maxSize + " bytes per event");
    }

    private interface Reader {
        LightEngineEvent read(DataInput in) throws IOException;
    }
}
