package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Cuerpo de {@code POST /api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas} (CULM-001 B2NM).
 *
 * <p>ROL-06: el controlador ya no recibe el DTO de Application como cuerpo HTTP; este request de Infrastructure lleva las
 * anotaciones de Bean Validation (Application no depende de {@code jakarta.validation}). El JSON es el mismo.
 * {@code codigoCaracter} es el codigo del catalogo {@code CaracterAsignaturaPlan} (NVARCHAR(3)).</p>
 */
public record AsociarAsignaturaPlanRequest(
        @NotNull(message = "La asignatura es obligatoria.") UUID idAsignatura,
        @NotNull(message = "El semestre es obligatorio.")
        @Positive(message = "El semestre debe ser un entero positivo.") Integer semestreNumero,
        @Size(max = 3, message = "El codigo del caracter no puede superar 3 caracteres.") String codigoCaracter
) {
}
