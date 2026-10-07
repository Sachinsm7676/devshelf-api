package com.devshelf.api.book;

import com.devshelf.api.book.dto.BookRequest;
import com.devshelf.api.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookServiceTest {

    private final BookRepository repository = mock(BookRepository.class);
    private final BookService service = new BookService(repository, mock(BookRequestValidator.class),
            Clock.fixed(Instant.parse("2026-10-07T20:00:00Z"), ZoneOffset.UTC));

    @Test
    void aUniqueConstraintRaceOnIsbnStillReportsIsbnTaken() {
        // The pre-check passes (the other insert has not committed yet) but the constraint fires on flush.
        when(repository.existsByIsbn("9789350000991")).thenReturn(false);
        when(repository.findIdsMatching(anyString(), anyString())).thenReturn(List.of());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("duplicate key value violates unique constraint \"uk_book_isbn\"")));

        BookRequest request = new BookRequest("Rust for Java Developers", "Asha Verma", BookCategory.JAVA,
                new BigDecimal("1099.50"), "9789350000991", null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOfSatisfying(ConflictException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("ISBN_TAKEN");
                    assertThat(ex.getFieldErrors()).isEqualTo(Map.of("isbn", "Another book already uses this ISBN."));
                });
    }

    @Test
    void otherIntegrityViolationsAreNotDisguisedAsIsbnConflicts() {
        when(repository.findIdsMatching(anyString(), anyString())).thenReturn(List.of());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("duplicate key value violates unique constraint \"pk_book\"")));

        BookRequest request = new BookRequest("Rust for Java Developers", "Asha Verma", BookCategory.JAVA,
                new BigDecimal("1099.50"), null, null, null, null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void escapesLikeWildcards() {
        assertThat(BookService.escapeLike("100%_!")).isEqualTo("100!%!_!!");
    }
}
