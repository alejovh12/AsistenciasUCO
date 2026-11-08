package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto;

import java.util.List;

/** Pagina de estudiantes del coordinador. */
public record PaginaEstudiantesProgramaDTO(List<EstudianteProgramaDTO> elementos, long total) {
}
