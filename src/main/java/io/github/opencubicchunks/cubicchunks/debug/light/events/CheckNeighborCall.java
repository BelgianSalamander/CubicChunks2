package io.github.opencubicchunks.cubicchunks.debug.light.events;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import io.github.opencubicchunks.cubicchunks.debug.light.EventType;
import io.github.opencubicchunks.cubicchunks.debug.light.LightEngineEvent;

public class CheckNeighborCall extends LightEngineEvent {
    public static final int SIZE = BASE_SIZE + 2 * 3 * 4 + 1 + 1;

    private final int fromX, fromY, fromZ;
    private final int toX, toY, toZ;
    private final byte lightValueToPropagate;
    private final boolean bl;

    public CheckNeighborCall(int causedByUpdateAtX, int causedByUpdateAtY, int causedByUpdateAtZ, int fromX, int fromY, int fromZ, int toX, int toY, int toZ, byte lightValueToPropagate,
                             boolean bl) {
        super(EventType.CHECK_NEIGHBOR, causedByUpdateAtX, causedByUpdateAtY, causedByUpdateAtZ);
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
        this.lightValueToPropagate = lightValueToPropagate;
        this.bl = bl;
    }

    public static CheckNeighborCall load(DataInput in) throws IOException {
        int causedByUpdateAtX = in.readInt();
        int causedByUpdateAtY = in.readInt();
        int causedByUpdateAtZ = in.readInt();
        int fromX = in.readInt();
        int fromY = in.readInt();
        int fromZ = in.readInt();
        int toX = in.readInt();
        int toY = in.readInt();
        int toZ = in.readInt();
        int lightValueToPropagate = in.readByte();
        boolean bl = in.readBoolean();
        return new CheckNeighborCall(causedByUpdateAtX, causedByUpdateAtY, causedByUpdateAtZ, fromX, fromY, fromZ, toX, toY, toZ, (byte) lightValueToPropagate, bl);
    }
}
