# Arquitectura del servidor (PlaceNote-Server)

## Rol

El servidor expone una API REST versionada (`/api/v1`) consumida por la app **PlaceNote-Client** (repositorio aparte). La fuente de verdad del contrato HTTP es [api/openapi.yaml](api/openapi.yaml).

## Datos

- **PostgreSQL** como base de datos principal (desarrollo local vía [infra/docker-compose.yml](../infra/docker-compose.yml)).
- Los identificadores de negocio son **UUID** generados en el cliente (offline-first); el servidor valida unicidad y permisos, no sustituye por IDs autoincrementales.

## Sincronización

La app puede operar sin red y enviar cambios en lotes. El servidor debe:

- Aceptar operaciones idempotentes donde aplique (mismo UUID = misma entidad).
- Resolver conflictos según política acordada (p. ej. `modified_at` / last-write-wins documentada en OpenAPI).

## Seguridad (pendiente de implementación)

- Autenticación orientada a **tokens** (p. ej. JWT) para rutas protegidas.
- TLS en producción (reverse proxy Nginx/Caddy/Traefik delante del contenedor del servidor).

## Despliegue

Self-hosted: Docker Compose puede ampliarse con un servicio de la aplicación Ktor además de PostgreSQL; el README describe el estado actual.
