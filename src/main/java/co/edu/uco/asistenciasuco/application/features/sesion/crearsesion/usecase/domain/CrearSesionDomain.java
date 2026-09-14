package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain;



import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Dominio de la operacion crear sesion.
 */
public final class CrearSesionDomain {

    private final UUID grupo;
    private final String tema;
    private final String descripcion;
    private final LocalDateTime fechaHoraInicio;
    private final LocalDateTime fechaHoraFin;
    private final String aula;
    private final String tipo;
    private final UUID docente;

    public CrearSesionDomain(
            final UUID grupo,
            final String tema,
            final String descripcion,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String aula,
            final String tipo,
            final UUID docente
    ) {
        validarGrupo(grupo);
        validarDocente(docente);
        validarFechas(fechaHoraInicio, fechaHoraFin);
        this.tema = validarTema(tema);
        this.descripcion = validarDescripcion(descripcion);
        this.aula = TextHelper.trim(aula);
        this.tipo = TextHelper.trim(tipo);
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.docente = docente;

        this.grupo = grupo;
    }

    private void validarGrupo(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
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

    private String validarTema(final String tema) {
        final String temaNormalizado = TextHelper.trim(tema);

        if (TextHelper.isNullOrBlank(temaNormalizado)) {
            throw new ValidationException(SesionErrorCode.ERR_TEMA_SESION_REQUERIDO);
        }

        if (!TextHelper.hasLengthBetween(temaNormalizado, 5, 100)) {
            throw new ValidationException(SesionErrorCode.ERR_TEMA_SESION_LONGITUD_INVALIDA);
        }

        return temaNormalizado;
    }

    private String validarDescripcion(final String descripcion) {
        final String descripcionNormalizada = TextHelper.trim(descripcion);

        if (TextHelper.isNullOrBlank(descripcionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(descripcionNormalizada, 10, 250)) {
            throw new ValidationException(SesionErrorCode.ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA);
        }

        return descripcionNormalizada;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public String getTema() {
        return tema;
    }

    public String getDescripcion() {
        return descripcion;
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

    public String getTipo() {
        return tipo;
    }

    public UUID getDocente() {
        return docente;
    }

}
