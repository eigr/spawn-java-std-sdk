package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.NamedActor;
import io.eigr.spawn.java.test.domain.Actor;

@NamedActor(name = "test_joe", stateful = true, stateType = Actor.State.class, channel = "test.channel")
public class JoeActor {
    @Action(inputType = Actor.Request.class)
    public Value setLanguage(Actor.Request msg, ActorContext<Actor.State> context) {
        if (context.getState().isPresent()) {
        }

        return Value.at()
                .response(Actor.Reply.newBuilder()
                        .setResponse("Hello From Java")
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
