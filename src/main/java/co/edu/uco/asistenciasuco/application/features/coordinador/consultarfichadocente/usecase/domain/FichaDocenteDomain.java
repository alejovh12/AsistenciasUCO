package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;

import java.util.List;

public record FichaDocenteDomain(
        DocenteProgramaDomain docente,
        ResumenCargaDocenteDomain resumen,
        List<GrupoHistorialDocenteDomain> historial,
        long totalHistorial
) {

    public FichaDocenteDomain {
        historial = historial == null ? List.of() : List.copyOf(historial);
    }
}
