# PlaceNote-Server

API [Ktor](https://ktor.io/) para **PlaceNote**: reseñas gastronómicas self-hosted, con PostgreSQL y contrato HTTP en [docs/api/openapi.yaml](docs/api/openapi.yaml).

Cliente oficial: [PlaceNote-Client](https://github.com/AlvaroMinarro/PlaceNote-Client) (repositorio separado).

## Requisitos

- JDK compatible con Kotlin 2 / Gradle 8 (recomendado JDK 17 u 21)
- Docker (solo para PostgreSQL en desarrollo)

## Desarrollo local

### 1. Base de datos

Desde la raíz de este repositorio:

```bash
cd infra
cp .env.example .env
docker compose up -d
```

PostgreSQL quedará disponible en `localhost` en el puerto configurado en `.env` (por defecto `5432`).

### 2. Servidor HTTP

```bash
./gradlew run
```

- Salud: `GET http://localhost:8080/health`
- Metadatos API: `GET http://localhost:8080/api/v1`

Variable opcional: `PORT` (por defecto `8080`).

## Compatibilidad con el cliente

El cliente declara la **versión mínima de API** que soporta. La fuente de verdad del contrato es `docs/api/openapi.yaml`. Los cambios que rompan compatibilidad deben incrementar la versión bajo `/api/v1` o publicar `/api/v2` según acuerdo del proyecto.

## Documentación

- [Arquitectura (servidor)](docs/architecture.md)
- [OpenAPI 3](docs/api/openapi.yaml)

## Licencia

MIT. Ver [LICENSE](LICENSE).
