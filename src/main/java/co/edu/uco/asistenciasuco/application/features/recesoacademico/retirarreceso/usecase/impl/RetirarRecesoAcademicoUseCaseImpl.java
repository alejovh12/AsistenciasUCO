package co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.usecase.RetirarRecesoAcademicoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;

import java.util.Objects;

/**
 * DEC-081: valida la forma y delega en la DB, que decide el ambito (ADMINISTRADOR de la institucion del periodo), que el dia exista
 * ({@code REC_009}, 404) y que no haya cancelado sesiones ({@code REC_010}, 409).
 */
public final class RetirarRecesoAcademicoUseCaseImpl implements RetirarRecesoAcademicoUseCase {

    private final RecesoAcademicoRepositoryPort repositorio;

    public RetirarRecesoAcademicoUseCaseImpl(final RecesoAcademicoRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "RecesoAcademicoRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para retirar el receso academico es obligatorio.");
        if (comando.idReceso() == null) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_069, "idReceso");
        }
        if (comando.usuarioEjecutor() == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_016);
        }
        return new Resultado(repositorio.retirar(comando.idReceso(), comando.usuarioEjecutor()));
    }
}
