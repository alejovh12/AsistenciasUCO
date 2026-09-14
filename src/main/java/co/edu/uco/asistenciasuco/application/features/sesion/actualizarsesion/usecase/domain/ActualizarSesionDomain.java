package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ActualizarSesionDomain {

    private final UUID sesion;
    private final String nombre;
    private final LocalDateTime fechaHoraInicio;
    private final LocalDateTime fechaHoraFin;
    private final String aula;
    private final String descripcion;
    private final UUID docente;

    public ActualizarSesionDomain(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String aula,
            final String descripcion,
            final UUID docente
    ) {
        validarSesion(sesion);
        validarDocente(docente);
        validarFechas(fechaHoraInicio, fechaHoraFin);
        this.nombre = validarNombre(nombre);
        this.descripcion = validarDescripcion(descripcion);
        this.aula = TextHelper.trim(aula);
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.docente = docente;
        this.sesion = sesion;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(SesionErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    private void validarDocente(final UUID docente) {
        if (ObjectHelper.isNull(docente)) {
            throw new ValidationException(SesionErrorCode.ERR_DOCENTE_REQUERIDO);
        }
    }

    private void validarFechas(final LocalDateTime fechaHoraInicio, final LocalDateTime fechaHoraFin) {
        if (fechaHoraInicio == null || fechaHoraFin == null || !fechaHoraFin.isAfter(fechaHoraInicio)) {
            throw new ValidationException(SesionErrorCode.ERR_RANGO_FECHAS_SESION_INVALIDO);
        }
    }

    private String validarNombre(final String nombre) {
        final String normalizado = TextHelper.trim(nombre);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(SesionErrorCode.ERR_TEMA_SESION_REQUERIDO);
        }
        if (!TextHelper.hasLengthBetween(normalizado, 1, 150)) {
            throw new ValidationException(SesionErrorCode.ERR_TEMA_SESION_LONGITUD_INVALIDA);
        }
        return normalizado;
    }

    private String validarDescripcion(final String descripcion) {
        final String normalizada = TextHelper.trim(descripcion);
        if (TextHelper.isNullOrBlank(normalizada)) {
            return null;
        }
        if (!TextHelper.hasLengthBetween(normalizada, 10, 250)) {
            throw new ValidationException(SesionErrorCode.ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA);
        }
        return normalizada;
    }

    public UUID getSesion() {
        return sesion;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public String getAula() {
        return aula;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public UUID getDocente() {
        return docente;
    }
}
