# Backlog de endurecimiento (opcional)

Ítems de producto y operación que **no** están implementados de forma explícita en el código; priorizar según despliegues reales y amenazas.

## Seguridad y abuso

- **Rate limiting** en `POST /api/v1/auth/login` (plugin Ktor, o reglas en Caddy/Nginx/Traefik frente al API).
- **Refresh tokens** con rotación y almacenamiento seguro en cliente, si se deja de usar solo access token de corta duración.
- **Política de contraseñas** y mensajes de error que no filtren si un email existe.

## Observabilidad

- Logs estructurados (nivel, correlación de petición).
- Métricas (latencia, errores 4xx/5xx) y opcionalmente trazas (OpenTelemetry).

## Contrato y deprecaciones

- Calendario de **sunset** para rutas marcadas `deprecated` en OpenAPI (`/me`, `/friendships`, `PUT` ratings).
- Comunicación en README/CHANGELOG para releases que eliminen alias.

## Infraestructura self-hosted

- Copias de seguridad del volumen PostgreSQL.
- Renovación ACME y monitorización del proxy (Caddy) en producción.
