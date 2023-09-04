# Spawn JVM SDK
JVM User Language Support for [Spawn](https://github.com/eigr/spawn).

# Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Advanced Use Cases](#advanced-use-cases)
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
    - [Async and other options](#async-calls-and-other-options)
5. [Deploy](#deploy)
    - [Defining an ActorSystem](#defining-an-actorsytem)
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

First we must create a new Java project. In this example we will use [Maven](https://maven.apache.org/) as our package manager.

```shell
mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4
```
Now you will need to fill in the data for groupId, artifactId, version, and package. 
Let's call our maven artifact spawn-java-demo. The output of this command will be similar to the output below
```shell
$ mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4
[INFO] Scanning for projects...
[INFO] Generating project in Interactive mode
[INFO] Archetype repository not defined. Using the one from [org.apache.maven.archetypes:maven-archetype-quickstart:1.4] found in catalog remote
Define value for property 'groupId': io.eigr.spawn
Define value for property 'artifactId': spawn-java-demo
Define value for property 'version' 1.0-SNAPSHOT: : 
Define value for property 'package' io.eigr.spawn: : io.eigr.spawn.java.demo 
Confirm properties configuration:
groupId: io.eigr.spawn
artifactId: spawn-java-demo
version: 1.0-SNAPSHOT
package: io.eigr.spawn.java.demo
 Y: : y
[INFO] ----------------------------------------------------------------------------
[INFO] Using following parameters for creating project from Archetype: maven-archetype-quickstart:1.4
[INFO] ----------------------------------------------------------------------------
[INFO] Parameter: groupId, Value: io.eigr.spawn
[INFO] Parameter: artifactId, Value: spawn-java-demo
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Parameter: package, Value: io.eigr.spawn.java.demo
[INFO] Parameter: packageInPathFormat, Value: io/eigr/spawn/java/demo
[INFO] Parameter: package, Value: io.eigr.spawn.java.demo
[INFO] Parameter: groupId, Value: io.eigr.spawn
[INFO] Parameter: artifactId, Value: spawn-java-demo
[INFO] Parameter: version, Value: 1.0-SNAPSHOT
[INFO] Project created from Archetype in dir: /home/sleipnir/workspaces/eigr/spawn-java-demo
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:39 min
[INFO] Finished at: 2023-08-28T11:37:57-03:00
[INFO] ------------------------------------------------------------------------
```

The second thing we have to do is add the spawn dependency to the project.

```xml
<dependency>
   <groupId>com.github.eigr</groupId>
   <artifactId>spawn-java-std-sdk</artifactId>
   <version>v0.5.0</version>
</dependency>
```
We're also going to configure a few things for our application build to work, including compiling the protobuf files. 
See below a full example of the pom.xml file:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>io.eigr.spawn</groupId>
   <artifactId>spawn-java-demo</artifactId>
   <packaging>jar</packaging>
   <version>1.0-SNAPSHOT</version>
   <name>spawn-java-demo</name>
   <url>https://eigr.io</url>

   <properties>
      <maven.compiler.source>11</maven.compiler.source>
      <maven.compiler.target>11</maven.compiler.target>
      <project.encoding>UTF-8</project.encoding>
   </properties>

   <repositories>
      <repository>
         <id>jitpack.io</id>
         <url>https://jitpack.io</url>
      </repository>
   </repositories>

   <dependencies>
      <dependency>
         <groupId>com.github.eigr</groupId>
         <artifactId>spawn-java-std-sdk</artifactId>
         <version>v0.5.0</version>
      </dependency>
      <dependency>
         <groupId>ch.qos.logback</groupId>
         <artifactId>logback-classic</artifactId>
         <version>1.4.7</version>
      </dependency>
      <dependency>
         <groupId>junit</groupId>
         <artifactId>junit</artifactId>
         <version>4.13.2</version>
         <scope>test</scope>
      </dependency>
   </dependencies>

   <build>
      <extensions>
         <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.6.2</version>
         </extension>
      </extensions>
      <!-- make jar runnable -->
      <plugins>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <executions>
               <execution>
                  <goals>
                     <goal>shade</goal>
                  </goals>
                  <configuration>
                     <shadedArtifactAttached>true</shadedArtifactAttached>
                     <transformers>
                        <transformer implementation=
                                             "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                           <mainClass>io.eigr.spawn.java.demo.App</mainClass>
                        </transformer>
                     </transformers>
                  </configuration>
               </execution>
            </executions>
         </plugin>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.7</version>
         </plugin>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
               <source>11</source>
               <target>11</target>
            </configuration>
         </plugin>
         <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
               <protocArtifact>com.google.protobuf:protoc:3.19.2:exe:${os.detected.classifier}</protocArtifact>
               <pluginId>grpc-java</pluginId>
               <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.47.0:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
               <execution>
                  <goals>
                     <goal>compile</goal>
                     <goal>compile-custom</goal>
                  </goals>
               </execution>
            </executions>
         </plugin>
         <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <version>3.2.0</version>
            <executions>
               <execution>
                  <id>add-test-sources</id>
                  <phase>generate-test-sources</phase>
                  <goals>
                     <goal>add-test-source</goal>
                  </goals>
                  <configuration>
                     <sources>
                        <source>${project.build.directory}/generated-test-sources/protobuf</source>
                     </sources>
                  </configuration>
               </execution>
            </executions>
         </plugin>
      </plugins>
   </build>
</project>
```

Now it is necessary to download the dependencies via Maven:

```shell
cd spawn-java-demo && mvn install
```

So far it's all pretty boring and not really Spawn related, so it's time to start playing for real.
The first thing we're going to do is define a place to put our protobuf files. In the root of the project we will create 
a folder called protobuf and some sub folders

```shell
mkdir -p src/main/proto/domain
```

That done, let's create our protobuf file inside the example folder.

```shell
touch src/main/proto/domain/domain.proto
```

And let's populate this file with the following content:

```protobuf
syntax = "proto3";

package domain;

option java_package = "io.eigr.spawn.java.demo.domain";

message JoeState {
  repeated string languages = 1;
}

message Request {
  string language = 1;
}

message Reply {
  string response = 1;
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

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.java.demo.domain.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StatefulNamedActor(name = "joe", stateType = Domain.JoeState.class)
public class Joe {
   private static final Logger log = LoggerFactory.getLogger(Joe.class);

   @Action(name = "hi", inputType = Domain.Request.class)
   public Value hi(Domain.Request msg, ActorContext<Domain.JoeState> context) {
      log.info("Received invocation. Message: {}. Context: {}", msg, context);
      if (context.getState().isPresent()) {
         log.info("State is present and value is {}", context.getState().get());
      }

      return Value.at()
              .response(Domain.Reply.newBuilder()
                      .setResponse("Hello From Java")
                      .build())
              .state(updateState("erlang"))
              .reply();
   }

   private Domain.JoeState updateState(String language) {
      return Domain.JoeState.newBuilder()
              .addLanguages(language)
              .build();
   }
}
```

Now with our Actor properly defined, we just need to start the SDK correctly. Create another file called App.java 
to serve as your application's entrypoint and fill it with the following content:

```Java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;

public class App {
   public static void main(String[] args) throws Exception {
      Spawn spawnSystem = new SpawnSystem()
              .create("spawn-system")
              .withPort(8091)
              .withProxyPort(9003)
              .addActor(Joe.class)
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

You may also want your Actors to be initialized with some dependent objects similarly to how you would use the 
dependency injection pattern. 
In this case, it is enough to declare a constructor that receives a single argument for its actor.

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.java.demo.domain.Domain;

import java.util.Map;

@StatefulNamedActor(name = "joe", stateful = true, stateType = Domain.JoeState.class, channel = "test")
public final class Joe {
    private final String someValue;

    public Joe(Map<String, String> args) {
        this.someValue = args.get("someKey");
    }

    @Action(inputType = Domain.Request.class)
    public Value setLanguage(Domain.Request msg, ActorContext<Domain.JoeState> context) {
        return Value.at()
                .response(Domain.Reply.newBuilder()
                        .setResponse("Hello From Java")
                        .build())
                .state(updateState("java"))
                .reply();
    }

    // ...
}
```

Then you also need to register your Actor using the `addActorWithArgs` method like as follows: 

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.actors.ActorRef;

import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Map<String, String> actorConstructorArgs = new HashMap<>();
        actorConstructorArgs.put("someKey", "someValue");
        
        Spawn spawnSystem = new Spawn.SpawnSystem()
                .create("spawn-system")
                .withPort(8091)
                .withProxyPort(9003)
                .addActorWithArgs(Joe.class, actorConstructorArgs, arg -> new Joe((Map<String, String>) arg))
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

## Advanced Use Cases

Spawn Actors abstract a huge amount of developer infrastructure and can be used for many types of jobs. 
In the sections below we will demonstrate some features available in Spawn that contribute to the development of 
complex applications in a simplified way.

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
For this the developer just needs to make use of the correct annotation. For example, I could declare a Serverless Actor using the following code:

```java
package io.eigr.spawn.test.actors;

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateless.StatelessNamedActor;
import io.eigr.spawn.java.test.domain.Actor;

@StatelessNamedActor(name = "test_joe")
public class JoeActor {
    @Action
    public Value hi(Actor.Request msg, ActorContext<?> context) {
        return Value.at()
                .response(Actor.Reply.newBuilder()
                        .setResponse("Hello From Java")
                        .build())
                .reply();
    }
}
```

Other than that the same Named, UnNamed types are supported. Just use the StatelessNamed or StatelessUnNamed annotations.

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

To consume from a topic, you just need to configure the Actor annotation using the channel option as follows:

```Java
@StatefulNamedActor(name = "joe", stateful = true, stateType = Domain.JoeState.class, channel = "test")
```
In the case above, the Actor `joe` was configured to receive events that are forwarded to the topic called `test`.

To produce events in a topic, just use the Broadcast Workflow. The example below demonstrates a complete example of 
producing and consuming events. In this case, the same actor is the event consumer and producer, but in a more realistic scenario, 
different actors would be involved in these processes.

```Java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.actors.workflows.Broadcast;
// some imports omitted for brevity

@StatefulNamedActor(name = "joe", stateType = Domain.JoeState.class, channel = "test")
public class Joe {
   @TimerAction(name = "hi", period = 60000)
   public Value hi(ActorContext<Domain.JoeState> context) {
      Domain.Request msg = Domain.Request.newBuilder()
              .setLanguage("erlang")
              .build();

      return Value.at()
              .flow(Broadcast.to("test", "setLanguage", msg))
              .response(Domain.Reply.newBuilder()
                      .setResponse("Hello From Erlang")
                      .build())
              .state(updateState("erlang"))
              .reply();
   }

   @Action(inputType = Domain.Request.class)
   public Value setLanguage(Domain.Request msg, ActorContext<Domain.JoeState> context) {
      return Value.at()
              .response(Domain.Reply.newBuilder()
                      .setResponse("Hello From Java")
                      .build())
              .state(updateState("java"))
              .reply();
   }
   // ....
}
```

### Side Effects

Actors can also emit side effects to other Actors as part of their response.
See an example:

```Java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.api.actors.workflows.SideEffect;
import io.eigr.spawn.java.demo.domain.Domain;

@StatefulNamedActor(name = "side_effect_actor", stateType = Domain.State.class)
public class SideEffectActorExample {
    @Action
    public Value setLanguage(Domain.Request msg, ActorContext<Domain.State> ctx) throws Exception {
        // Create a ActorReference to send side effect message
        ActorRef sideEffectReceiverActor = ctx.getSpawnSystem()
                .createActorRef("spawn-system", "mike", "abs_actor");

        return Value.at()
                .response(Domain.Reply.newBuilder()
                        .setResponse("Hello From Java")
                        .build())
                .state(updateState("java"))
                .flow(SideEffect.to(sideEffectReceiverActor, "setLanguage", msg))
                //.flow(SideEffect.to(emailSenderReceiverActor, "sendEmail", emailMessage))
                //.flow(SideEffect.to(otherReceiverActor, "otherAction", otherMessage))
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
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.api.actors.workflows.Forward;
import io.eigr.spawn.java.demo.domain.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StatefulNamedActor(name = "routing_actor", stateType = Domain.State.class)
public class ForwardExample {
   private static final Logger log = LoggerFactory.getLogger(ForwardExample.class);

   @Action
   public Value setLanguage(Domain.Request msg, ActorContext<Domain.State> ctx) throws Exception {
      log.info("Received invocation. Message: {}. Context: {}", msg, ctx);
      if (ctx.getState().isPresent()) {
         log.info("State is present and value is {}", ctx.getState().get());
      }
      ActorRef forwardedActor = ctx.getSpawnSystem()
              .createActorRef("spawn-system", "mike", "abs_actor");

      return Value.at()
              .flow(Forward.to(forwardedActor,"setLanguage"))
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
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.api.actors.workflows.Pipe;
import io.eigr.spawn.java.demo.domain.Domain;

@StatefulNamedActor(name = "pipe_actor", stateType = Domain.State.class)
public class PipeActorExample {

    @Action
    public Value setLanguage(Domain.Request msg, ActorContext<Domain.State> ctx) throws Exception {
        ActorRef pipeReceiverActor = ctx.getSpawnSystem()
                .createActorRef("spawn-system", "joe");

        return Value.at()
                .response(Domain.Reply.newBuilder()
                        .setResponse("Hello From Java")
                        .build())
                .flow(Pipe.to(pipeReceiverActor, "someAction"))
                .state(updateState("java"))
                .noReply();
    }

    private Domain.State updateState(String language) {
        return Domain.State.newBuilder()
                .addLanguages(language)
                .build();
    }
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

These snapshots happen from time to time. And this time is configurable through the ***snapshotTimeout*** property of 
the ***StatefulNamedActor*** or ***UnStatefulNamedActor*** annotation. 
However, you can tell the Spawn runtime that you want it to persist the data immediately synchronously after executing an Action.
And this can be done in the following way:

Example:

```Java
import io.eigr.spawn.api.Value;
import io.eigr.spawn.api.actors.ActorContext;
import io.eigr.spawn.api.actors.annotations.Action;
import io.eigr.spawn.api.actors.annotations.stateful.StatefulNamedActor;
import io.eigr.spawn.java.demo.domain.Domain;

@StatefulNamedActor(name = "joe", stateType = Domain.JoeState.class)
public final class Joe {
    @Action(inputType = Domain.Request.class)
    public Value setLanguage(Domain.Request msg, ActorContext<Domain.JoeState> context) {
        return Value.at()
                .response(Domain.Reply.newBuilder()
                        .setResponse("Hello From Java")
                        .build())
                .state(updateState("java"), true)
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
ActorRef joeActor = spawnSystem.createActorRef("spawn-system", "joe");
        
        Domain.Request msg = Domain.Request.newBuilder()
                .setLanguage("erlang")
                .build();
        Domain.Reply reply = 
                (Domain.Reply) joeActor.invoke("setLanguage", msg, Domain.Reply.class);
```

More detailed in complete main class:

```java
package io.eigr.spawn.java.demo;

import io.eigr.spawn.api.Spawn;
import io.eigr.spawn.api.Spawn.SpawnSystem;
import io.eigr.spawn.api.actors.ActorRef;
import io.eigr.spawn.java.demo.domain.Domain;

import java.util.Optional;

public class App {
   public static void main(String[] args) throws Exception {
      Spawn spawnSystem = new SpawnSystem()
              .create("spawn-system")
              .withPort(8091)
              .withProxyPort(9003)
              .withActor(Joe.class)
              .build();

      spawnSystem.start();

      ActorRef joeActor = spawnSystem.createActorRef("spawn-system", "joe");

      Domain.Request msg = Domain.Request.newBuilder()
              .setLanguage("erlang")
              .build();
      Domain.Reply reply =
              (Domain.Reply) joeActor.invoke("setLanguage", msg, Domain.Reply.class);
   }
}
```

### Call Unnamed Actors

Unnamed actors are equally simple to invoke. All that is needed is to inform the `parent` parameter which refers to the 
name given to the actor that defines the ActorRef template.

To better exemplify, let's first show the Actor's definition code and later how we would call this actor with a concrete 
name at runtime:

```java
package io.eigr.spawn.java.demo;
// omitted imports for brevity...

@UnStatefulNamedActor(name = "abs_actor", stateful = true, stateType = Domain.State.class)
public class AbstractActor {
    @Action(inputType = Domain.Request.class)
    public Value setLanguage(Domain.Request msg, ActorContext<Domain.State> context) {
        return Value.at()
                .response(Domain.Reply.newBuilder()
                        .setResponse("Hello From Java")
                        .build())
                .state(updateState("java"))
                .reply();
    }

    private Domain.State updateState(String language) {
        return Domain.State.newBuilder()
                .addLanguages(language)
                .build();
    }
}
```

So you could define and call this actor at runtime like this:

```Java
ActorRef mike = spawnSystem.createActorRef("spawn-system", "mike", "abs_actor");
        
        Domain.Request msg = Domain.Request.newBuilder()
                .setLanguage("erlang")
                .build();
        Domain.Reply reply = 
                (Domain.Reply) mike.invoke("setLanguage", msg, Domain.Reply.class);
```

The important part of the code above is the following snippet:

```Java
ActorRef mike = spawnSystem.createActorRef("spawn-system", "mike", "abs_actor");
```

These tells Spawn that this actor will actually be named at runtime. The name parameter with value "mike" 
in this case is just a reference to "abs_actor" Actor that will be used later 
so that we can actually create an instance of the real Actor.

### Async calls and other options

Basically Spawn can perform actor functions in two ways. Synchronously, where the callee waits for a response, 
or asynchronously, where the callee doesn't care about the return value of the call. 
In this context we should not confuse Spawn's asynchronous way with Java's concept of async like Promises because async for Spawn is 
just a fire-and-forget call.

Therefore, to call an actor's function asynchronously, just use the invokeAsync method:

```Java
mike.invokeAsync("setLanguage", msg, Domain.Reply.class);
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