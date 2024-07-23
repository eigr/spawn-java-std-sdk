package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.actors.*;

import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.java.test.domain.Actor.*;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class JoeActor extends StatefulActor<State> {

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        return new NamedActorBehavior(
                name("test_joe"),
                channel("test.channel"),
                action("SetLanguage", this::setLanguage)
        );
    }

    public Value setLanguage(ActorContext<State> context, Request msg) {
        if (context.getState().isPresent()) {
        }

        return Value.at()
                .response(Reply.newBuilder()
                        .setResponse("Hello From Java")
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
