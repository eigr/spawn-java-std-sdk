package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.java.test.domain.Actor;

@StatefulNamedActor(name = "test_actor_constructor", stateType = Actor.State.class)
public final class ActorWithConstructor {
    private final String defaultMessage;

    public ActorWithConstructor(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    @Action(inputType = Actor.Request.class)
    public Value setLanguage(Actor.Request msg, ActorContext<Actor.State> context) {
        if (context.getState().isPresent()) {
        }

        return Value.at()
                .response(Actor.Reply.newBuilder()
                        .setResponse(defaultMessage)
                        .build())
                .state(updateState("java"))
                .reply();
    }

    private Actor.State updateState(String language) {
        return Actor.State.newBuilder()
                .addLanguages(language)
                .build();
    }
}
