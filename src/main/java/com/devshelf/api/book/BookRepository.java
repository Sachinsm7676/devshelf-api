package com.devshelf.api.book;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, String> {

    /**
     * Shared filter for {@link #search} and {@link #countSearch}. {@code pattern} is an already
     * lower-cased LIKE pattern whose escape character is {@code !}.
     */
    String SEARCH_FILTER = """
            where b.category in :categories
              and (lower(b.title) like :pattern escape '!'
                   or lower(b.author) like :pattern escape '!'
                   or lower(coalesce(b.isbn, '')) like :pattern escape '!')
            """;

    /** One page of matching books; ordering, offset and limit come from {@code pageable}. */
    @Query("select b from Book b " + SEARCH_FILTER)
    List<Book> search(@Param("categories") Collection<BookCategory> categories,
                      @Param("pattern") String pattern,
                      Pageable pageable);

    @Query("select count(b) from Book b " + SEARCH_FILTER)
    long countSearch(@Param("categories") Collection<BookCategory> categories,
                     @Param("pattern") String pattern);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, String id);

    /** Ids equal to {@code base} or matching {@code prefixPattern} (a LIKE pattern escaped with {@code !}). */
    @Query("select b.id from Book b where b.id = :base or b.id like :prefixPattern escape '!'")
    List<String> findIdsMatching(@Param("base") String base, @Param("prefixPattern") String prefixPattern);
}
