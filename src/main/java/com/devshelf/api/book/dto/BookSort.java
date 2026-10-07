package com.devshelf.api.book.dto;

import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

/**
 * The orderings offered by {@code GET /books?sort=}. Every ordering ends with {@code id} ascending so
 * paging is stable even when the listed keys tie.
 */
public enum BookSort {

    POPULAR("popular", Sort.by(desc("popularity"), asc("title"))),
    /** Books without a published date come last: see hibernate.order_by.default_null_ordering. */
    NEWEST("newest", Sort.by(desc("publishedAt"), desc("createdAt"))),
    PRICE_ASC("price-asc", Sort.by(asc("priceInr"), asc("title"))),
    PRICE_DESC("price-desc", Sort.by(desc("priceInr"), asc("title"))),
    RATING("rating", Sort.by(desc("rating"), desc("ratingCount"), asc("title"))),
    TITLE("title", Sort.by(asc("title"))),
    UPDATED("updated", Sort.by(desc("updatedAt")));

    public static final BookSort DEFAULT = POPULAR;

    private final String param;
    private final Sort sort;

    BookSort(String param, Sort sort) {
        this.param = param;
        this.sort = sort.and(Sort.by(asc("id")));
    }

    public String param() {
        return param;
    }

    public Sort toSort() {
        return sort;
    }

    public static Optional<BookSort> fromParam(String value) {
        return Arrays.stream(values()).filter(s -> s.param.equals(value)).findFirst();
    }
}
