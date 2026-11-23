package co.edu.uco.notificaciones.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /plantillas/vista-previa. El cuerpo puede ser de un borrador que aun no se guarda. */
public record VistaPreviaRequest(
        @NotBlank @Size(max = 60) String tipo,
        @Size(max = 2) String idioma,
        @NotNull @Size(max = 200) String asunto,
        @NotNull @Size(max = 4000) String cuerpo
) {
}
