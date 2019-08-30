package com.keithmackay.api.utils;

import com.keithmackay.api.benchmark.Benchmark;
import org.bson.Document;

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

  @Benchmark(limit = 1)
  public static Document cloneDoc(final Document doc) {
    if (doc == null) return null;
    return Document.parse(doc.toJson());
  }

  public static Document cleanDoc(final Document doc) {
    if (doc == null) return null;
    final Document d = cloneDoc(doc);
    d.remove("_id");
    return d;
  }

  // Just a shorthand for Elective.ofNullable
  public static <T> Elective<T> optional(final T object) {
    return Elective.ofNullable(object);
  }

  /**
   * Merge 2 Documents. Values in d2 are the priority value
   *
   * @param d1 - First Document to overwrite
   * @param d2 - Second Document with priority values
   * @return Merged Documents
   */
  public static Document merge(final Document d1, final Document d2) {
    d2.keySet().forEach(s -> d1.append(s, d2.get(s)));
    return d1;
  }

  public static Document doc() {
    return new Document();
  }
}
