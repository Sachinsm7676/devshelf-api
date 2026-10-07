package com.devshelf.api.book.dto;

import com.devshelf.api.book.Book;
import com.devshelf.api.book.BookCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A book as the API returns it. Every key is always present; missing values are {@code null}. */
public record BookResponse(
        String id,
        String title,
        String author,
        BookCategory category,
        BigDecimal priceInr,
        String isbn,
        LocalDate publishedAt,
        String description,
        String coverUrl,
        BigDecimal rating,
        int ratingCount,
        int popularity,
        Instant createdAt,
        Instant updatedAt) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getPriceInr(),
                book.getIsbn(),
                book.getPublishedAt(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getRating(),
                book.getRatingCount(),
                book.getPopularity(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
