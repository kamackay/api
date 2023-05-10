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
    public int getPercentage() {
        return (int) (((double) this.count / (double) this.total) * 100);
    }
}
