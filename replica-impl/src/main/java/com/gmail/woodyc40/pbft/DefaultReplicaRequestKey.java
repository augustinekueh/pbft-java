package com.gmail.woodyc40.pbft;

import org.checkerframework.checker.nullness.qual.NonNull;

public class DefaultReplicaRequestKey implements ReplicaRequestKey {
    private final String clientId;
    private final long timestamp;

    public DefaultReplicaRequestKey(String clientId, long timestamp) {
        this.clientId = clientId;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        DefaultReplicaRequestKey that = (DefaultReplicaRequestKey) o;

        if (this.timestamp != that.timestamp) return false;
        return this.clientId != null ? this.clientId.equals(that.clientId) : that.clientId == null;
    }

    @Override
    public int hashCode() {
        int result = this.clientId != null ? this.clientId.hashCode() : 0;
        result = 31 * result + (int) (this.timestamp ^ (this.timestamp >>> 32));
        return result;
    }

    @Override
    public @NonNull String clientId() {
        return this.clientId;
    }

    @Override
    public long timestamp() {
        return this.timestamp;
    }
}
