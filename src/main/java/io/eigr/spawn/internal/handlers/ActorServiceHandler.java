package io.eigr.spawn.internal.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.eigr.spawn.Actor;

import java.io.IOException;
import java.util.List;

public final class ActorServiceHandler implements HttpHandler {

    private String system;

    private List<Actor> actors;

    public ActorServiceHandler(String system, List<Actor> actors) {
        this.system = system;
        this.actors = actors;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

    }
}
