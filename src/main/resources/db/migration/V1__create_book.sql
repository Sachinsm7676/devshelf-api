-- The book catalogue. Written to run unchanged on PostgreSQL and on H2 in PostgreSQL mode (tests).
-- Note: H2 requires DEFAULT before NOT NULL; PostgreSQL accepts either order.
CREATE TABLE book (
    id           VARCHAR(160)             NOT NULL,
    title        VARCHAR(120)             NOT NULL,
    author       VARCHAR(80)              NOT NULL,
    category     VARCHAR(20)              NOT NULL,
    price_inr    NUMERIC(7, 2)            NOT NULL,
    isbn         VARCHAR(13),
    published_at DATE,
    description  VARCHAR(1000),
    cover_url    VARCHAR(500),
    rating       NUMERIC(2, 1)            DEFAULT 0 NOT NULL,
    rating_count INTEGER                  DEFAULT 0 NOT NULL,
    popularity   INTEGER                  DEFAULT 0 NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_book PRIMARY KEY (id),
    CONSTRAINT uk_book_isbn UNIQUE (isbn),
    CONSTRAINT ck_book_category CHECK (category IN
        ('JAVASCRIPT', 'JAVA', 'PYTHON', 'DEVOPS', 'SYSTEM_DESIGN', 'AI_ML', 'DATABASES')),
    CONSTRAINT ck_book_price_inr CHECK (price_inr >= 0)
);

CREATE INDEX ix_book_category ON book (category);
