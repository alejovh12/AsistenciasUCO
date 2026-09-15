package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.dto.EjecutarCierreMasivoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain.EjecutarCierreMasivoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class EjecutarCierreMasivoMapper {

    private EjecutarCierreMasivoMapper() {
    }

    public static EjecutarCierreMasivoDomain toDomain(final EjecutarCierreMasivoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para ejecutar cierre masivo es obligatorio.");
        }

        return new EjecutarCierreMasivoDomain(dto.idPeriodoAcademico(), dto.actorUsuarioId());
    }
}
