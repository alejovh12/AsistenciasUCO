#!/bin/sh
set -eu

: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required}"
: "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required}"
: "${MINIO_BUCKET:?MINIO_BUCKET is required}"

if [ "${MINIO_BUCKET}" != "asistencias-soportes" ]; then
  echo "minio-init: the frozen bucket must be asistencias-soportes" >&2
  exit 1
fi

if [ "${MINIO_ROOT_USER}" = "${MINIO_ACCESS_KEY}" ] \
  || [ "${MINIO_ROOT_PASSWORD}" = "${MINIO_SECRET_KEY}" ]; then
  echo "minio-init: root and application credentials must be distinct" >&2
  exit 1
fi

export MC_CONFIG_DIR=/tmp/.mc-init
mc alias set bootstrap http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null
mc mb --ignore-existing "bootstrap/${MINIO_BUCKET}" >/dev/null
mc anonymous set none "bootstrap/${MINIO_BUCKET}" >/dev/null
mc admin user add bootstrap "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" >/dev/null
mc admin policy create bootstrap asistencias-backend /opt/asistencias/app-policy.json >/dev/null
mc admin policy attach bootstrap asistencias-backend --user "${MINIO_ACCESS_KEY}" >/dev/null

echo "minio-init: private bucket and least-privilege application identity are ready"
