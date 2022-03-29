package com.keithmackay.api.utils;

import com.google.gson.JsonElement;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaUtils {

    public static final String JSON_BREAK_PATTERN = "(?<!/)\\.";

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

    public static Optional<String> getDeepString(final JsonElement element,
                                                 final String path) {
        return getDeep(element, path).mapOut(JsonElement::getAsString);
    }

    public static Optional<Double> getDeepDouble(final JsonElement element,
                                                 final String path) {
        return getDeep(element, path).mapOut(JsonElement::getAsDouble);
    }

    public static JsonOptional<JsonElement> getDeep(final JsonElement element,
                                                    final String path) {
        final AtomicReference<JsonOptional<JsonElement>> maybeEl = new AtomicReference<>(JsonOptional.ofNullable(element));

        Arrays.stream(path.split(JSON_BREAK_PATTERN))
                .map(String::trim)
                .filter(not(String::isEmpty))
                .forEach(subPath -> {
                    maybeEl.getAndUpdate(optEl -> {
                        return optEl.map(JsonElement::getAsJsonObject)
                                .map(el -> el.get(subPath));
                    });
                });
        return maybeEl.get();
    }

    /**
     * Returns a predicate that is the negation of the supplied predicate.
     * This is accomplished by returning result of the calling
     * {@code target.negate()}.
     *
     * @param <T>    the type of arguments to the specified predicate
     * @param target predicate to negate
     * @return a predicate that negates the results of the supplied
     * predicate
     * @throws NullPointerException if target is null
     * @since 11
     */
    @SuppressWarnings("unchecked")
    static <T> Predicate<T> not(Predicate<? super T> target) {
        Objects.requireNonNull(target);
        return (Predicate<T>) target.negate();
    }

    public static class Time {
        public static long seconds(final int seconds) {
            return seconds * 1000;
        }

        public static long minutes(final int minutes) {
            return seconds(minutes) * 60;
        }

        public static long hours(final int hours) {
            return minutes(hours) * 60;
        }

        public static long days(final int days) {
            return hours(days) * 24;
        }
    }

}
