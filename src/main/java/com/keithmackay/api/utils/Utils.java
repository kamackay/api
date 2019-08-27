package com.keithmackay.api.utils;

public class Utils {

    private final static long DAY = 86400000;
    private final static long HOUR = 3600000;
    private final static long MINUTE = 60000;
    private final static long SECOND = 1000;

    public static String millisToReadableTime(long millis) {
        final StringBuilder maker = new StringBuilder();
        millis = readableTimeHelper(millis, DAY, "days", maker);
        millis = readableTimeHelper(millis, HOUR, "hours", maker);
        millis = readableTimeHelper(millis, MINUTE, "minutes", maker);
        millis = readableTimeHelper(millis, SECOND, "seconds", maker);
        readableTimeHelper(millis, 1, "ms", maker);
        if (maker.length() == 0) return "0 ms";
        return maker.toString().trim();
    }

    private static long readableTimeHelper(long time, long unit, String unitName, StringBuilder builder) {
        if (time >= unit) {
            builder.append(String.format("%d %s ", time / unit, unitName));
            return time % unit;
        } else return time;
    }
}
