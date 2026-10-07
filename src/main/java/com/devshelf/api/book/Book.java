package com.devshelf.api.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A book in the catalogue. The id is a slug assigned once at creation and never changed.
 * Rating, rating count and popularity are maintained outside this API and are read-only here.
 */
@Entity
@Table(name = "book")
public class Book implements Persistable<String> {

    @Id
    @Column(length = 160)
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 80)
    private String author;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private BookCategory category;

    @Column(name = "price_inr", nullable = false, precision = 7, scale = 2)
    private BigDecimal priceInr;

    @Column(length = 13, unique = true)
    private String isbn;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(length = 1000)
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(nullable = false)
    private int popularity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Lets {@code save} insert directly instead of first probing for a row with the assigned id. */
    @Transient
    private boolean isNew;

    protected Book() {
        // for JPA
    }

    /** A new, unrated book; call {@link #edit} to fill in its details. */
    public Book(String id, Instant now) {
        this.id = id;
        this.rating = new BigDecimal("0.0");
        this.ratingCount = 0;
        this.popularity = 0;
        this.createdAt = now;
        this.updatedAt = now;
        this.isNew = true;
    }

    /** Replaces every editable field and stamps {@code updatedAt}. */
    public void edit(String title, String author, BookCategory category, BigDecimal priceInr, String isbn,
                     LocalDate publishedAt, String description, String coverUrl, Instant now) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.priceInr = priceInr;
        this.isbn = isbn;
        this.publishedAt = publishedAt;
        this.description = description;
        this.coverUrl = coverUrl;
        this.updatedAt = now;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public BookCategory getCategory() {
        return category;
    }

    public BigDecimal getPriceInr() {
        return priceInr;
    }

    public String getIsbn() {
        return isbn;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getPopularity() {
        return popularity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
