package io.github.opencubicchunks.cubicchunks.world.level.chunk;

public interface NoiseCube {
    int getPreliminarySurfaceForY(long quart, int y);
    void setMinY(int minY);
    void setCubic(boolean cubic);

    boolean isCubic();
}
