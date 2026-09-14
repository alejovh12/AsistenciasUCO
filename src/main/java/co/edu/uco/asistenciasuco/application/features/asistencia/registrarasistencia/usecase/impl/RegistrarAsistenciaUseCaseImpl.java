package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.RegistrarAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain.RegistrarAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.mapper.RegistrarAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import java.util.Map;
import java.util.Objects;

/**
 * Implementacion del caso de uso registrar asistencia.
 *
 * <p>Tras persistir exitosamente, publica un {@link RealtimeEvent} de negocio a traves de
 * {@link RealtimePublisherPort}. La publicacion realtime es estrictamente posterior y
 * secundaria: {@link RealtimePublisherPort#publish(RealtimeEvent)} nunca lanza excepcion, por lo
 * que un problema de emision jamas revierte ni oculta el registro ya persistido.</p>
 */
public final class RegistrarAsistenciaUseCaseImpl implements RegistrarAsistenciaUseCase {

    private static final String EVENT_TYPE_ASISTENCIA_REGISTRADA = "ASISTENCIA_REGISTRADA";

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final RealtimePublisherPort realtimePublisherPort;

    public RegistrarAsistenciaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "El puerto de salida AsistenciaRepositoryPort es obligatorio.");
        this.realtimePublisherPort = Objects.requireNonNull(realtimePublisherPort, "El puerto de salida RealtimePublisherPort es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencia es obligatorio.");
        }
        asistenciaRepositoryPort.registrarAsistencia(RegistrarAsistenciaRepositoryMapper.toRepositoryDTO(domain));
        realtimePublisherPort.publish(RealtimeEvent.of(EVENT_TYPE_ASISTENCIA_REGISTRADA, Map.of(
                "estudiante", domain.getEstudiante().toString(),
                "grupo", domain.getGrupo().toString(),
                "sesion", domain.getSesion().toString(),
                "presente", domain.isPresente()
        )));
    }
}