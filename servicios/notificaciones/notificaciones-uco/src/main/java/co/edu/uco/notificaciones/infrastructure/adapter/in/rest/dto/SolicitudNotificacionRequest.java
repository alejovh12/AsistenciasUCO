package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Cuerpo de POST /api/v1/notificaciones. */
public record SolicitudNotificacionRequest(
        @NotBlank @Size(max = 60) String aplicacion,
        @NotBlank @Size(max = 120) String idempotencyKey,
        @NotBlank @Size(max = 60) String tipo,
        @Size(max = 2) String idioma,
        @Size(max = 200) String asunto,
        @Size(max = 4000) String mensaje,
        @NotEmpty @Size(max = 200) List<@NotNull @Valid DestinatarioRequest> destinatarios,
        Map<String, String> datos,
        @Size(max = 64) String idCorrelacion,
        @Size(max = 80) String traceparent
) {

    public record DestinatarioRequest(@NotBlank @Size(max = 254) String correo, @Size(max = 200) String nombre) {
    }
}
