package io.eigr.spawn.internal.client;

import io.eigr.functions.protocol.Protocol;

public interface SpawnClient {

    Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws Exception;

    Protocol.SpawnResponse spawn(Protocol.SpawnRequest registration) throws Exception;
    Protocol.InvocationResponse invoke(Protocol.InvocationRequest request) throws Exception;
}

