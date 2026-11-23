package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /plantillas/{tipo}/versiones. */
public record NuevaVersionRequest(
        @Size(max = 2) String idioma,
        @NotBlank @Size(max = 200) String asunto,
        @NotBlank @Size(max = 4000) String cuerpo,
        @Size(max = 100) String creadoPor
) {
}
