package io.eigr.spawn;

import io.eigr.spawn.api.actors.ActorBehavior;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatelessActorBehavior;
import io.eigr.spawn.api.actors.Value;

import io.eigr.spawn.java.test.domain.Actor.*;

import static io.eigr.spawn.api.actors.ActorBehavior.*;

public class ActorBehaviorTestEntity extends StatelessActorBehavior {

    @Override
    public ActorBehavior setup() {
        return new ActorBehavior(
                name("test"),
                system("spawn-system"),
                deactivated(30000),
                snapshot(10000),
                action("hi", (ctx) -> Value.at().noReply()),
                action("SayHello", this::sayHello),
                action("SayHelloTwo", (ctx, arg) -> Value.at().noReply())
        );
    }

    public Value sayHello(ActorContext<State> context, Request req) {
        return Value.at().noReply();
    }
}
