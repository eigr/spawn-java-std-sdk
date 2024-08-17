# Spawn JVM SDK
[![](https://jitpack.io/v/eigr/spawn-java-std-sdk.svg)](https://jitpack.io/#eigr/spawn-java-std-sdk)

JVM User Language Support for [Spawn](https://github.com/eigr/spawn).

# Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
   - [Deploy](#deploy)
3. [Advanced Use Cases](#advanced-use-cases)
    - [Dependency Injection](#dependency-injection)
    - [Types of Actors](#types-of-actors)
    - [Stateless Actors](#stateless-actors)
    - [Considerations about Spawn actors](#considerations-about-spawn-actors)
    - [Broadcast](#broadcast)
    - [Side Effects](#side-effects)
    - [Forward](#forward)
    - [Pipe](#pipe)
    - [State Management](#state-management)
4. [Using Actors](#using-actors)
    - [Call Named Actors](#call-named-actors)
    - [Call Unnamed Actors](#call-unnamed-actors)
    - [Async](#async)
    - [Timeouts](#timeouts)
5. [Deploy](#deploy)
    - [Defining an ActorSystem](#defining-an-actorsystem)
    - [Defining an ActorHost](#defining-an-actorhost)
    - [Activators](#activators)
6. [Actor Model](#actor-model)
    - [Virtual Actors](#virtual-actors)


## Overview

Spawn is a Stateful Serverless Runtime and Framework based on the [Actor Model](https://youtu.be/7erJ1DV_Tlo) and operates as a Service Mesh.

Spawn's main goal is to remove the complexity in developing services or microservices, providing simple and intuitive APIs, 
as well as a declarative deployment and configuration model and based on a Serverless architecture and Actor Model.
This leaves the developer to focus on developing the business domain while the platform deals with the complexities and 
infrastructure needed to support the scalable, resilient, distributed, and event-driven architecture that modern systems requires.

Spawn is based on the sidecar proxy pattern to provide a polyglot Actor Model framework and platform.
Spawn's technology stack, built on the [BEAM VM](https://www.erlang.org/blog/a-brief-beam-primer/) (Erlang's virtual machine) 
and [OTP](https://www.erlang.org/doc/design_principles/des_princ.html), provides support for different languages from its native Actor model.

For more information consult the main repository [documentation](https://github.com/eigr/spawn).

## Getting Started

First we must need to install spawn cli tool to create a new Java project.

```shell
curl -sSL https://github.com/eigr/spawn/releases/download/v1.4.2/install.sh | sh
```
Now you will need to fill in the data for groupId, artifactId, version, and package. 
Let's call our maven artifact spawn-java-demo. The output of this command will be similar to the output below

```shell
spawn new java hello_world --group-id=io.eigr.spawn --artifact-id=spawn-java-demo --version=1.0.0 --package=io.eigr.spawn.java.demo
```
Now it is necessary to download the dependencies via Maven:

```shell
cd spawn-java-demo && mvn install
```

So far it's all pretty boring and not really Spawn related, so it's time to start playing for real.
The first thing we're going to do is define a place to put our protobuf files. 

```shell
touch src/main/proto/domain/domain.proto
```

And let's populate this file with the following content:

```protobuf
syntax = "proto3";

package domain;
option java_package = "io.eigr.spawn.java.demo.domain";

message State {
   repeated string languages = 1;
}

message Request {
   string language = 1;
}

message Reply {
   string response = 1;
}

service JoeActor {
   rpc SetLanguage(Request) returns (Reply);
}
```

We must compile this file using the protoc utility. In the root of the project type the following command:

```shell
mvn protobuf:compile
```

Now in the spawn-java-demo folder we will create our first Java file containing the code of our Actor.

```shell
touch src/main/java/io/eigr/spawn/java/demo/Joe.java
```

Populate this file with the following content:

```Java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class JoeActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("JoeActor"),
              channel("test.channel"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      if (context.getState().isPresent()) {
         //Do something with previous state
      }

      return Value.at()
              .response(Reply.newBuilder()
                      .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                      .build())
              .state(updateState(msg.getLanguage()))
              .reply();
   }

   private State updateState(String language) {
      return State.newBuilder()
              .addLanguages(language)
              .build();
   }
}
```

### Dissecting the code

***Class Declaration***

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.java.demo.domain.Actor.State;

public final class JoeActor implements StatefulActor<State> {
 // ...
}
```

The `JoeActor` class implements `StatefulActor<State>` interface. `StatefulActor` is a generic interface provided by the Spawn API, 
which takes a type parameter for the state. In this case, the state type is `io.eigr.spawn.java.demo.domain.Actor.State` 
defined in above protobuf file.

***Configure Actor Behavior***

```java
public final class JoeActor implements StatefulActor<State> {
   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("JoeActor"),
              channel("test.channel"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }
}
```

This `configure` method is overridden from `StatefulActor` and is used to configure the actor's behavior.

* `name("JoeActor")`: Specifies the name of the actor. Note that the Actor name has the same name as the service declared 
                      in protobuf. This is not a coincidence, the Spawn proxy uses the protobuf metadata to map **actors** and 
                      their **actions** and therefore these names should correctly reflect this behavior.
* `channel("test.channel")`: Specifies the channel the actor listens to. See [Broadcast](#broadcast) section below.
* `action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))`: Binds the `SetLanguage` action to the `setLanguage` method, 
                                                                                which takes a `Request` message as input. 
                                                                                Where the second parameter of `ActionBindings.of(type, lambda)` method is a lambda.

***Handle request***

```java
public final class JoeActor implements StatefulActor<State> {
   //
   private Value setLanguage(ActorContext<State> context, Request msg) {
      if (context.getState().isPresent()) {
         // Do something with the previous state
      }

      return Value.at()
              .response(Reply.newBuilder()
                      .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                      .build())
              .state(updateState(msg.getLanguage()))
              .reply();
   }
}
```

This method `setLanguage` is called when the `SetLanguage` action is invoked. It takes an `ActorContext<State>` and a `Request` message as parameters.

* `context.getState().isPresent()`: Checks if there is a previous existing state.
* The method then creates a new `Value` response:
  * `response(Reply.newBuilder().setResponse(...).build())`: Builds a `Reply` object with a response message.
  * `state(updateState(msg.getLanguage()))`: Updates the state with the new language.
  * `reply()`: Indicates that this is a reply message. You could also ignore the reply if you used a `noReply()` method instead of the `reply` method.

Ok now with our Actor properly defined, we just need to start the SDK correctly. Create another file called App.java 
to serve as your application's entrypoint and fill it with the following content:

```Java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;

public class App {
   public static void main(String[] args) throws Exception {
      Spawn spawnSystem = new SpawnSystem()
              .create("spawn-system")
              .withActor(Joe.class)
              .build();

      spawnSystem.start();
   }
}
```

Or passing transport options like:

```Java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.TransportOpts;

public class App {
   public static void main(String[] args) throws Exception {
      TransportOpts opts = TransportOpts.builder()
              .port(8091)
              .proxyPort(9003)
              .executor(Executors.newVirtualThreadPerTaskExecutor()) // If you use java above 19 and use the --enable-preview flag when running the jvm
              .build();

      Spawn spawnSystem = new SpawnSystem()
              .create("spawn-system")
              .withActor(Joe.class)
              .withTransportOptions(opts)
              .build();

      spawnSystem.start();
   }
}
```

Then:

```shell
mvn compile && mvn package && java -jar target/spawn-java-demo-1.0-SNAPSHOT.jar 
```

But of course you will need to locally run the Elixir proxy which will actually provide all the functionality for your Java application. 
One way to do this is to create a docker-compose file containing all the services that your application depends on, 
in this case, in addition to the Spawn proxy, it also has a database and possibly a nats broker if you want access to 
more advanced Spawn features.

```docker-compose
version: "3.8"
services:
  mariadb:
    image: mariadb
    environment:
      MYSQL_ROOT_PASSWORD: admin
      MYSQL_DATABASE: eigr-functions-db
      MYSQL_USER: admin
      MYSQL_PASSWORD: admin
    volumes:
      - mariadb:/var/lib/mysql
    ports:
      - "3307:3306"
  nats:
    image: nats:0.8.0
    entrypoint: "/gnatsd -DV"
    ports:
      - "8222:8222"
      - "4222:4222"
  spawn-proxy:
    build:
      context: https://github.com/eigr/spawn.git#main
      dockerfile: ./Dockerfile-proxy
    restart: always
    network_mode: "host"
    environment:
      SPAWN_USE_INTERNAL_NATS: "true"
      SPAWN_PUBSUB_ADAPTER: nats
      SPAWN_STATESTORE_KEY: 3Jnb0hZiHIzHTOih7t2cTEPEpY98Tu1wvQkPfq/XwqE=
      PROXY_APP_NAME: spawn
      PROXY_CLUSTER_STRATEGY: gossip
      PROXY_DATABASE_PORT: 3307
      PROXY_DATABASE_TYPE: mariadb
      PROXY_HTTP_PORT: 9003
      USER_FUNCTION_PORT: 8091
    depends_on:
      - mariadb
      - nats
networks:
  mysql-compose-network:
    driver: bridge
volumes:
  mariadb:

```

> **_NOTE:_** Or just use the [Spawn CLI](https://github.com/eigr/spawn?tab=readme-ov-file#getting-started-with-spawn) to take care of the development environment for you.

You may also want your Actors to be initialized with some dependent objects similarly to how you would use the 
dependency injection pattern. 
In this case, it is enough to declare a constructor that receives a single argument for its actor.

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.action;
import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.name;
public final class JoeActor implements StatefulActor<State> {

   private String defaultMessage;

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      defaultMessage = context.getInjector().getInstance(String.class);
      return new NamedActorBehavior(
              name("JoeActor"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   // ...
}
```

Then you also need to register your Actor using injector :

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;

import java.util.HashMap;
import java.util.Map;

public class App {
   public static void main(String[] args) {
      DependencyInjector injector = SimpleDependencyInjector.createInjector();
      injector.bind(String.class, "Hello with Constructor");

      Spawn spawnSystem = new Spawn.SpawnSystem()
              .create("spawn-system", injector)
              .withActor(Joe.class)
              .build();

      spawnSystem.start();
   }
}
```

Spawn is based on kubernetes and containers, so you will need to generate a docker container for your application.
There are many ways to do this, one of them is by adding Maven's jib plugin. 
Add the following lines to your plugin's section in pom.xml file:

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.3.2</version>
    <configuration>
        <to>
            <image>your-repo-here/spawn-java-demo</image>
        </to>
    </configuration>
</plugin>
```
finally you will be able to create your container by running the following command in the root of your project:

```shell
mvn compile jib:build
```

And this is it to start! Now that you know the basics of local development, we can go a little further.

### Deploy

Please see main documentation [page](https://github.com/eigr/spawn/blob/main/docs/getting_started.md).

## Advanced Use Cases

Spawn Actors abstract a huge amount of developer infrastructure and can be used for many types of jobs. 
In the sections below we will demonstrate some features available in Spawn that contribute to the development of 
complex applications in a simplified way.

### Dependency Injection

Sometimes we need to pass many arguments as dependencies to the Actor class.
In this case, it is more convenient to use your own dependency injection mechanism.
However, the Spawn SDK already comes with an auxiliary class to make this easier for the developer.
Let's look at an example:

1. First let's take a look at some example dependency classes:

```java
// We will have an interface that represents any type of service.
public interface MessageService {
   String getDefaultMessage();
}

// and concrete implementation here
public class MessageServiceImpl implements MessageService {
   @Override
   public String getDefaultMessage() {
      return "Hello Spawn in English";
   }
}
```

2. Second, let's define an actor so that it receives an instance of the DependencyInjector class through the context of configure method:

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.action;
import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.name;
public final class JoeActor implements StatefulActor<State> {

   private String defaultMessage;

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      defaultMessage = context.getInjector().getInstance(String.class);
      return new NamedActorBehavior(
              name("JoeActor"),
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
```

3. Then you can pass your dependent classes this way to your Actor:
```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.extensions.DependencyInjector;
import io.eigr.spawn.api.extensions.SimpleDependencyInjector;

public class App {
   public static void main(String[] args) {
      DependencyInjector injector = SimpleDependencyInjector.createInjector();
      /* 
      You can bind as many objects as you want. As long as they are of unique types.
      If you try to add different instances of the same type you will receive an error.        
      */ 
      injector.bind(MessageService.class, new MessageServiceImpl());
      
      // or using alias for put different values of same key types
      injector.bind(MessageService.class, "myMessageService", new MessageServiceImpl());

      Spawn spawnSystem = new Spawn.SpawnSystem()
              .create("spawn-system", injector)
              .withActor(Joe.class)
              .build();

      spawnSystem.start();
   }
}
```

It is important to note that this helper mechanism does not currently implement any type of complex dependency graph. 
Therefore, it will not build objects based on complex dependencies nor take care of the object lifecycle for you. 
In other words, all instances added through the bind method of the SimpleDependencyInjector class will be singletons. 
This mechanism works much more like a bucket of objects that will be forwarded via your actor's context.

> **_NOTE:_** **Why not use the java cdi 2.0 spec?**
Our goals are to keep the SDK for standalone Java applications very simple. We consider that implementing the entire specification would not be viable for us at the moment. It would be a lot of effort and energy expenditure that we consider spending on other parts of the ecosystem that we think will guarantee us more benefits.
However, as an open source project we will be happy if anyone wants to contribute in this regard.


### Types of Actors

First we need to understand how the various types of actors available in Spawn behave. Spawn defines the following types of Actors:

* **Named Actors**: Named actors are actors whose name is defined at compile time. They also behave slightly differently 
Then unnamed actors and pooled actors. Named actors when they are defined with the stateful parameter equal to True are 
immediately instantiated when they are registered at the beginning of the program, they can also only be referenced by 
the name given to them in their definition.

* **Unnamed Actors**: Unlike named actors, unnamed actors are only created when they are named at runtime, that is, 
during program execution. Otherwise, they behave like named actors.

* **Pooled Actors**: Pooled Actors, as the name suggests, are a collection of actors that are grouped under the same name 
assigned to them at compile time. Pooled actors are generally used when higher performance is needed and are also 
recommended for handling serverless loads.

### Stateless Actors

In addition to these types, Spawn also allows the developer to choose Stateful actors, who need to maintain the state, 
or Stateless, those who do not need to maintain the state.
For this the developer just needs to make extend of the correct base interface. For example, I could declare a Serverless Actor using the following code:

```java
package io.eigr.spawn.java.demo.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatelessActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.action;
import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.name;

public final class StatelessNamedActor implements StatelessActor {

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

```

Other than that the same Named, UnNamed types are supported. Just use the NamedActorBehavior or UnNamedActorBehavior class inside a `configure` method.

### Considerations about Spawn actors

Another important feature of Spawn Actors is that the lifecycle of each Actor is managed by the platform itself. 
This means that an Actor will exist when it is invoked and that it will be deactivated after an idle time in its execution. 
This pattern is known as [Virtual Actors](#virtual-actors) but Spawn's implementation differs from some other known 
frameworks like [Orleans](https://www.microsoft.com/en-us/research/project/orleans-virtual-actors/) or 
[Dapr](https://docs.dapr.io/developing-applications/building-blocks/actors/actors-overview/) 
by defining a specific behavior depending on the type of Actor (named, unnamed, pooled, and etc...).

For example, named actors are instantiated the first time as soon as the host application registers them with the Spawn proxy. 
Whereas unnamed and pooled actors are instantiated the first time only when they receive their first invocation call.

### Broadcast

Actors in Spawn can subscribe to a thread and receive, as well as broadcast, events for a given thread.

To consume from a topic, you just need to configure the Actor using the channel option as follows:

```
return new NamedActorBehavior(
  name("JoeActor"),
  channel("test.channel"),
);
```
In the case above, the Actor `JoeActor` was configured to receive events that are forwarded to the topic called `test.channel`.

To produce events in a topic, just use the Broadcast Workflow. The example below demonstrates a complete example of 
producing and consuming events. In this case, the same actor is the event consumer and producer, but in a more realistic scenario, 
different actors would be involved in these processes.

```Java
package io.eigr.spawn.java.demo.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.api.actors.workflows.Broadcast;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class LoopActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("LoopActor"),
              channel("test.channel"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      return Value.at()
              .flow(Broadcast.to("test.channel", "setLanguage", msg))
              .response(Reply.newBuilder()
                      .setResponse("Hello From Erlang")
                      .build())
              .state(updateState("erlang"))
              .reply();
   }

   // ...
}
```

### Side Effects

Actors can also emit side effects to other Actors as part of their response.
See an example:

```Java
package io.eigr.spawn.java.demo.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class JoeActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("JoeActor"),
              channel("test.channel"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      ActorRef sideEffectReceiverActor = ctx.getSpawnSystem()
              .createActorRef(ActorIdentity.of("spawn-system", "MikeFriendActor", "MikeParentActor"));

      return Value.at()
              .flow(SideEffect.to(sideEffectReceiverActor, "setLanguage", msg))
              .response(Reply.newBuilder()
                      .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                      .build())
              .state(updateState(msg.getLanguage()))
              .noReply();
   }
   
   // ....
}
```

Side effects such as broadcast are not part of the response flow to the caller. They are request-asynchronous events that 
are emitted after the Actor's state has been saved in memory.

### Forward

Actors can route some actions to other actors as part of their response. For example, sometimes you may want another 
Actor to be responsible for processing a message that another Actor has received. We call this forwarding, 
and it occurs when we want to forward the input argument of a request that a specific Actor has received to the input of 
an action in another Actor.

See an example:

```Java
package io.eigr.spawn.java.demo.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class RoutingActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("RoutingActor"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      ActorRef forwardedActor = ctx.getSpawnSystem()
              .createActorRef(ActorIdentity.of("spawn-system", "MikeFriendActor", "MikeActor"));

      return Value.at()
              .flow(Forward.to(forwardedActor, "setLanguage"))
              .noReply();
   }
}
```

### Pipe

Similarly, sometimes we want to chain a request through several processes. For example forwarding an actor's computational 
output as another actor's input. There is this type of routing we call Pipe, as the name suggests, a pipe forwards what 
would be the response of the received request to the input of another Action in another Actor.
In the end, just like in a Forward, it is the response of the last Actor in the chain of routing to the original caller.

Example:

```Java
package io.eigr.spawn.java.demo.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class PipeActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("PipeActor"),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      ActorRef pipeReceiverActor = ctx.getSpawnSystem()
              .createActorRef(ActorIdentity.of("spawn-system", "JoeActor"));

      return Value.at()
              .response(Reply.newBuilder()
                      .setResponse("Hello From Java")
                      .build())
              .flow(Pipe.to(pipeReceiverActor, "someAction"))
              .state(updateState("java"))
              .noReply();
   }
   
   // ...
}
```

Forwards and pipes do not have an upper thread limit other than the request timeout.

### State Management

The Spawn runtime handles the internal state of your actors. It is he who maintains its state based on the types of actors 
and configurations that you, the developer, have made.

The persistence of the state of the actors happens through snapshots that follow to [Write Behind Pattern](https://redisson.org/glossary/write-through-and-write-behind-caching.html) 
during the period in which the Actor is active and [Write Ahead](https://martinfowler.com/articles/patterns-of-distributed-systems/wal.html) 
during the moment of the Actor's deactivation. 
That is, data is saved at regular intervals asynchronously while the Actor is active and once synchronously 
when the Actor suffers a deactivation, when it is turned off.

These snapshots happen from time to time. And this time is configurable through the ***snapshotTimeout*** method of 
the ***NamedActorBehavior*** or ***UnNamedActorBehavior*** class. 
However, you can tell the Spawn runtime that you want it to persist the data immediately synchronously after executing an Action.
And this can be done in the following way:

Example:

```Java
package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.NamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class JoeActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new NamedActorBehavior(
              name("JoeActor"),
              snapshot(1000),
              deactivated(60000),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      return Value.at()
              .response(Reply.newBuilder()
                      .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                      .build())
              .state(updateState(msg.getLanguage()), true)
              .reply();
   }

  // ...
}
```

The most important thing in this example is the use of the last parameter with the true value:

```
state(updateState("java"), true)
```

It is this parameter that will indicate to the Spawn runtime that you want the data to be saved immediately after this 
Action is called back.
In most cases this strategy is completely unnecessary, as the default strategy is sufficient for most use cases. 
But Spawn democratically lets you choose when you want your data persisted.

In addition to this functionality regarding state management, Spawn also allows you to perform some more operations 
on your Actors such as restoring the actor's state to a specific point in time:

Restore Example:

TODO

## Using Actors

There are several ways to interact with our actors, some internal to the application code and others external to the application code. 
In this section we will deal with the internal ways of interacting with our actors and this will be done through direct calls to them. 
For more details on the external ways to interact with your actors see the [Activators](#activators) section.

In order to be able to call methods of an Actor, we first need to get a reference to the actor. This is done with the 
help of the static method `createAactorRef` of the `Spawn` class.

In the sections below we will give some examples of how to invoke different types of actors in different ways.

### Call Named Actors

To invoke an actor named like the one we defined in section [Getting Started](#getting-started) we could do as follows:

```Java
ActorRef joeActor = spawnSystem.createActorRef(ActorIdentity.of("spawn-system", "JoeActor"));
        
Request msg = Request.newBuilder()
       .setLanguage("erlang")
       .build();
        
Optional<Reply> maybeResponse = joeActor.invoke("setLanguage", msg, Reply.class);
Reply reply = maybeResponse.get();
```

More detailed in complete main class:

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.Spawn.SpawnSystem;
import io.eigr.spawn.api.ActorIdentity;
import io.eigr.spawn.api.ActorRef;
import io.eigr.spawn.api.TransportOpts;
import io.eigr.spawn.api.exceptions.SpawnException;
import io.eigr.spawn.java.demo.domain.Domain;

public class App {
   public static void main(String[] args) throws SpawnException {
      Spawn spawnSystem = new SpawnSystem()
              .create("spawn-system")
              .withActor(Joe.class)
              .withTransportOptions(
                      TransportOpts.builder()
                              .port(8091)
                              .proxyPort(9003)
                              .build()
              )
              .build();

      spawnSystem.start();

      ActorRef joeActor = spawnSystem.createActorRef(ActorIdentity.of("spawn-system", "JoeActor"));

      Request msg = Request.newBuilder()
              .setLanguage("erlang")
              .build();
     
      joeActor.invoke("setLanguage", msg, Reply.class)
              .ifPresent(response ->  log.info("Response is: {}", response));
   }
}
```

### Call Unnamed Actors

Unnamed actors are equally simple to invoke. All that is needed is to inform the `parent` parameter which refers to the 
name given to the actor that defines the ActorRef template.

To better exemplify, let's first show the Actor's definition code and later how we would call this actor with a concrete 
name at runtime:

```java
package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.StatefulActor;
import io.eigr.spawn.api.actors.Value;
import io.eigr.spawn.api.actors.behaviors.ActorBehavior;
import io.eigr.spawn.api.actors.behaviors.BehaviorCtx;
import io.eigr.spawn.api.actors.behaviors.UnNamedActorBehavior;
import io.eigr.spawn.internal.ActionBindings;
import io.eigr.spawn.java.demo.domain.Actor.Reply;
import io.eigr.spawn.java.demo.domain.Actor.Request;
import io.eigr.spawn.java.demo.domain.Actor.State;

import static io.eigr.spawn.api.actors.behaviors.ActorBehavior.*;

public final class MikeActor implements StatefulActor<State> {

   @Override
   public ActorBehavior configure(BehaviorCtx context) {
      return new UnNamedActorBehavior(
              name("MikeActor"),
              snapshot(1000),
              deactivated(60000),
              action("SetLanguage", ActionBindings.of(Request.class, this::setLanguage))
      );
   }

   private Value setLanguage(ActorContext<State> context, Request msg) {
      return Value.at()
              .response(Reply.newBuilder()
                      .setResponse(String.format("Hi %s. Hello From Java", msg.getLanguage()))
                      .build())
              .state(updateState(msg.getLanguage()), true)
              .reply();
   }

   // ...
}
```

So you could define and call this actor at runtime like this:

```Java
ActorRef mike = spawnSystem.createActorRef(ActorIdentity.of("spawn-system", "MikeInstanceActor", "MikeActor"));
        
Request msg = Request.newBuilder()
       .setLanguage("erlang")
       .build();

Optional<Reply> maybeResponse = mike.invoke("setLanguage", msg, Reply.class);
Reply reply = maybeResponse.get();
```

The important part of the code above is the following snippet:

```Java
ActorRef mike = spawnSystem.createActorRef(ActorIdentity.of("spawn-system", "MikeInstanceActor", "MikeActor"));
```

These tells Spawn that this actor will actually be named at runtime. The name parameter with value "MikeInstanceActor" 
in this case is just a reference to "MikeActor" Actor that will be used later 
so that we can actually create an instance of the real Actor.

### Async

Basically Spawn can perform actor functions in two ways. Synchronously, where the callee waits for a response, 
or asynchronously, where the callee doesn't care about the return value of the call. 
In this context we should not confuse Spawn's asynchronous way with Java's concept of async like Promises because async for Spawn is 
just a fire-and-forget call.

Therefore, to call an actor's function asynchronously, just use the invokeAsync method:

```Java
mike.invokeAsync("setLanguage", msg);
```

### Timeouts

It is possible to change the request waiting timeout using the invocation options as below:

```Java
package io.eigr.spawn.java.demo;

// omitted imports for brevity

public class App {
   public static void main(String[] args) {
      Spawn spawnSystem = new Spawn.SpawnSystem()
              .create("spawn-system")
              .withActor(Joe.class)
              .build();

      spawnSystem.start();

      ActorRef joeActor = spawnSystem.createActorRef(ActorIdentity.of("spawn-system", "JoeActor"));

      Request msg = Request.newBuilder()
              .setLanguage("erlang")
              .build();

      InvocationOpts opts = InvocationOpts.builder()
              .timeoutSeconds(Duration.ofSeconds(30))
              .build();
      
      Optional<Reply> maybeResponse = joeActor.invoke("setLanguage", msg, Reply.class, opts);
   }
}
```

## Deploy

See [Getting Started](https://github.com/eigr/spawn#getting-started) section from the main Spawn repository for more 
details on how to deploy a Spawn application.

### Defining an ActorSystem

See [Getting Started](https://github.com/eigr/spawn#getting-started) section from the main Spawn repository for more 
details on how to define an ActorSystem.

### Defining an ActorHost

See [Getting Started](https://github.com/eigr/spawn#getting-started) section from the main Spawn repository for more 
details on how to define an ActorHost.

### Activators
TODO

## Actor Model

According to Wikipedia Actor Model is:

"A mathematical model of concurrent computation that treats actor as the universal primitive of concurrent computation. 
In response to a message it receives, an actor can: make local decisions, create more actors, send more messages, 
and determine how to respond to the next message received. Actors may modify their own private state, but can only affect 
each other indirectly through messaging (removing the need for lock-based synchronization).

The actor model originated in 1973. It has been used both as a framework for a theoretical understanding of computation 
and as the theoretical basis for several practical implementations of concurrent systems."

The Actor Model was proposed by Carl Hewitt, Peter Bishop, and Richard Steiger and is inspired by several characteristics of the physical world.

Although it emerged in the 70s of the last century, only in the previous two decades of our century has this model 
gained strength in the software engineering communities due to the massive amount of existing data and the performance 
and distribution requirements of the most current applications.

For more information about the Actor Model, see the following links:

https://en.wikipedia.org/wiki/Actor_model

https://codesync.global/media/almost-actors-comparing-pony-language-to-beam-languages-erlang-elixir/

https://www.infoworld.com/article/2077999/understanding-actor-concurrency--part-1--actors-in-erlang.html

https://doc.akka.io/docs/akka/current/general/actors.html

### Virtual Actors

In the context of the Virtual Actor paradigm, actors possess the inherent ability to seamlessly retain their state. 
The underlying framework dynamically manages the allocation of actors to specific nodes. If a node happens to experience an outage, 
the framework automatically revives the affected actor on an alternate node. This process of revival maintains 
data integrity as actors are inherently designed to preserve their state. Interruptions to availability are minimized 
during this seamless transition, contingent on the actors correctly implementing their state preservation mechanisms.

The Virtual Actor model offers several merits:

* **Scalability**: The system can effortlessly accommodate a higher number of actor instances by introducing additional nodes.

* **Availability**: In case of a node failure, actors swiftly and nearly instantly regenerate on another node, 
all while safeguarding their state from loss.