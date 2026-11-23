package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

/** Respuesta de POST /notificaciones: 202 si es nueva, 200 con la misma id si la clave de idempotencia ya existia. */
public record NotificacionAceptadaResponse(UUID id, String estado, String idempotencyKey, boolean duplicada) {
}
