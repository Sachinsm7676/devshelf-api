package com.devshelf.api.book.dto;

import java.util.List;

/**
 * One page of results. {@code pageNumber} is 1-based and is the page actually returned, which may be
 * lower than the one requested (requests past the end are clamped to the last page).
 */
public record PageResponse<T>(List<T> list, int pageNumber, int size, long totalElements, int totalPages) {
}
