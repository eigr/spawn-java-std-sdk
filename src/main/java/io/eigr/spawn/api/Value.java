package io.eigr.spawn.api;

import com.google.protobuf.GeneratedMessageV3;
import io.eigr.spawn.api.actors.workflows.Broadcast;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.api.actors.workflows.Pipe;
import io.eigr.spawn.api.actors.workflows.SideEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Value<S extends GeneratedMessageV3, R extends GeneratedMessageV3> {

    private S state;
    private R response;

    private boolean checkpoint;
    private Optional<Broadcast<?>> broadcast;
    private Optional<Forward> forward;
    private Optional<Pipe> pipe;
    private Optional<List<SideEffect>> effects;

    private ResponseType type;

    private Value() {
        this.state = null;
        this.response = null;
        this.checkpoint = false;
        this.broadcast = Optional.empty();
        this.forward = Optional.empty();
        this.pipe = Optional.empty();
        this.effects = Optional.empty();
        this.type = ResponseType.EMPTY_REPLY;
    }

    private Value(
            R response,
            S state,
            boolean checkpoint,
            Optional<Broadcast<?>> broadcast,
            Optional<Forward> forward,
            Optional<Pipe> pipe,
            Optional<List<SideEffect>> effects,
            ResponseType type) {
        this.response = response;
        this.state = state;
        this.checkpoint = checkpoint;
        this.broadcast = broadcast;
        this.forward = forward;
        this.pipe = pipe;
        this.effects = effects;
        this.type = type;
    }

    public static <S, V> Value at() {
        return new Value();
    }

    public R getResponse() {
        return response;
    }

    public S getState() {
        return state;
    }

    public boolean getCheckpoint() {
        return checkpoint;
    }

    public Optional<Broadcast<?>> getBroadcast() {
        return broadcast;
    }

    public Optional<Forward> getForward() {
        return forward;
    }

    public Optional<Pipe> getPipe() {
        return pipe;
    }

    public Optional<List<SideEffect>> getEffects() {
        return effects;
    }

    public ResponseType getType() {
        return type;
    }

    public Value response(R value) {
        this.response = value;
        return this;
    }

    public Value state(S state) {
        this.state = state;
        return this;
    }

    public Value state(S state, boolean checkpoint) {
        this.state = state;
        this.checkpoint = checkpoint;
        return this;
    }

    public Value flow(Broadcast broadcast) {
        this.broadcast = Optional.of(broadcast);
        return this;
    }

    public Value flow(Forward forward) {
        this.forward = Optional.ofNullable(forward);
        return this;
    }

    public Value flow(Pipe pipe) {
        this.pipe = Optional.of(pipe);
        return this;
    }

    public Value flow(SideEffect effect) {
        List<SideEffect> ef;
        if (this.effects.isPresent()) {
            ef = this.effects.get();
            ef.add(effect);
        } else {
            ef = new ArrayList<>();
            ef.add(effect);
        }

        this.effects = Optional.of(ef);
        return this;
    }

    public Value flow(List<SideEffect> effects) {
        this.effects = Optional.of(effects);
        return this;
    }

    public Value reply() {
        return new Value(this.response, this.state, this.checkpoint, this.broadcast, this.forward, this.pipe, this.effects, ResponseType.REPLY);
    }

    public Value noReply() {
        return new Value(this.response, this.state, this.checkpoint, this.broadcast, this.forward, this.pipe, this.effects, ResponseType.NO_REPLY);
    }

    public Value empty() {
        return new Value();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Value{");
        sb.append("state=").append(state);
        sb.append("checkpoint=").append(checkpoint);
        sb.append(", value=").append(response);
        sb.append(", broadcast=").append(broadcast);
        sb.append(", forward=").append(forward);
        sb.append(", pipe=").append(pipe);
        sb.append(", effects=").append(effects);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value<?, ?> value = (Value<?, ?>) o;
        return Objects.equals(state, value.state) &&
                Objects.equals(response, value.response) &&
                Objects.equals(checkpoint, value.checkpoint) &&
                Objects.equals(broadcast, value.broadcast) &&
                Objects.equals(forward, value.forward) &&
                Objects.equals(pipe, value.pipe) &&
                Objects.equals(effects, value.effects) &&
                type == value.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, response, checkpoint, broadcast, forward, pipe, effects, type);
    }

    enum ResponseType {
        REPLY, NO_REPLY, EMPTY_REPLY
    }

}
