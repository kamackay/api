package domn8.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {

    private static final long BYTE_UNIT_INCREMENT = 1000;
    private static final String[] BYTE_UNITS = new String[]{"bytes", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    public static String millisToReadableTime(final long diff) {
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        StringBuilder builder = new StringBuilder();
        if (diffDays > 0) {
            builder.append(diffDays).append(" Day${conditionalPlural(diffDays)} ");
        }
        if (diffHours > 0) {
            builder.append(diffHours).append(" Hour${conditionalPlural(diffHours)} ");
        }
        if (diffMinutes > 0) {
            builder.append(diffMinutes).append(" Minute${conditionalPlural(diffMinutes)} ");
        }
        if (diffSeconds > 0) {
            builder.append(diffSeconds).append(" Second${conditionalPlural(diffSeconds)} ");
        }
        return builder.append(diff % 1000).append("ms").toString();
    }

    public static String humanizeBytes(long bytes) {
        double b = (double) bytes;
        short unit = 0;
        while (b >= BYTE_UNIT_INCREMENT && unit < BYTE_UNITS.length) {
            b /= BYTE_UNIT_INCREMENT;
            unit++;
        }
        return String.format("%.2f %s", b, BYTE_UNITS[unit]);
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }
}
