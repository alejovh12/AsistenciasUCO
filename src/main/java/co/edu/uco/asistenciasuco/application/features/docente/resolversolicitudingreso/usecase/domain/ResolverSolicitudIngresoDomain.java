package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.Locale;
import java.util.UUID;

/** Criterios validados: accion ACEPTAR/RECHAZAR; al rechazar, motivo sin espacios extremos de 1..500 caracteres. */
public record ResolverSolicitudIngresoDomain(UUID actorUsuarioId, UUID solicitudId, String accion, String motivo) {

    public static final String ACEPTAR = "ACEPTAR";
    public static final String RECHAZAR = "RECHAZAR";
    public static final int MOTIVO_MAXIMO = 500;

    public static ResolverSolicitudIngresoDomain of(
            final UUID actorUsuarioId,
            final UUID solicitudId,
            final String accion,
            final String motivo
    ) {
        if (actorUsuarioId == null || solicitudId == null) {
            throw new ValidationException("El usuario autenticado y la solicitud son obligatorios.");
        }
        final String accionNormalizada = accion == null ? "" : accion.strip().toUpperCase(Locale.ROOT);
        if (!ACEPTAR.equals(accionNormalizada) && !RECHAZAR.equals(accionNormalizada)) {
            throw new ValidationException("La accion debe ser ACEPTAR o RECHAZAR.");
        }
        if (ACEPTAR.equals(accionNormalizada)) {
            return new ResolverSolicitudIngresoDomain(actorUsuarioId, solicitudId, accionNormalizada, null);
        }
        final String motivoNormalizado = motivo == null ? "" : motivo.strip();
        if (motivoNormalizado.isEmpty() || motivoNormalizado.length() > MOTIVO_MAXIMO) {
            throw new ValidationException("El motivo del rechazo es obligatorio y no puede superar 500 caracteres.");
        }
        return new ResolverSolicitudIngresoDomain(actorUsuarioId, solicitudId, accionNormalizada, motivoNormalizado);
    }
}
