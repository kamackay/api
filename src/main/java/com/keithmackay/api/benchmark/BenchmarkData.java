package com.keithmackay.api.benchmark;

import lombok.*;

/**
 * Class used by the BenchmarkTimer to track timers
 */
@Data
@Builder(toBuilder = true, builderClassName = "Builder", access = AccessLevel.PUBLIC)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class BenchmarkData {
    private long limit;
    private String name;
    private boolean logAbsolute;
    private long startTime;

    public static BenchmarkData data(String name, long limit) {
        return BenchmarkData.builder()
                .name(name)
                .limit(limit)
                .startTime(System.currentTimeMillis())
                .logAbsolute(false)
                .build();
    }
}
