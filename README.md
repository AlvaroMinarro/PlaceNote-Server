# PlaceNote-Server

API [Ktor](https://ktor.io/) para **PlaceNote**: reseñas gastronómicas self-hosted, con PostgreSQL y contrato HTTP en [docs/api/openapi.yaml](docs/api/openapi.yaml).

Cliente oficial: [PlaceNote-Client](https://github.com/AlvaroMinarro/PlaceNote-Client) (repositorio separado).

## Requisitos

- JDK compatible con Kotlin 2 / Gradle 8 (recomendado JDK 17 u 21)
- **Docker:** imprescindible si usas contenedores. Sirve para levantar **solo PostgreSQL** mientras desarrollas con `./gradlew run` en el host, o el **stack completo** (PostgreSQL + API Ktor + Caddy) descrito en [Despliegue en producción](#despliegue-en-producción-docker) y en [infra/docker-compose.yml](infra/docker-compose.yml).

## Desarrollo local

### 1. Base de datos

Desde la raíz de este repositorio:

```bash
cd infra
cp .env.example .env
docker compose up -d postgres
```

PostgreSQL quedará disponible en `localhost` en el puerto configurado en `.env` (por defecto `5432`). Para levantar solo la base, usa el servicio `postgres` como arriba; el compose completo también define la API y Caddy (véase más abajo).

### 2. Variables de entorno

El servidor lee la configuración de PostgreSQL y JWT desde el entorno (o un `.env` cargado manualmente). Tras `cp infra/.env.example infra/.env`, puedes exportarlas desde la raíz del repo:

```bash
set -a && source infra/.env && set +a
```

Imprescindibles para producción: `JWT_SECRET` (cadena larga y aleatoria). Las variables `POSTGRES_*` deben coincidir con las del contenedor Docker.

### 3. Servidor HTTP

```bash
./gradlew run
```

- Salud: `GET http://localhost:8080/health`
- Metadatos API: `GET http://localhost:8080/api/v1`
- Registro: `POST /api/v1/auth/register` · Login: `POST /api/v1/auth/login`

Variables opcionales: `PORT` (por defecto `8080`), `JWT_ACCESS_TTL_SECONDS` (segundos de validez del token).

### Tests

Requiere **Docker** (Testcontainers). En la raíz del servidor:

```bash
./gradlew test
```

## Despliegue en producción (Docker)

Stack recomendado: **PostgreSQL** + **API Ktor** (imagen construida con el `Dockerfile` de este repo) + **Caddy** como proxy TLS. Requisitos habituales: un VPS con Docker, DNS del dominio apuntando al servidor, puertos 80/443 abiertos para Let’s Encrypt (si usas HTTPS automático).

1. Copia `infra/.env.example` a `infra/.env` y define al menos `JWT_SECRET`, credenciales de PostgreSQL y, para HTTPS con Caddy hacia un dominio real, `DOMAIN` y `ACME_EMAIL`.
2. Revisa `infra/Caddyfile.example`: para desarrollo local suele bastar el bloque por defecto (`tls internal`). En producción, sigue los comentarios del archivo para usar el bloque con `tls` y correo ACME, o sustituye por un proxy que ya gestione TLS.
3. Desde `infra/`:

   ```bash
   docker compose build api
   docker compose up -d
   ```

La API escucha en el puerto 8080 **dentro de la red Docker**; Caddy publica 80/443 según `CADDY_HTTP_PORT` / `CADDY_HTTPS_PORT` en `.env`. Documentación del servidor web: [Caddy](https://caddyserver.com/docs/).

## Compatibilidad con el cliente

El cliente declara la **versión mínima de API** que soporta. La fuente de verdad del contrato es `docs/api/openapi.yaml`. Los cambios que rompan compatibilidad deben incrementar la versión bajo `/api/v1` o publicar `/api/v2` según acuerdo del proyecto.

## Documentación

- [Arquitectura (servidor)](docs/architecture.md)
- [OpenAPI 3](docs/api/openapi.yaml)

## Licencia

MIT. Ver [LICENSE](LICENSE).
