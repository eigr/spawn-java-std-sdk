package io.eigr.spawn.api;

import lombok.*;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransportOpts {

    @Builder.Default
    private String host = "127.0.0.1";
    @Builder.Default
    private int port = 8091;
    @Builder.Default
    private String proxyHost = "127.0.0.1";
    @Builder.Default
    private int proxyPort = 9001;
    @Builder.Default
    private Executor executor = Executors.newCachedThreadPool();
}
