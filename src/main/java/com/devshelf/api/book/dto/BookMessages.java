package com.devshelf.api.book.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every validation message the book endpoints can return. The frontend shows these verbatim and
 * mirrors the rules, so the text is part of the API contract: change it only together with the frontend.
 */
public final class BookMessages {

    public static final String TITLE_REQUIRED = "Title is required.";
    public static final String TITLE_LENGTH = "Title must be 2 to 120 characters.";
    public static final String AUTHOR_REQUIRED = "Author is required.";
    public static final String AUTHOR_LENGTH = "Author must be 2 to 80 characters.";
    public static final String CATEGORY_REQUIRED = "Choose a category.";
    public static final String CATEGORY_UNKNOWN = "Choose a category from the list.";
    public static final String PRICE_REQUIRED = "Price is required.";
    public static final String PRICE_NOT_A_NUMBER = "Price must be a number.";
    public static final String PRICE_RANGE = "Price must be between ₹0 and ₹99,999.99.";
    public static final String PRICE_DECIMALS = "Price can have at most 2 decimal places.";
    public static final String ISBN_FORMAT = "ISBN must be exactly 13 digits.";
    public static final String ISBN_TAKEN = "Another book already uses this ISBN.";
    public static final String PUBLISHED_AT_FORMAT = "Enter the date as YYYY-MM-DD.";
    public static final String PUBLISHED_AT_FUTURE = "Published date cannot be in the future.";
    public static final String DESCRIPTION_LENGTH = "Description can be at most 1,000 characters.";
    public static final String COVER_URL_LENGTH = "Cover URL can be at most 500 characters.";
    public static final String COVER_URL_PREFIX = "Cover URL must start with http://, https:// or /assets/images/.";

    // Query-parameter messages for GET /books.
    public static final String Q_LENGTH = "Search text can be at most 100 characters.";
    public static final String SORT_UNKNOWN =
            "Sort must be one of: popular, newest, price-asc, price-desc, rating, title, updated.";
    public static final String PAGE_INVALID = "Page must be a whole number of 1 or more.";
    public static final String SIZE_INVALID = "Size must be a whole number from 1 to 50.";

    /** Request fields in the order their errors are reported. */
    public static final List<String> FIELD_ORDER = List.of(
            "title", "author", "category", "priceInr", "isbn", "publishedAt", "description", "coverUrl");

    /** Message for a JSON value of the wrong type (e.g. {@code "priceInr": "abc"}), by field. */
    private static final Map<String, String> TYPE_ERRORS = Map.of(
            "title", TITLE_LENGTH,
            "author", AUTHOR_LENGTH,
            "category", CATEGORY_UNKNOWN,
            "priceInr", PRICE_NOT_A_NUMBER,
            "isbn", ISBN_FORMAT,
            "publishedAt", PUBLISHED_AT_FORMAT,
            "description", DESCRIPTION_LENGTH,
            "coverUrl", COVER_URL_PREFIX);

    private BookMessages() {
    }

    public static Optional<String> typeError(String field) {
        return Optional.ofNullable(field).map(TYPE_ERRORS::get);
    }
}
