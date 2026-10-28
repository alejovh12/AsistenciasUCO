package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain;

import java.time.LocalDate;
import java.util.UUID;

public record GrupoHistorialDocenteDomain(
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idPeriodoAcademico,
        Integer codigoPeriodoAcademico,
        String nombrePeriodoAcademico,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaFinPeriodo,
        String estadoPeriodo,
        int capacidadMaxima,
        int estudiantesActivos,
        int estudiantesFinalizados,
        int estudiantesCanceladosVoluntad,
        int estudiantesCanceladosInasistencia
) {
}
