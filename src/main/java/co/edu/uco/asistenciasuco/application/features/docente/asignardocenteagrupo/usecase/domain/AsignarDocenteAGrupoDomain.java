package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;

import java.util.Objects;
import java.util.UUID;

/**
 * Dominio de la operacion asignar o reasignar docente a grupo.
 */
public final class AsignarDocenteAGrupoDomain {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final UUID docente;
    private final UUID grupo;

    private AsignarDocenteAGrupoDomain(
            final UUID docente,
            final UUID grupo
    ) {
        this.docente = docente;
        this.grupo = grupo;
    }

    public static AsignarDocenteAGrupoDomain crear(
            final UUID docente,
            final UUID grupo
    ) {
        validarDocente(docente);
        validarGrupo(grupo);
        return new AsignarDocenteAGrupoDomain(docente, grupo);
    }

    private static void validarDocente(final UUID docente) {
        validarIdentificador(
                docente,
                "ERR_DOCENTE_REQUERIDO",
                "El docente es obligatorio.",
                "ERR_DOCENTE_INVALIDO",
                "Debe seleccionar un docente valido."
        );
    }

    private static void validarGrupo(final UUID grupo) {
        validarIdentificador(
                grupo,
                "ERR_GRUPO_REQUERIDO",
                "El grupo es obligatorio.",
                "ERR_GRUPO_INVALIDO",
                "El grupo debe ser valido."
        );
    }

    private static void validarIdentificador(
            final UUID valor,
            final String codigoNull,
            final String mensajeNull,
            final String codigoVacio,
            final String mensajeVacio
    ) {
        if (Objects.isNull(valor)) {
            throw new ValidationException(codigoNull, mensajeNull);
        }
        if (EMPTY_UUID.equals(valor)) {
            throw new ValidationException(codigoVacio, mensajeVacio);
        }
    }

    public UUID getDocente() {
        return docente;
    }

    public UUID getGrupo() {
        return grupo;
    }

}
