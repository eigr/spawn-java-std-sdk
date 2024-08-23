package io.eigr.spawn.test.actors;

import domain.actors.Reply;
import domain.actors.Request;
import domain.actors.State;
import io.eigr.spawn.api.actors.ActionBindings;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.UnNamedActorBehavior;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.action;
import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.name;

public final class UnNamedActor implements StatefulActor<State> {

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        return new UnNamedActorBehavior(
                name("UnNamedActor"),
                action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
        );
    }

    private Value setLanguage(ActorContext<State> context, Request msg) {
        return Value.at()
                .response(Reply.newBuilder()
                        .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                        .build())
                .state(updateState("java"))
                .reply();
    }

    private State updateState(String language) {
        return State.newBuilder()
                .addLanguages(language)
                .build();
    }
}
