package com.devshelf.api.book;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Turns a title into a URL-safe book id, e.g. {@code "Docker & Kubernetes in Practice"} becomes
 * {@code "docker-kubernetes-in-practice"}.
 */
public final class SlugGenerator {

    public static final int MAX_LENGTH = 120;

    /** Used when a title has no ASCII letters or digits at all (e.g. a title written only in Hangul). */
    public static final String FALLBACK = "book";

    private SlugGenerator() {
    }

    /**
     * Lower-cases the title, folds accents ({@code é} becomes {@code e}), replaces every run of
     * characters outside {@code [a-z0-9]} with one hyphen, trims hyphens from both ends and cuts the
     * result to {@value #MAX_LENGTH} characters.
     */
    public static String slugify(String title) {
        String folded = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        String slug = folded.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? FALLBACK : slug;
    }

    /** Returns {@code base} if it is free, otherwise the first free one of {@code base-2}, {@code base-3}, ... */
    public static String firstFree(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        for (int n = 2; ; n++) {
            String candidate = base + "-" + n;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }
}
