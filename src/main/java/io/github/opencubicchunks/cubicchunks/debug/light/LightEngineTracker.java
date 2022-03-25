package io.github.opencubicchunks.cubicchunks.debug.light;

import java.io.BufferedOutputStream;
import java.nio.ByteBuffer;

import io.github.opencubicchunks.cubicchunks.debug.light.events.CheckEdge;
import io.github.opencubicchunks.cubicchunks.debug.light.events.CheckNeighborCall;
import io.github.opencubicchunks.cubicchunks.debug.light.events.Enqueue;
import io.github.opencubicchunks.cubicchunks.debug.light.events.RunUpdateForPosition;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public class LightEngineTracker {
    private static final int SIZE = 256 * 1024 * 1024; // 256MB

    private BufferOutputStream out;
    private boolean released = false;

    public LightEngineTracker() {
        ByteBuffer buffer = MemoryUtil.memAlloc(SIZE);

        out = new BufferOutputStream(buffer);
    }


    private void align() {
        out.align(EventType.getMaxSize());
    }

    public void writeRunUpdate(int x, int y, int z, int currLevel, int computedLevel) {
        align();
        //For debug we check we wrote the right amount
        int startPos = out.getBuffer().position();

        out.writeByte(EventType.RUN_UPDATE.getId());

        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);

        out.writeByte(currLevel);
        out.writeByte(computedLevel);

        assert out.getBuffer().position() - startPos == RunUpdateForPosition.SIZE;
    }

    public void writeCheckNeighbor(int causeByUpdateAtX, int causedByUpdateAtY, int causedByUpdateAtZ, int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int lightValue,
                                   boolean decrease) {
        align();
        //For debug we check we wrote the right amount
        int startPos = out.getBuffer().position();

        out.writeByte(EventType.CHECK_NEIGHBOR.getId());

        out.writeInt(causeByUpdateAtX);
        out.writeInt(causedByUpdateAtY);
        out.writeInt(causedByUpdateAtZ);

        out.writeInt(fromX);
        out.writeInt(fromY);
        out.writeInt(fromZ);

        out.writeInt(toX);
        out.writeInt(toY);
        out.writeInt(toZ);

        out.writeByte(lightValue);

        out.writeBoolean(decrease);

        assert out.getBuffer().position() - startPos == CheckNeighborCall.SIZE;
    }

    public void writeEnqueue(int x, int y, int z, int lightLevel) {
        align();
        //For debug we check we wrote the right amount
        int startPos = out.getBuffer().position();

        out.writeByte(EventType.ENQUEUE.getId());

        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(z);

        out.writeByte(lightLevel);

        assert out.getBuffer().position() - startPos == Enqueue.SIZE;
    }

    public void writeCheckEdge(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int updateLevel, int currStoredLevel, int currComputedLevel, boolean decrease) {
        align();
        //For debug we check we wrote the right amount
        int startPos = out.getBuffer().position();

        out.writeByte(EventType.CHECK_EDGE.getId());

        out.writeInt(fromX);
        out.writeInt(fromY);
        out.writeInt(fromZ);

        out.writeInt(toX);
        out.writeInt(toY);
        out.writeInt(toZ);

        out.writeByte(updateLevel);
        out.writeByte(currStoredLevel);
        out.writeByte(currComputedLevel);

        out.writeBoolean(decrease);

        assert out.getBuffer().position() - startPos == CheckEdge.SIZE;
    }

    public void release() {
        if (released) {
            return;
        }

        MemoryUtil.memFree(out.getBuffer());

        released = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    public interface Access {
        @Nullable
        LightEngineTracker getTracker();

        void setTracker(LightEngineTracker tracker);
    }
}
