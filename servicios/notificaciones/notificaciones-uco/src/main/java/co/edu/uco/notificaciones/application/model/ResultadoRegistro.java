package co.edu.uco.notificaciones.application.model;

import co.edu.uco.notificaciones.domain.Notificacion;

/** {@code creada=false} cuando la clave de idempotencia ya existia: se devuelve la misma notificacion. */
public record ResultadoRegistro(Notificacion notificacion, boolean creada) {
}
