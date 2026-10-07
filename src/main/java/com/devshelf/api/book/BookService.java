package com.devshelf.api.book;

import com.devshelf.api.book.dto.BookMessages;
import com.devshelf.api.book.dto.BookRequest;
import com.devshelf.api.book.dto.BookResponse;
import com.devshelf.api.book.dto.BookSearchQuery;
import com.devshelf.api.book.dto.PageResponse;
import com.devshelf.api.common.ApiError;
import com.devshelf.api.common.ConflictException;
import com.devshelf.api.common.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BookService {

    /** Name of the unique constraint on {@code book.isbn} (see V1__create_book.sql). */
    static final String ISBN_CONSTRAINT = "uk_book_isbn";

    private final BookRepository repository;
    private final BookRequestValidator validator;
    private final Clock clock;

    public BookService(BookRepository repository, BookRequestValidator validator, Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Filters, sorts and pages the catalogue in the database. A page past the end is clamped to the
     * last page; an empty result is reported as page 1 of 1.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(BookSearchQuery query) {
        Collection<BookCategory> categories = query.category() == null
                ? EnumSet.allOf(BookCategory.class)
                : EnumSet.of(query.category());
        String pattern = "%" + escapeLike(query.q().toLowerCase(Locale.ROOT)) + "%";

        long total = repository.countSearch(categories, pattern);
        int totalPages = (int) Math.max(1, (total + query.size() - 1) / query.size());
        int page = Math.min(query.page(), totalPages);

        List<BookResponse> books = total == 0 ? List.of()
                : repository.search(categories, pattern, PageRequest.of(page - 1, query.size(), query.sort().toSort()))
                        .stream().map(BookResponse::from).toList();
        return new PageResponse<>(books, page, query.size(), total, totalPages);
    }

    @Transactional(readOnly = true)
    public BookResponse get(String id) {
        return BookResponse.from(find(id));
    }

    /** Creates a book whose id is a slug of its title, suffixed {@code -2}, {@code -3}, ... if taken. */
    @Transactional
    public BookResponse create(BookRequest request) {
        validator.validate(request);
        if (request.isbn() != null && repository.existsByIsbn(request.isbn())) {
            throw isbnTaken();
        }
        Instant now = now();
        Book book = new Book(nextFreeId(request.title()), now);
        apply(request, book, now);
        return BookResponse.from(saveAndFlush(book));
    }

    /** Replaces the editable fields. The id, rating fields and {@code createdAt} never change. */
    @Transactional
    public BookResponse update(String id, BookRequest request) {
        Book book = find(id);
        validator.validate(request);
        if (request.isbn() != null && repository.existsByIsbnAndIdNot(request.isbn(), id)) {
            throw isbnTaken();
        }
        apply(request, book, now());
        return BookResponse.from(saveAndFlush(book));
    }

    @Transactional
    public void delete(String id) {
        repository.delete(find(id));
    }

    private Book find(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(
                "BOOK_NOT_FOUND", "This book does not exist. It may have been deleted."));
    }

    private String nextFreeId(String title) {
        String base = SlugGenerator.slugify(title);
        Set<String> taken = new HashSet<>(repository.findIdsMatching(base, escapeLike(base) + "-%"));
        return SlugGenerator.firstFree(base, taken::contains);
    }

    private void apply(BookRequest request, Book book, Instant now) {
        book.edit(
                request.title(),
                request.author(),
                request.category(),
                request.priceInr().setScale(2, RoundingMode.UNNECESSARY),
                request.isbn(),
                BookRequestValidator.parseDate(request.publishedAt()),
                request.description(),
                request.coverUrl(),
                now);
    }

    /**
     * Flushes so constraint violations surface here. The ISBN pre-check above can lose a race with a
     * concurrent write; the unique constraint then catches it and the caller still gets ISBN_TAKEN.
     */
    private Book saveAndFlush(Book book) {
        try {
            return repository.saveAndFlush(book);
        } catch (DataIntegrityViolationException ex) {
            String detail = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (detail.contains(ISBN_CONSTRAINT)) {
                throw isbnTaken();
            }
            throw ex;
        }
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    private static ConflictException isbnTaken() {
        return new ConflictException("ISBN_TAKEN", ApiError.VALIDATION_MESSAGE,
                Map.of("isbn", BookMessages.ISBN_TAKEN));
    }

    /** Escapes LIKE wildcards so user text matches literally; {@code !} is the escape character. */
    static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
