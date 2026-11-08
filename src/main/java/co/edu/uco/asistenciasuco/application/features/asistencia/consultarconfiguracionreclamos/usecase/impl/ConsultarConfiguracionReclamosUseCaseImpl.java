package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.ConsultarConfiguracionReclamosUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain.ConfiguracionReclamosDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.mapper.ConsultarConfiguracionReclamosRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConfiguracionReclamosQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ConfiguracionReclamosProjection;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuracion del formulario de reclamos (CULM-001 D.2). El rol (ESTUDIANTE o DOCENTE) lo valida la base de datos en
 * {@code usp_consultar_configuracion_reclamos}; aqui solo se exige un usuario autenticado y una configuracion completa: un
 * parametro ausente es un fallo de configuracion, no un valor que Java deba inventar.
 */
public final class ConsultarConfiguracionReclamosUseCaseImpl implements ConsultarConfiguracionReclamosUseCase {

    private final ConfiguracionReclamosQueryPort queryPort;

    public ConsultarConfiguracionReclamosUseCaseImpl(final ConfiguracionReclamosQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "ConfiguracionReclamosQueryPort es obligatorio.");
    }

    @Override
    public ConfiguracionReclamosDomain execute(final UUID actorUsuarioId) {
        if (actorUsuarioId == null) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
        final ConfiguracionReclamosProjection fila = queryPort.consultar(actorUsuarioId);
        if (fila == null || fila.maximoSoportes() == null || fila.tamanioMaximoBytes() == null
                || fila.limiteSubidasPorMinuto() == null || fila.diasLimiteJustificacion() == null) {
            throw new InternalApplicationException("La configuracion de reclamos no esta disponible.");
        }
        return ConsultarConfiguracionReclamosRepositoryMapper.toDomain(fila);
    }
}
