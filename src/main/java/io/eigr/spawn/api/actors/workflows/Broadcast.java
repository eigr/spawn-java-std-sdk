package io.eigr.spawn.api.actors.workflows;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class Broadcast<T extends GeneratedMessageV3> {

    private final Optional<String> channel;
    private final Optional<String> action;
    private final T payload;

    private Broadcast(Optional<String> channel, Optional<String> action, T payload) {
        this.channel = channel;
        this.action = action;
        this.payload = payload;
    }

    @NotNull
    public static <T extends GeneratedMessageV3> Broadcast to(String channel, String action, T payload) {
        return new Broadcast<T>(Optional.of(channel), Optional.of(action), payload);
    }

    @NotNull
    public static <T extends GeneratedMessageV3> Broadcast to(String channel, T payload) {
        return new Broadcast<T>(Optional.ofNullable(channel), Optional.empty(), payload);
    }

    public Optional<String> getChannel() {
        return channel;
    }

    public Optional<String> getAction() {
        return action;
    }

    public T getPayload() {
        return payload;
    }

    public Protocol.Broadcast build() {
        Protocol.Broadcast.Builder builder = Protocol.Broadcast.newBuilder();
        if (this.action.isPresent()) {
            builder.setActionName(this.action.get());
        }

        if (this.channel.isPresent()) {
            builder.setActionName(this.channel.get());
        }

        if (Objects.isNull(this.payload)) {
            builder.setNoop(Protocol.Noop.newBuilder().build());
        } else {
            builder.setValue(Any.pack(this.payload));
        }

        return builder.build();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Broadcast{");
        sb.append("channel='").append(channel).append('\'');
        sb.append(", action=").append(action);
        sb.append(", payload=").append(payload);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Broadcast<?> broadcast = (Broadcast<?>) o;
        return Objects.equals(channel, broadcast.channel) && Objects.equals(action, broadcast.action) && Objects.equals(payload, broadcast.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, action, payload);
    }
}
