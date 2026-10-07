package com.devshelf.api.book;

import com.devshelf.api.book.dto.BookMessages;
import com.devshelf.api.book.dto.BookRequest;
import com.devshelf.api.book.dto.BookResponse;
import com.devshelf.api.book.dto.BookSearchQuery;
import com.devshelf.api.book.dto.PageResponse;
import com.devshelf.api.common.ApiError;
import com.devshelf.api.common.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search, filter, sort and page the catalogue")
    public PageResponse<BookResponse> list(
            @Parameter(description = "Matches title, author or ISBN (case-insensitive, max 100 chars)")
            @RequestParam(required = false) String q,
            @Parameter(description = "A category label such as \"System Design\", or \"All\"")
            @RequestParam(required = false) String category,
            @Parameter(schema = @Schema(allowableValues = {
                    "popular", "newest", "price-asc", "price-desc", "rating", "title", "updated"},
                    defaultValue = "popular"))
            @RequestParam(required = false) String sort,
            @Parameter(description = "1-based; clamped to the last page",
                    schema = @Schema(type = "integer", minimum = "1", defaultValue = "1"))
            @RequestParam(required = false) String page,
            @Parameter(schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "8"))
            @RequestParam(required = false) String size) {
        return service.search(BookSearchQuery.parse(q, category, sort, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one book")
    public BookResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a book; its id is a slug of the title")
    public ResponseEntity<BookResponse> create(@RequestBody BookRequest request) {
        BookResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/books/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the editable fields of a book")
    public BookResponse update(@PathVariable String id, @RequestBody BookRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a book")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /**
     * A body value of the wrong JSON type (e.g. {@code "priceInr": "abc"}) is reported against its
     * field, like any other validation error. Parsing stops at the first such value, so it is the only
     * field reported. Anything else unreadable gets the generic BAD_REQUEST.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex) {
        // Only a well-formed value of the wrong type is a field error. A syntax error can arrive wrapped
        // in a plain JsonMappingException that still carries a path, and must stay a generic BAD_REQUEST.
        if (ex.getCause() instanceof JsonMappingException mapping
                && (mapping instanceof MismatchedInputException || mapping instanceof ValueInstantiationException)
                && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().get(0).getFieldName();
            var message = BookMessages.typeError(field);
            if (message.isPresent()) {
                return ApiError.validationFailed(Map.of(field, message.get()));
            }
        }
        return ApiError.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", GlobalExceptionHandler.UNREADABLE_MESSAGE);
    }
}
