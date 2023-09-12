package io.eigr.spawn.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvocationOpts {

    @Builder.Default
    private boolean async = false;

    @Builder.Default
    private Duration timeoutSeconds = Duration.ofSeconds(10);

    @Builder.Default
    private Optional<Long> delaySeconds = Optional.empty();

    @Builder.Default
    private Optional<LocalDateTime> scheduledTo = Optional.empty();

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
}
