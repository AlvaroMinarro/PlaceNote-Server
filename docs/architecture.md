# Arquitectura del servidor (PlaceNote-Server)

## Rol

El servidor expone una API REST versionada (`/api/v1`) consumida por la app **PlaceNote-Client** (repositorio aparte). La fuente de verdad del contrato HTTP es [api/openapi.yaml](api/openapi.yaml).

## Datos

- **PostgreSQL** como base de datos principal (desarrollo local vía [infra/docker-compose.yml](../infra/docker-compose.yml)).
- Los identificadores de negocio son **UUID** generados en el cliente (offline-first); el servidor valida unicidad y permisos, no sustituye por IDs autoincrementales.

## Persistencia y migraciones

- **Flyway** aplica el esquema en `src/main/resources/db/migration` al arrancar.
- **Exposed** + **HikariCP** para acceso JDBC a PostgreSQL.

## Sincronización

- `POST /api/v1/sync/push` procesa operaciones por entidad (`REVIEW`, `RATING`, `FRIENDSHIP`) y acción (`INSERT`, `UPDATE`, `DELETE`).
- `GET /api/v1/sync/pull` devuelve reseñas visibles modificadas desde el cursor `since` (ISO-8601).

## Seguridad

- **JWT** (HS256) en rutas bajo `authenticate`: cabecera `Authorization: Bearer <token>`.
- En producción: definir `JWT_SECRET` robusto y TLS delante del servidor (Nginx/Caddy/Traefik).

## Despliegue

Self-hosted: Docker Compose puede ampliarse con un servicio de la aplicación Ktor además de PostgreSQL; el README describe el estado actual.
