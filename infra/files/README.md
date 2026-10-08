# Infraestructura de archivos — MinIO + ClamAV

Esta carpeta contiene la infraestructura local de la storage foundation LB-004B.2H. El compose de
la raíz conserva únicamente la responsabilidad del backend.

## Preparación y arranque DEV_ONLY

Copiar `.env.example` a `.env` y reemplazar todos los `CHANGE_ME_*` por valores locales distintos.
No versionar `.env` ni reutilizar las credenciales root como credenciales de aplicación.

```sh
docker compose --env-file infra/files/.env \
  -f infra/files/compose.yaml up -d
```

```sh
docker compose --env-file infra/files/.env \
  -f infra/files/compose.yaml down
```

Los puertos `9000` (MinIO API) y `3310` (clamd) se publican exclusivamente en `127.0.0.1` para que
un backend iniciado desde IntelliJ pueda accederlos. Esta publicación es `DEV_ONLY`. La consola de
MinIO está deshabilitada y no se publica ningún puerto de consola.

En una ejecución completamente containerizada o deployment, el backend debe unirse a la red
privada del runtime y usar nombres internos (`http://minio:9000`, `clamav:3310`); los bloques
`ports` DEV_ONLY no se incluyen en el manifiesto de deployment. MinIO/ClamAV no se publican al host
ni a ingress. Este compose local no es el manifiesto de producción.

## Identidades y policy

- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`: bootstrap y administración; solo llegan a `minio`,
  `minio-init` y al perfil explícito de validación.
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`: usuario técnico de aplicación creado/verificado por
  `minio-init`; son las únicas credenciales que recibe Spring Boot. El bootstrap falla si usuario
  o secreto de aplicación coinciden con los equivalentes root.
- `asistencias-backend`: permite `s3:GetObject`, `s3:PutObject` y `s3:DeleteObject` únicamente sobre
  `arn:aws:s3:::asistencias-soportes/soportes/*`. `HeadObject` usa la autorización `GetObject` del
  protocolo S3. Además permite `s3:GetBucketLocation` exclusivamente sobre
  `arn:aws:s3:::asistencias-soportes`: el SDK MinIO Java ejecuta esa consulta antes del
  `PutObject`; sin ella la integración real responde `AccessDenied`. No permite crear/eliminar
  buckets, administrar usuarios/policies ni acceder a otro bucket/prefix.

El bucket `asistencias-soportes` se crea y mantiene privado en `minio-init`. El backend no crea
buckets en runtime, no entrega presigned URLs y no recibe credenciales root.

## Validación real de policy

Con los servicios levantados:

```sh
docker compose --env-file infra/files/.env \
  -f infra/files/compose.yaml --profile validation run --rm minio-policy-test
```

El comando exige Put/Get/Delete en el prefix permitido y exige `DENIED` para create-bucket, otro
bucket, anonymous GET y anonymous PUT. No sustituye los IT Java de MinIO/ClamAV/HTTP.

## Imagen MinIO

`infra/files/minio/Dockerfile` compila MinIO Community desde el tag upstream verificable
`RELEASE.2025-10-15T17-29-55Z` (commit corto publicado `9e49d5e`) y `mc` desde
`RELEASE.2025-08-13T08-35-41Z` (commit corto publicado `7394ce0`), usando Go `1.24.8`. No usa
`:latest`, `bitnamilegacy` ni una distribución comercial silenciosa.

MinIO Community es AGPL-3.0 y upstream quedó archivado/read-only en 2026. Por ello esta imagen se
clasifica `LOCAL_DEV_TEMPORARY`: es source-built y reproducible por tag, pero no se declara target
productivo mantenido ni con SLA. La selección de un target productivo mantenido y la revisión de
obligaciones AGPL permanecen abiertas antes de deployment.
