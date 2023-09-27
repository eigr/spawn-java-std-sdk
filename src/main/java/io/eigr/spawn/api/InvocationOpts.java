package io.eigr.spawn.api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class InvocationOpts {

    private final boolean async;
    private final Duration timeoutSeconds;
    private final Optional<Long> delaySeconds;
    private final Optional<LocalDateTime> scheduledTo;

    private InvocationOpts(InvocationOptsBuilder invocationOptsBuilder) {
        this.async = invocationOptsBuilder.async;
        this.timeoutSeconds = invocationOptsBuilder.timeoutSeconds;
        this.delaySeconds = invocationOptsBuilder.delaySeconds;
        this.scheduledTo = invocationOptsBuilder.scheduledTo;
    }

    public static InvocationOptsBuilder builder() {
        return new InvocationOptsBuilder();
    }

    public boolean isAsync() {
        return async;
    }

    public Duration getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public Optional<Long> getDelaySeconds() {
        return delaySeconds;
    }

    public Optional<LocalDateTime> getScheduledTo() {
        return scheduledTo;
    }

    public long getScheduleTimeInLong() {
        if (scheduledTo.isPresent()) {
            LocalDateTime ldt = scheduledTo.get();
            return ChronoUnit.MILLIS.between(LocalDateTime.now(), ldt);
        }

        return 0;
    }

    public long getTimeout() {
        return this.timeoutSeconds.toMillis();
    }

    public static final class InvocationOptsBuilder {

        private boolean async = false;
        private Duration timeoutSeconds = Duration.ofSeconds(10);
        private Optional<Long> delaySeconds = Optional.empty();
        private Optional<LocalDateTime> scheduledTo = Optional.empty();

        public InvocationOpts build() {
            return new InvocationOpts(this);
        }

        public InvocationOptsBuilder async(boolean async) {
            this.async = async;
            return this;
        }

        public InvocationOptsBuilder timeoutSeconds(Duration timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public InvocationOptsBuilder delaySeconds(Optional<Long> delaySeconds) {
            this.delaySeconds = delaySeconds;
            return this;
        }

        public InvocationOptsBuilder scheduledTo(Optional<LocalDateTime> scheduledTo) {
            this.scheduledTo = scheduledTo;
            return this;
        }
    }
}
