package io.github.opencubicchunks.cubicchunks.config;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.level.chunk.ChunkStatus;

public class ChunkGeneratorSettingsEntry {

    public static final ChunkGeneratorSettingsEntry DEFAULT = new ChunkGeneratorSettingsEntry(ImmutableList.of(ChunkStatus.SURFACE));

    private final transient ObjectOpenHashSet<ChunkStatus> controlledStatuses = new ObjectOpenHashSet<>();
    @SerializedName("controlled_statuses") private final String controlledStatusesString;


    public ChunkGeneratorSettingsEntry(String controlledStatuses) {
        this.controlledStatusesString = controlledStatuses;
        String[] statuses = controlledStatuses.replace(" ", "").trim().split(",");

        for (String status : statuses) {
            ChunkStatus chunkStatus = ChunkStatus.byName(status.toLowerCase());
            if (chunkStatus != null) {
                this.controlledStatuses.add(chunkStatus);
            } else {
                CubicChunks.LOGGER.error("\"" + status + "\" is not a valid chunk status!");
            }
        }
    }

    public ChunkGeneratorSettingsEntry(Collection<ChunkStatus> chunkStatuses) {
        this(remapToCommentedListString(chunkStatuses));
    }

    public ObjectOpenHashSet<ChunkStatus> getControlledStatuses() {
        return controlledStatuses;
    }

    public static <T> String remapToCommentedListString(Collection<T> values) {
        StringBuilder builder = new StringBuilder();
        for (T object : values) {
            if (builder.isEmpty()) {
                builder.append(object.toString());
            } else {
                builder.append(",").append(object.toString());
            }
        }

        return builder.toString();
    }


    public static class Builder {

        private final ObjectOpenHashSet<ChunkStatus> statuses = new ObjectOpenHashSet<>();

        public Builder addControlledStatus(ChunkStatus status) {
            statuses.add(status);
            return this;
        }

        public Builder addControlledStatus(String status) {
            ChunkStatus chunkStatus = ChunkStatus.byName(status.toLowerCase());
            if (chunkStatus != null) {
                statuses.add(chunkStatus);
            } else {
                CubicChunks.LOGGER.error("\"" + status + "\" is not a valid chunk status!");
            }
            return this;
        }

        public Builder addControlledStatuses(String statusesString) {
            String[] statuses = statusesString.replace(" ", "").trim().split(",");

            for (String status : statuses) {
                addControlledStatus(status);
            }
            return this;
        }

        public ChunkGeneratorSettingsEntry build() {
            return new ChunkGeneratorSettingsEntry(statuses);
        }
    }
}
