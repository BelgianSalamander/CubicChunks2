package io.github.opencubicchunks.cubicchunks.debug.light;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jetbrains.annotations.NotNull;

public class BufferOutputStream extends OutputStream implements DataOutput {
    private final ByteBuffer buffer;

    public BufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void align(int alignment) {
        if (alignment <= 1) return;

        int remainder = buffer.position() % alignment;
        if (remainder == 0) return;

        int padding = alignment - remainder;
        buffer.position(buffer.position() + padding);
    }

    @Override
    public void write(int b) {
        buffer.put((byte) b);
    }

    @Override
    public void writeBoolean(boolean v) {
        buffer.put((byte) (v ? 1 : 0));
    }

    @Override
    public void writeByte(int v) {
        this.write(v);
    }

    @Override
    public void writeShort(int v) {
        buffer.putShort((short) v);
    }

    @Override
    public void writeChar(int v) {
        buffer.putChar((char) v);
    }

    @Override
    public void writeInt(int v) {
        buffer.putInt(v);
    }

    @Override
    public void writeLong(long v) {
        buffer.putLong(v);
    }

    @Override
    public void writeFloat(float v) {
        buffer.putFloat(v);
    }

    @Override
    public void writeDouble(double v) {
        buffer.putDouble(v);
    }

    @Override
    public void writeBytes(@NotNull String s) {
        for (int i = 0; i < s.length(); i++) {
            this.writeByte(s.charAt(i));
        }
    }

    @Override
    public void writeChars(@NotNull String s) {
        for (int i = 0; i < s.length(); i++) {
            this.writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeUTF(@NotNull String s) {
        throw new UnsupportedOperationException();
    }
}
