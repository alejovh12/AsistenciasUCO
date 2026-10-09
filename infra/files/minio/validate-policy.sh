#!/bin/sh
set -eu

: "${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
: "${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"
: "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required}"
: "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required}"
: "${MINIO_BUCKET:?MINIO_BUCKET is required}"

export MC_CONFIG_DIR=/tmp/.mc-policy-test
mc alias set bootstrap http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null
mc alias set app http://minio:9000 "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" >/dev/null

probe="policy-$(date +%s)-$$"
other_bucket="asistencias-denied-${probe}"
target_object="${MINIO_BUCKET}/soportes/${probe}.txt"
payload=/tmp/policy-payload.txt
download=/tmp/policy-download.txt
printf 'least-privilege-policy-probe' > "${payload}"

cleanup() {
  mc rm --force "bootstrap/${target_object}" >/dev/null 2>&1 || true
  mc rm --recursive --force "bootstrap/${other_bucket}" >/dev/null 2>&1 || true
  mc rb --force "bootstrap/${other_bucket}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

mc mb "bootstrap/${other_bucket}" >/dev/null

mc cp "${payload}" "app/${target_object}" >/dev/null
mc cp "app/${target_object}" "${download}" >/dev/null
cmp "${payload}" "${download}"
mc rm "app/${target_object}" >/dev/null
echo "MINIO_APP_PUT_GET_DELETE_TARGET_BUCKET: PASS"

if mc mb "app/app-must-not-create-${probe}" >/dev/null 2>&1; then
  echo "MINIO_APP_CREATE_BUCKET: FAIL" >&2
  exit 1
fi
echo "MINIO_APP_CREATE_BUCKET: DENIED"

if mc cp "${payload}" "app/${other_bucket}/${probe}.txt" >/dev/null 2>&1; then
  echo "MINIO_APP_ACCESS_OTHER_BUCKET: FAIL" >&2
  exit 1
fi
echo "MINIO_APP_ACCESS_OTHER_BUCKET: DENIED"

mc cp "${payload}" "bootstrap/${target_object}" >/dev/null
get_status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "http://minio:9000/${target_object}")
put_status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
  --request PUT --data-binary @"${payload}" "http://minio:9000/${MINIO_BUCKET}/soportes/anonymous-${probe}.txt")

if [ "${get_status}" != "403" ] || [ "${put_status}" != "403" ]; then
  echo "MINIO_ANONYMOUS_ACCESS: FAIL (GET=${get_status}, PUT=${put_status})" >&2
  exit 1
fi
echo "MINIO_ANONYMOUS_GET: DENIED"
echo "MINIO_ANONYMOUS_PUT: DENIED"
