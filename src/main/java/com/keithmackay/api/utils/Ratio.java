package com.keithmackay.api.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Data
public class Ratio {
    private long count;
    private long total;

    public Ratio(final long count, final long total) {
        this.count = count;
        this.total = total;
    }

    @JsonProperty
    public double getPercentage() {
        return (double) this.count * 100 / (double) this.total;
    }
}
