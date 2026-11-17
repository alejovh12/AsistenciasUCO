package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.domain;

import java.math.BigDecimal;

public record ResumenMateriasEstudianteDomain(int totalMaterias, Integer materiasEnRiesgo, BigDecimal totalHorasFalta, int totalCreditos) {

    public ResumenMateriasEstudianteDomain {
        totalHorasFalta = totalHorasFalta == null ? BigDecimal.ZERO : totalHorasFalta;
    }
}
