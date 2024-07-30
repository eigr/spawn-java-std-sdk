package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.test.domain.Actor.Reply;
import io.eigr.spawn.java.test.domain.Actor.Request;
import io.eigr.spawn.java.test.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.action;
import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.name;
public final class ActorWithConstructor implements StatefulActor<State> {

    private String defaultMessage;

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        defaultMessage = context.getInjector().getInstance(String.class);
        return new NamedActorBehavior(
                name("TestActorConstructor"),
                action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
        );
    }

    private Value setLanguage(ActorContext<State> context, Request msg) {
        return Value.at()
                .response(Reply.newBuilder()
                        .setResponse(defaultMessage)
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
