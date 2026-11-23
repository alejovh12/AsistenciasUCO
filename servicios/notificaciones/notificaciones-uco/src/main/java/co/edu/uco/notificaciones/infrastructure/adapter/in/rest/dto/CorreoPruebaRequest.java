package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /correos-prueba. Sin tipo se envia el correo generico de prueba del canal. */
public record CorreoPruebaRequest(
        @Size(max = 60) String tipo,
        @NotBlank @Size(max = 254) String correoDestino,
        @Size(max = 64) String solicitadoPor
) {
}
