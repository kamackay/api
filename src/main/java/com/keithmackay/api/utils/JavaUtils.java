package com.keithmackay.api.utils;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaUtils {

  public static Map<String, String> toMap(final Document document) {
    return document.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            set -> String.valueOf(set.getValue())));
  }

  public static <T> List<T> iterableToList(final Iterable<T> iter) {
    final List<T> list = new ArrayList<>();
    for (T t : iter) {
      list.add(t);
    }
    return list;
  }

  public static <T> Stream<T> streamIterable(final Iterable<T> iter) {
    return iterableToList(iter).stream();
  }

  public static <O> Function<String, O> provide(final Function<String, O> lambda, final String val) {
    return a -> lambda.apply(val);
  }
}
