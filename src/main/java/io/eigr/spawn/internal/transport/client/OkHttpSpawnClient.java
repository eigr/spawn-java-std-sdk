package io.eigr.spawn.internal.transport.client;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.TransportOpts;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.api.exceptions.ActorRegistrationException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public final class OkHttpSpawnClient implements SpawnClient {
    private static final Logger log = LoggerFactory.getLogger(OkHttpSpawnClient.class);

    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    private static final String SPAWN_REGISTER_URI = "/api/v1/system";

    private static final String SPAWN_ACTOR_SPAWN = "/api/v1/system/%s/actors/spawn";

    private final Executor executor;

    private final String system;

    private final TransportOpts opts;
    private final OkHttpClient client;

    public OkHttpSpawnClient(String system, TransportOpts opts) {
        this.system = system;
        this.opts = opts;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .callTimeout(400, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(256, 100, TimeUnit.SECONDS))
                .build();
        this.executor =  opts.getExecutor();
    }

    @Override
    public Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws ActorRegistrationException {
        RequestBody body = RequestBody.create(registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder().url(makeURLFrom(SPAWN_REGISTER_URI)).post(body).build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            log.debug("Decode response from {}", response.body().bytes());
            return Protocol.RegistrationResponse.parseFrom(
                    Objects.requireNonNull(response.body()
                    ).bytes());
        } catch (Exception e) {
            throw new ActorRegistrationException("Error registering Actors", e);
        }
    }

    @Override
    public Protocol.SpawnResponse spawn(Protocol.SpawnRequest registration) throws ActorCreationException {
        RequestBody body = RequestBody.create(registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder()
                .url(makeSpawnURLFrom(registration.getActors(0).getSystem()))
                .post(body).build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
            return io.eigr.functions.protocol.Protocol.SpawnResponse.parseFrom(
                    Objects.requireNonNull(response.body()
                    ).bytes());
        } catch (Exception e) {
            throw new ActorCreationException("Error registering Actors", e);
        }
    }

    @Override
    public Protocol.InvocationResponse invoke(Protocol.InvocationRequest request) throws ActorInvocationException {
        RequestBody body = RequestBody.create(
                request.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request invocationRequest = new Request.Builder()
                .url(makeURLForSystemAndActor(request.getSystem().getName(), request.getActor().getId().getName()))
                .post(body)
                .build();

        Call invocationCall = client.newCall(invocationRequest);
        try (Response callInvocationResponse = invocationCall.execute()){
            assert callInvocationResponse.body() != null;
            return Protocol.InvocationResponse
                    .parseFrom(Objects.requireNonNull(callInvocationResponse.body()).bytes());
        } catch (Exception e) {
            throw new ActorInvocationException(e);
        }
    }

    @Override
    public void invokeAsync(Protocol.InvocationRequest request) {
        executor.execute(() -> {
            RequestBody body = RequestBody.create(
                    request.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

            Request invocationRequest = new Request.Builder()
                    .url(makeURLForSystemAndActor(request.getSystem().getName(), request.getActor().getId().getName()))
                    .post(body)
                    .build();

            Call invocationCall = client.newCall(invocationRequest);
            invocationCall.enqueue(new Callback() {
                @Override
                public void onFailure(final Call call, IOException err) {
                    log.warn("Error while Actor invoke async.", err);
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    String res = response.body().string();
                    log.trace("Actor invoke async response [{}].", res);
                }
            });
        });
    }

    private String makeURLForSystemAndActor(String systemName, String actorName) {
        String uri = String.format("/api/v1/system/%s/actors/%s/invoke", systemName, actorName);
        return makeURLFrom(uri);
    }

    private String makeURLFrom(String uri) {
        return String.format("http://%s:%S%s", this.opts.getProxyHost(), this.opts.getProxyPort(), uri);
    }

    private String makeSpawnURLFrom(String systemName) {
        String uri = String.format(SPAWN_ACTOR_SPAWN, systemName);
        return makeURLFrom(uri);
    }
}
