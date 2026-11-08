package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.CategoriaReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.SoporteReclamoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CategoriaReclamoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.SoporteReclamoResponse;

import java.util.List;

/** Categoria y soportes de un reclamo hacia la representacion HTTP (CULM-001 D.2); compartido por el detalle del docente y la lista del estudiante. */
public final class ReclamoCategoriaSoportesHttpMapper {

    private ReclamoCategoriaSoportesHttpMapper() {
    }

    public static CategoriaReclamoResponse categoria(final CategoriaReclamoDTO dto) {
        return dto == null ? null : new CategoriaReclamoResponse(dto.codigo(), dto.nombre());
    }

    public static List<SoporteReclamoResponse> soportes(final List<SoporteReclamoDTO> dtos) {
        return dtos == null ? List.of() : dtos.stream()
                .map(s -> new SoporteReclamoResponse(s.fileId(), s.nombre(), s.tipoContenido(), s.tamanioBytes()))
                .toList();
    }
}
