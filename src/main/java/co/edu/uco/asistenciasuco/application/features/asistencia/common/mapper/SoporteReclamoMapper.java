package co.edu.uco.asistenciasuco.application.features.asistencia.common.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.domain.SoporteReclamoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.CategoriaReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.SoporteReclamoDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SoporteReclamoProjection;

import java.util.List;

/**
 * Conversiones de categoria y soportes de un reclamo entre la proyeccion del repositorio, el modelo interno y el DTO de salida
 * (CULM-001 D.2). Las comparten el detalle del docente y la lista del estudiante.
 */
public final class SoporteReclamoMapper {

    private SoporteReclamoMapper() {
    }

    /** Lista de la fila del repositorio; nula se trata como sin soportes. */
    public static List<SoporteReclamoDomain> toDomain(final List<SoporteReclamoProjection> projections) {
        return projections == null ? List.of() : projections.stream()
                .map(p -> new SoporteReclamoDomain(p.fileId(), p.nombre(), p.tipoContenido(), p.tamanioBytes()))
                .toList();
    }

    public static List<SoporteReclamoDTO> toDTO(final List<SoporteReclamoDomain> soportes) {
        return soportes == null ? List.of() : soportes.stream()
                .map(s -> new SoporteReclamoDTO(s.fileId(), s.nombre(), s.tipoContenido(), s.tamanioBytes()))
                .toList();
    }

    /** Categoria del reclamo; nula en reclamos anteriores a la categoria persistida (no hay codigo ni nombre). */
    public static CategoriaReclamoDTO categoria(final String codigo, final String nombre) {
        return codigo == null && nombre == null ? null : new CategoriaReclamoDTO(codigo, nombre);
    }
}
