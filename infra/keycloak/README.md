# Keycloak - AsistenciasUCO

Infraestructura local de identidad y autenticacion para AsistenciasUCO.

## Servicios

- Keycloak: http://localhost:8081
- Keycloak Management / Health: http://localhost:9001
- PostgreSQL: disponible unicamente dentro de la red Docker

## Configuracion inicial

Antes de iniciar los servicios, reemplazar en `.env`:

- `CHANGE_ME_KEYCLOAK_ADMIN_PASSWORD`
- `CHANGE_ME_KEYCLOAK_DB_PASSWORD`

por contrasenas seguras y diferentes.

## Validar configuracion

```bash
docker compose config
```

## Descargar imagenes

```bash
docker compose pull
```

## Iniciar

```bash
docker compose up -d
```

## Ver estado

```bash
docker compose ps
```

## Logs de Keycloak

```bash
docker compose logs -f keycloak
```

## Logs de PostgreSQL

```bash
docker compose logs -f keycloak-db
```

## Comprobar health

http://localhost:9001/health/ready

## Detener

```bash
docker compose down
```

## Advertencia

No ejecutar:

```bash
docker compose down -v
```

salvo que se quiera eliminar completamente el volumen local de PostgreSQL utilizado por Keycloak.

Eliminar el volumen implica perder realms, usuarios, clients, roles y demas configuraciones locales almacenadas en Keycloak.
