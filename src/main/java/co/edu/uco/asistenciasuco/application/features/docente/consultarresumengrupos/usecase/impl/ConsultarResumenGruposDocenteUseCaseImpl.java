package co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumengrupos.usecase.ConsultarResumenGruposDocenteUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenGruposDocenteQueryPort;

import java.util.Objects;

/** CIERRE-W3I: orquesta la consulta; el perfil DOCENTE y el ambito (solo sus grupos) los valida {@code usp_consultar_resumen_grupos_docente}. */
public final class ConsultarResumenGruposDocenteUseCaseImpl implements ConsultarResumenGruposDocenteUseCase {

    private final ResumenGruposDocenteQueryPort resumen;

    public ConsultarResumenGruposDocenteUseCaseImpl(final ResumenGruposDocenteQueryPort resumen) {
        this.resumen = Objects.requireNonNull(resumen, "ResumenGruposDocenteQueryPort es obligatorio.");
    }

    @Override
    public ResumenGruposDocenteDTO execute(final Consulta consulta) {
        if (consulta == null || consulta.usuarioEjecutor() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        final ResumenGruposDocenteQueryPort.ResumenGrupos r = resumen.consultarResumenGruposDocente(consulta.usuarioEjecutor());
        return new ResumenGruposDocenteDTO(r.gruposHabilitados(), r.estudiantesActivos(), r.cupoTotal(), r.tasaOcupacion());
    }
}
