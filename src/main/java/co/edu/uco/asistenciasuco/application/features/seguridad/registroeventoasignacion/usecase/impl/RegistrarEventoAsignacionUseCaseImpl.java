package co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase.RegistrarEventoAsignacionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.security.RegistroEventoAsignacionPort;

import java.util.Objects;
import java.util.UUID;

/**
 * SEC-BASELINE-020: valida el contrato ({@code schemaVersion}, {@code applicationId}) y delega el registro
 * idempotente al puerto secundario. No conoce RabbitMQ, JPA ni Spring.
 */
public class RegistrarEventoAsignacionUseCaseImpl implements RegistrarEventoAsignacionUseCase {

    static final int SCHEMA_VERSION_SOPORTADA = 1;

    private final RegistroEventoAsignacionPort repositorio;
    private final UUID applicationIdPropio;

    public RegistrarEventoAsignacionUseCaseImpl(final RegistroEventoAsignacionPort repositorio, final UUID applicationIdPropio) {
        this.repositorio = Objects.requireNonNull(repositorio, "RegistroEventoAsignacionPort es obligatorio.");
        this.applicationIdPropio = Objects.requireNonNull(applicationIdPropio, "El applicationId propio es obligatorio.");
    }

    @Override
    public ResultadoRegistro procesar(final EventoAsignacionSeguridad evento) {
        validar(evento);
        return repositorio.registrarSiNuevo(evento) ? ResultadoRegistro.PROCESADO : ResultadoRegistro.DUPLICADO;
    }

    private void validar(final EventoAsignacionSeguridad evento) {
        if (evento == null || evento.eventId() == null) {
            throw new EventoAsignacionInvalidoException("El evento de asignacion no trae eventId.");
        }
        if (evento.schemaVersion() != SCHEMA_VERSION_SOPORTADA) {
            throw new EventoAsignacionInvalidoException("schemaVersion no soportada: " + evento.schemaVersion());
        }
        if (!applicationIdPropio.equals(evento.applicationId())) {
            throw new EventoAsignacionInvalidoException("applicationId ajeno a esta aplicacion: " + evento.applicationId());
        }
    }
}
