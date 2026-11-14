package co.edu.uco.asistenciasuco.application.features.grupo.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorSpec;
import co.edu.uco.asistenciasuco.crosscutting.exception.SpecErrorDefinition;

public enum GrupoErrorCode implements SpecErrorDefinition {
    ERR_MATRICULA_DUPLICADA(new ErrorSpec("ERR_MATRICULA_DUPLICADA", "La matricula ya se encuentra registrada para este grupo.", ErrorKind.CONFLICT)),
    ERR_CUPO_SUPERADO(new ErrorSpec("ERR_CUPO_SUPERADO", "No hay cupos disponibles para el grupo.", ErrorKind.CONFLICT)),
    ERR_CRUCE_HORARIO_ESTUDIANTE(new ErrorSpec("ERR_CRUCE_HORARIO_ESTUDIANTE", "Existe cruce de horario para el estudiante.", ErrorKind.CONFLICT)),
    ERR_CRUCE_HORARIO_DOCENTE(new ErrorSpec("ERR_CRUCE_HORARIO_DOCENTE", "Existe cruce de horario para el docente.", ErrorKind.CONFLICT)),
    ERR_GRUPO_NO_EXISTE(new ErrorSpec("ERR_GRUPO_NO_EXISTE", "El grupo consultado no existe.", ErrorKind.NOT_FOUND)),
    ERR_GRUPO_NO_HABILITADO(new ErrorSpec("ERR_GRUPO_NO_HABILITADO", "El grupo no se encuentra habilitado.", ErrorKind.CONFLICT)),
    /** DEC-073: el código del grupo es un entero mayor que cero (usp_crear_grupo / usp_actualizar_grupo). */
    ERR_CODIGO_GRUPO_INVALIDO(new ErrorSpec("ERR_CODIGO_GRUPO_INVALIDO", "El código del grupo debe ser un número entero mayor que cero.", ErrorKind.VALIDATION)),
    ERR_GRUPO_REQUERIDO(new ErrorSpec("ERR_GRUPO_REQUERIDO", "El grupo es obligatorio.", ErrorKind.VALIDATION)),
    ERR_GRUPO_INVALIDO(new ErrorSpec("ERR_GRUPO_INVALIDO", "El identificador del grupo no es valido.", ErrorKind.VALIDATION)),
    // CULM-001 A.2-15: matricula manual sin correo de un documento que no resuelve a un estudiante matriculable (400, correo).
    ERR_CORREO_REQUERIDO_MATRICULA(new ErrorSpec("FIELD_REQUIRED", "El correo es obligatorio: no hay un estudiante registrado que pueda matricular con ese documento.", ErrorKind.VALIDATION));

    private final ErrorSpec spec;

    GrupoErrorCode(final ErrorSpec spec) {
        this.spec = spec;
    }

    @Override
    public ErrorSpec spec() {
        return spec;
    }

    /**
     * VAL MSG-03: {@link #ERR_CORREO_REQUERIDO_MATRICULA} conserva el codigo publico {@code FIELD_REQUIRED} (contrato A.2-15), pero su
     * texto se busca en el catalogo por su propio codigo; asi no se reserva el codigo generico FIELD_REQUIRED para un texto de
     * matricula. En las demas constantes el codigo de mensaje es el mismo codigo publico.
     */
    @Override
    public String messageCode() {
        return this == ERR_CORREO_REQUERIDO_MATRICULA ? name() : spec.code();
    }
}
