package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto;

import java.util.List;

/** Pagina de sesiones de una materia del estudiante: las de la pagina y el total que cumple el filtro. */
public record SesionesMateriaEstudiantePaginaDTO(List<SesionMateriaEstudianteDTO> sesiones, long totalRegistros) {

    public SesionesMateriaEstudiantePaginaDTO {
        sesiones = sesiones == null ? List.of() : List.copyOf(sesiones);
    }
}
