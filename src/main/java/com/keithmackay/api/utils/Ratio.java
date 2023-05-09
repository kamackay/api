package com.keithmackay.api.utils;

@lombok.Data
public class Ratio {
    private long count;
    private long total;

    public Ratio(final long count, final long total) {
        this.count = count;
        this.total = total;
    }
}
