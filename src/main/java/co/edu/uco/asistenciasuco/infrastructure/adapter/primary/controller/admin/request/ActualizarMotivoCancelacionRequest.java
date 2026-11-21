package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.TextoSeguro;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code PUT /api/v1/admin/motivos-cancelacion-sesion/{id}}: el codigo no cambia. */
public record ActualizarMotivoCancelacionRequest(
        @NotBlank(message = "El campo nombre es obligatorio.")
        @TextoSeguro
        @Size(max = 200, message = "El campo nombre no puede superar 200 caracteres.")
        String nombre,
        @NotNull(message = "El campo orden es obligatorio.")
        @Min(value = 1, message = "El campo orden debe ser mayor que cero.") Integer orden) {
}
