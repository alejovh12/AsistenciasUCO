package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;

import java.util.List;

/** Pagina del padron con la pagina y el tamano efectivos y el total que cumple el filtro. */
public record PadronGrupoPaginaDTO(
        List<EstudianteGrupoDTO> estudiantes, int numeroPagina, int tamanoPagina, long totalRegistros) {
}
