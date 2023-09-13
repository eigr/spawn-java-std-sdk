package io.eigr.spawn.internal.transport.client;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.api.exceptions.ActorCreationException;
import io.eigr.spawn.api.exceptions.ActorInvocationException;
import io.eigr.spawn.api.exceptions.ActorRegistrationException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class OkHttpSpawnClient implements SpawnClient {
    private static final Logger log = LoggerFactory.getLogger(OkHttpSpawnClient.class);

    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    private static final String SPAWN_REGISTER_URI = "/api/v1/system";

    private static final String SPAWN_ACTOR_SPAWN = "/api/v1/system/%s/actors/spawn";

    private final String system;
    private final String proxyHost;
    private final int proxyPort;
    private final OkHttpClient client;

    public OkHttpSpawnClient(String system, String proxyHost, int proxyPort) {
        this.system = system;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(200, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(256, 100, TimeUnit.SECONDS))
                .build();
    }

    @Override
    public Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws ActorRegistrationException {
        RequestBody body = RequestBody.create(registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder().url(makeURLFrom(SPAWN_REGISTER_URI)).post(body).build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
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

    private String makeURLForSystemAndActor(String systemName, String actorName) {
        String uri = String.format("/api/v1/system/%s/actors/%s/invoke", systemName, actorName);
        return makeURLFrom(uri);
    }

    private String makeURLFrom(String uri) {
        return String.format("http://%s:%S%s", this.proxyHost, this.proxyPort, uri);
    }

    private String makeSpawnURLFrom(String systemName) {
        String uri = String.format(SPAWN_ACTOR_SPAWN, systemName);
        return makeURLFrom(uri);
    }
}
