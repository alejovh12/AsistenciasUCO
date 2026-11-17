package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.domain.PeriodoAcademicoDomain;

import java.util.List;

public interface ConsultarPeriodosAcademicosUseCase { List<PeriodoAcademicoDomain> execute();

    /** CIERRE-W2A: con {@code estado} (PLANEACION, ACTIVO, CERRADO, sin distinguir mayusculas) solo los periodos en ese estado. */
    List<PeriodoAcademicoDomain> execute(String estado); }
