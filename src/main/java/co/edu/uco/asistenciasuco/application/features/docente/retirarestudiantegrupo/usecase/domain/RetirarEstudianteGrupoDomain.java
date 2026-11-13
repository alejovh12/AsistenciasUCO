package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.UUID;

/** Criterios validados de HU051: actor, grupo y estudiante obligatorios; motivo sin espacios extremos, 1..500. */
public record RetirarEstudianteGrupoDomain(UUID actorUsuarioId, UUID grupoId, UUID estudianteId, String motivo) {

    public static final int MOTIVO_MAXIMO = 500;

    public static RetirarEstudianteGrupoDomain of(
            final UUID actorUsuarioId,
            final UUID grupoId,
            final UUID estudianteId,
            final String motivo
    ) {
        if (actorUsuarioId == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        if (grupoId == null || estudianteId == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_053);
        }
        final String motivoNormalizado = motivo == null ? "" : motivo.strip();
        if (motivoNormalizado.isEmpty() || motivoNormalizado.length() > MOTIVO_MAXIMO) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_054);
        }
        return new RetirarEstudianteGrupoDomain(actorUsuarioId, grupoId, estudianteId, motivoNormalizado);
    }
}
