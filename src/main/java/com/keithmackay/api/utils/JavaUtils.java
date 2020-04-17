package com.keithmackay.api.utils;

import org.bson.Document;

import java.util.Map;
import java.util.stream.Collectors;

public class JavaUtils {

  public static Map<String, String> toMap(final Document document) {
    return document.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            set -> String.valueOf(set.getValue())));
  }
}
