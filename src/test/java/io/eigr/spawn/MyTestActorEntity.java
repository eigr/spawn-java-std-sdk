package io.eigr.spawn;

import io.eigr.spawn.api.actors.*;

import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.java.test.domain.Actor.*;

import java.util.Optional;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public class MyTestActorEntity extends StatefulActor<State> {

    @Override
    public ActorBehavior configure() {
        return new NamedActorBehavior(
                name("test"),
                deactivated(30000),
                snapshot(10000),
                init(this::handleInit),
                action("Hi", ctx -> Value.at().noReply()),
                action("SayHello", this::sayHello),
                action("SayHelloTwo", (ctx, arg) -> Value.at().noReply())
        );
    }
    public Value sayHello(ActorContext<State> ctx, Request req) {
        return Value.at().response(Request.newBuilder().setLanguage("Java").build()).reply();
    }

    private Value handleInit(ActorContext ctx) {
        Optional<ActorContext<State>> maybeState = ctx.getState();
        if (maybeState.isPresent()) {
            State state = maybeState.get().getState().get();
            return Value.at()
                    .state(state)
                    .noReply();
        }
        return Value.at()
                .state(State.newBuilder().addLanguages("Java").build())
                .noReply();
    }
}
