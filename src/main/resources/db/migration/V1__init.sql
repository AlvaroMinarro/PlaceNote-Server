CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE friendships (
    id UUID PRIMARY KEY,
    user_a_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_b_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    initiated_by UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (user_a_id < user_b_id),
    UNIQUE (user_a_id, user_b_id)
);

CREATE INDEX idx_friendships_state ON friendships (state);

CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    nombre_sitio TEXT NOT NULL,
    latitud DOUBLE PRECISION NOT NULL,
    longitud DOUBLE PRECISION NOT NULL,
    foto_url TEXT,
    ocr_texto_crudo TEXT,
    precio_total DOUBLE PRECISION,
    fecha_visita TIMESTAMPTZ NOT NULL,
    creador_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_reviews_creador ON reviews (creador_id);
CREATE INDEX idx_reviews_modified ON reviews (modified_at);

CREATE TABLE ratings (
    review_id UUID NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    nota DOUBLE PRECISION NOT NULL,
    comentario_privado TEXT,
    modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (review_id, user_id),
    CHECK (nota >= 0 AND nota <= 10)
);

CREATE INDEX idx_ratings_modified ON ratings (modified_at);
