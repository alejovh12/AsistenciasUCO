package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.TextoSeguro;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/admin/motivos-cancelacion-sesion}; sin orden queda al final. */
public record CrearMotivoCancelacionRequest(
        @NotBlank(message = "El campo codigo es obligatorio.")
        @Size(max = 50, message = "El campo codigo no puede superar 50 caracteres.")
        @Pattern(regexp = "^[A-Z0-9_]*$", message = "El codigo solo admite letras mayusculas, numeros y guion bajo.")
        String codigo,
        @NotBlank(message = "El campo nombre es obligatorio.")
        @TextoSeguro
        @Size(max = 200, message = "El campo nombre no puede superar 200 caracteres.")
        String nombre,
        @Min(value = 1, message = "El campo orden debe ser mayor que cero.") Integer orden) {
}
