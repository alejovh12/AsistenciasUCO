package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.ConsultarEstudiantesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.PaginaEstudiantesProgramaDomain;

public interface ConsultarEstudiantesProgramaUseCase {
    PaginaEstudiantesProgramaDomain execute(ConsultarEstudiantesProgramaDomain consulta);
}
