package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Respuesta 2xx del servicio: estado HTTP y cuerpo JSON ({@code MissingNode} si no hubo cuerpo). */
public record RespuestaServicio(int status, JsonNode cuerpo) {
}
