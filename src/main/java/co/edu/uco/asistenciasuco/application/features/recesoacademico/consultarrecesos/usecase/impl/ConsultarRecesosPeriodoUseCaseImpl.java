package co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.usecase.ConsultarRecesosPeriodoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;

import java.util.List;
import java.util.Objects;

/** DEC-081: valida la forma y delega en la DB, que aplica el ambito institucional y ordena los dias por fecha. */
public final class ConsultarRecesosPeriodoUseCaseImpl implements ConsultarRecesosPeriodoUseCase {

    private final RecesoAcademicoRepositoryPort repositorio;

    public ConsultarRecesosPeriodoUseCaseImpl(final RecesoAcademicoRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "RecesoAcademicoRepositoryPort es obligatorio.");
    }

    @Override
    public List<RecesoDia> execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar los recesos del periodo es obligatorio.");
        if (comando.idPeriodo() == null) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_069, "idPeriodo");
        }
        if (comando.usuarioEjecutor() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_016);
        }
        return repositorio.consultar(comando.idPeriodo(), comando.usuarioEjecutor()).stream()
                .map(r -> new RecesoDia(r.id(), r.fecha(), r.motivo(), r.sesionesCanceladas(), r.estaAplicado(), r.puedeRetirarse()))
                .toList();
    }
}
