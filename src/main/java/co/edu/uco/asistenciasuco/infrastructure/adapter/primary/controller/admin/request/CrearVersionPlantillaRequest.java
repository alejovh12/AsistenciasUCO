package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import jakarta.validation.constraints.NotBlank;

public record CrearVersionPlantillaRequest(
        @NotBlank(message = "El asunto de la plantilla es obligatorio.") String asunto,
        @NotBlank(message = "El cuerpo de la plantilla es obligatorio.") String cuerpo,
        String idioma
) {
}
