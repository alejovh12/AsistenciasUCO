package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.SinEtiquetasHtml;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.TextoSeguro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/admin/categorias-justificacion}. La DB repite las reglas (CTG_001, CTG_002, CTG_004). */
public record CrearCategoriaJustificacionRequest(
        @NotBlank(message = "El campo codigo es obligatorio.")
        @Size(max = 5, message = "El campo codigo no puede superar 5 caracteres.")
        @Pattern(regexp = "^[A-Z0-9_]*$", message = "El codigo solo admite letras mayusculas, numeros y guion bajo.")
        String codigo,
        @NotBlank(message = "El campo nombre es obligatorio.")
        @TextoSeguro
        @Size(max = 50, message = "El campo nombre no puede superar 50 caracteres.")
        String nombre,
        @SinEtiquetasHtml
        @Size(max = 250, message = "El campo descripcion no puede superar 250 caracteres.")
        String descripcion,
        @NotNull(message = "El campo requiereSoporte es obligatorio.") Boolean requiereSoporte) {
}
