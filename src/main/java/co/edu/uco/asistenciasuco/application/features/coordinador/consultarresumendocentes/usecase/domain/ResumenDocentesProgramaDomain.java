package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain;

import java.util.List;

public record ResumenDocentesProgramaDomain(
        int totalDocentes,
        int totalActivos,
        int totalInactivos,
        List<ResumenTipoVinculacionDomain> porTipoVinculacion
) {

    public ResumenDocentesProgramaDomain {
        porTipoVinculacion = porTipoVinculacion == null ? List.of() : List.copyOf(porTipoVinculacion);
    }
}
