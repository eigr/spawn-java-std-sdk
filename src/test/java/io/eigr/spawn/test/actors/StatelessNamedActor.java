package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatelessActor;
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

public final class StatelessNamedActor extends StatelessActor {

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        return new NamedActorBehavior(
                name("StatelessNamedActor"),
                action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
        );
    }

    private Value setLanguage(ActorContext<State> context, Request msg) {
        if (context.getState().isPresent()) {
        }

        return Value.at()
                .response(Reply.newBuilder()
                        .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                        .build())
                .reply();
    }
}
