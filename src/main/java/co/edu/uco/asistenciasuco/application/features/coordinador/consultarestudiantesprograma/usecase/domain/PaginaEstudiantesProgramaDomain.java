package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain;

import java.util.List;

/** Pagina de estudiantes del coordinador. */
public record PaginaEstudiantesProgramaDomain(List<EstudianteProgramaDomain> elementos, long total) {
}
