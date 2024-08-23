package io.eigr.spawn.test.actors;

import domain.actors.Reply;
import domain.actors.Request;
import io.eigr.spawn.api.actors.ActionBindings;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatelessActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.action;
import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.name;

public class StatelessNamedActor implements StatelessActor {

    @Override
    public ActorBehavior configure(BehaviorCtx context) {
        return new NamedActorBehavior(
                name("StatelessNamedActor"),
                action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
        );
    }

    private Value setLanguage(ActorContext<?> context, Request msg) {
        return Value.at()
                .response(Reply.newBuilder()
                        .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                        .build())
                .reply();
    }
}
