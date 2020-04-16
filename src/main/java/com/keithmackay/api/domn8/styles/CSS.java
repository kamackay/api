package com.keithmackay.api.domn8.styles;

import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;

public class CSS {
  private final Map<String, Object> values = newHashMap();

  private CSS() {

  }

  public static CSS css() {
    return new CSS();
  }

  public CSS setValue(final String name, final Object value) {
    this.values.put(name, value);
    return this;
  }

  public CSS merge(final CSS otherStyles) {
    this.values.putAll(otherStyles.values);
    return this;
  }

  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  public String print() {
    return this.values
        .entrySet()
        .stream()
        .map(entry -> String.format("%s: %s", entry.getKey(),
            String.valueOf(entry.getValue())))
        .collect(Collectors.joining("; "));
  }
}
