ARG GO_IMAGE=golang:1.24.8-alpine3.21@sha256:03e93f0ccbe939edf5ee89f90a93e32ac1a5b6143a2a2f57405e4ad8b99379a6
FROM ${GO_IMAGE} AS builder

ARG MINIO_VERSION=RELEASE.2025-10-15T17-29-55Z
ARG MC_VERSION=RELEASE.2025-08-13T08-35-41Z

ENV CGO_ENABLED=0 \
    GOOS=linux \
    GOBIN=/out

RUN go install -trimpath -ldflags="-s -w" github.com/minio/minio@${MINIO_VERSION} \
    && go install -trimpath -ldflags="-s -w" github.com/minio/mc@${MC_VERSION}

FROM alpine:3.21.3@sha256:a8560b36e8b8210634f77d9f7f9efd7ffa463e380b75e2e74aff4511df3ef88c

ARG MINIO_VERSION=RELEASE.2025-10-15T17-29-55Z
ARG MC_VERSION=RELEASE.2025-08-13T08-35-41Z

LABEL org.opencontainers.image.source="https://github.com/minio/minio" \
      org.opencontainers.image.version="${MINIO_VERSION}" \
      org.opencontainers.image.revision="${MINIO_VERSION}" \
      org.opencontainers.image.licenses="AGPL-3.0-only" \
      io.asistenciasuco.mc.version="${MC_VERSION}"

RUN apk add --no-cache ca-certificates curl \
    && addgroup -S minio \
    && adduser -S -G minio -h /home/minio minio \
    && mkdir -p /data /home/minio/.mc \
    && chown -R minio:minio /data /home/minio

COPY --from=builder /out/minio /usr/local/bin/minio
COPY --from=builder /out/mc /usr/local/bin/mc
COPY minio-init.sh /usr/local/bin/minio-init
COPY validate-policy.sh /usr/local/bin/validate-minio-policy
COPY app-policy.json /opt/asistencias/app-policy.json

RUN chmod 0555 /usr/local/bin/minio /usr/local/bin/mc /usr/local/bin/minio-init \
        /usr/local/bin/validate-minio-policy \
    && chmod 0444 /opt/asistencias/app-policy.json

USER minio
VOLUME ["/data"]
EXPOSE 9000
ENTRYPOINT ["/usr/local/bin/minio"]
CMD ["server", "/data"]
