package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain;




import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.exception.DocenteErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

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
                DocenteErrorCode.ERR_DOCENTE_REQUERIDO,
                DocenteErrorCode.ERR_DOCENTE_INVALIDO
        );
    }

    private static void validarGrupo(final UUID grupo) {
        validarIdentificador(
                grupo,
                GrupoErrorCode.ERR_GRUPO_REQUERIDO,
                GrupoErrorCode.ERR_GRUPO_INVALIDO
        );
    }

    private static void validarIdentificador(
            final UUID valor,
            final ErrorDefinition codigoNull,
            final ErrorDefinition codigoVacio
    ) {
        if (Objects.isNull(valor)) {
            throw new ValidationException(codigoNull);
        }
        if (EMPTY_UUID.equals(valor)) {
            throw new ValidationException(codigoVacio);
        }
    }

    public UUID getDocente() {
        return docente;
    }

    public UUID getGrupo() {
        return grupo;
    }

}
