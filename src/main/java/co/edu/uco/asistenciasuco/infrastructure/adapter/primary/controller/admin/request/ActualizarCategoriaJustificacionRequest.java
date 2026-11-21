package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.SinEtiquetasHtml;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.TextoSeguro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code PUT /api/v1/admin/categorias-justificacion/{id}}: el codigo no cambia. */
public record ActualizarCategoriaJustificacionRequest(
        @NotBlank(message = "El campo nombre es obligatorio.")
        @TextoSeguro
        @Size(max = 50, message = "El campo nombre no puede superar 50 caracteres.")
        String nombre,
        @SinEtiquetasHtml
        @Size(max = 250, message = "El campo descripcion no puede superar 250 caracteres.")
        String descripcion,
        @NotNull(message = "El campo requiereSoporte es obligatorio.") Boolean requiereSoporte) {
}
