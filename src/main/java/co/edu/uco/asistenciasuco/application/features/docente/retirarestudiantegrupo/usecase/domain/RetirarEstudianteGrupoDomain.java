package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.domain;

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
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        if (grupoId == null || estudianteId == null) {
            throw new ValidationException("El grupo y el estudiante son obligatorios.");
        }
        final String motivoNormalizado = motivo == null ? "" : motivo.strip();
        if (motivoNormalizado.isEmpty() || motivoNormalizado.length() > MOTIVO_MAXIMO) {
            throw new ValidationException("El motivo del retiro es obligatorio y no puede superar 500 caracteres.");
        }
        return new RetirarEstudianteGrupoDomain(actorUsuarioId, grupoId, estudianteId, motivoNormalizado);
    }
}
