package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.common.mapper.FranjaHorarioMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.PrevisualizarHorarioGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.domain.PrevisualizarHorarioGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioGrupoPrevisualizacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;

import java.util.Objects;

/**
 * SES-GEN-01: previsualiza, sin escribir, las sesiones, horas y conflictos de un horario propuesto. Aqui solo se valida la forma (hay al
 * menos una franja y los identificadores minimos); las reglas de negocio, el calculo de las ranuras y el ambito del coordinador son de la
 * DB (misma definicion de ranuras que la precarga que ejecuta la confirmacion).
 */
public final class PrevisualizarHorarioGrupoUseCaseImpl implements PrevisualizarHorarioGrupoUseCase {

    private final HorarioGrupoPrevisualizacionPort previsualizacionPort;
    private final AsegurarFestivosPeriodoInputPort asegurarFestivos;

    public PrevisualizarHorarioGrupoUseCaseImpl(final HorarioGrupoPrevisualizacionPort previsualizacionPort,
                                                final AsegurarFestivosPeriodoInputPort asegurarFestivos) {
        this.previsualizacionPort = Objects.requireNonNull(previsualizacionPort, "HorarioGrupoPrevisualizacionPort es obligatorio.");
        this.asegurarFestivos = Objects.requireNonNull(asegurarFestivos, "AsegurarFestivosPeriodoInputPort es obligatorio.");
    }

    @Override
    public PrevisualizacionHorarioGrupoProjection execute(final PrevisualizarHorarioGrupoDomain domain) {
        if (domain.franjas() == null || domain.franjas().isEmpty()) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_076);
        }
        if (domain.idGrupo() == null && (domain.idAsignatura() == null || domain.idPeriodoAcademico() == null || domain.idDocente() == null)) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_069,
                    domain.idAsignatura() == null ? "idAsignatura" : domain.idPeriodoAcademico() == null ? "idPeriodoAcademico" : "idDocente");
        }
        // Paridad con la confirmacion (DR-07): igual que al crear o editar, se aseguran los festivos del periodo (mejor esfuerzo: nunca bloquea).
        asegurarFestivos.execute(domain.idGrupo() != null
                ? AsegurarFestivosPeriodoInputPort.Comando.deGrupo(domain.idGrupo())
                : AsegurarFestivosPeriodoInputPort.Comando.dePeriodo(domain.idPeriodoAcademico()));
        return previsualizacionPort.previsualizar(domain.idGrupo(), domain.idAsignatura(), domain.idPeriodoAcademico(), domain.idDocente(),
                FranjaHorarioMapper.toRepositoryDTOs(domain.franjas()), domain.usuarioEjecutor());
    }
}
