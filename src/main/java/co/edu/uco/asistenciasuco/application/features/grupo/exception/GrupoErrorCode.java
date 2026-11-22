package co.edu.uco.asistenciasuco.application.features.grupo.exception;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorSpec;
import co.edu.uco.asistenciasuco.crosscutting.exception.SpecErrorDefinition;

public enum GrupoErrorCode implements SpecErrorDefinition {
    ERR_MATRICULA_DUPLICADA(new ErrorSpec("ERR_MATRICULA_DUPLICADA", "Usted ya se encuentra registrado o matriculado en este grupo.", ErrorKind.CONFLICT)),
    ERR_CUPO_SUPERADO(new ErrorSpec("ERR_CUPO_SUPERADO", "El grupo ha superado la capacidad maxima de estudiantes permitida.", ErrorKind.CONFLICT)),
    ERR_CRUCE_HORARIO_ESTUDIANTE(new ErrorSpec("ERR_CRUCE_HORARIO_ESTUDIANTE", "No es posible realizar el registro. Existe un cruce de horario con otra asignatura.", ErrorKind.CONFLICT)),
    ERR_CRUCE_HORARIO_DOCENTE(new ErrorSpec("ERR_CRUCE_HORARIO_DOCENTE", "Existe un cruce de horario para el docente en la misma franja horaria.", ErrorKind.CONFLICT)),
    ERR_GRUPO_NO_EXISTE(new ErrorSpec("ERR_GRUPO_NO_EXISTE", "El grupo seleccionado no existe o no esta habilitado.", ErrorKind.NOT_FOUND)),
    ERR_GRUPO_NO_HABILITADO(new ErrorSpec("ERR_GRUPO_NO_HABILITADO", "El grupo seleccionado no se encuentra habilitado para matricula.", ErrorKind.CONFLICT)),
    /** DEC-073: el código del grupo es un entero mayor que cero (usp_crear_grupo / usp_actualizar_grupo). */
    ERR_CODIGO_GRUPO_INVALIDO(new ErrorSpec("ERR_CODIGO_GRUPO_INVALIDO", "El código del grupo debe ser un número entero mayor que cero.", ErrorKind.VALIDATION)),
    ERR_GRUPO_REQUERIDO(new ErrorSpec("ERR_GRUPO_REQUERIDO", "El grupo es obligatorio.", ErrorKind.VALIDATION)),
    ERR_GRUPO_INVALIDO(new ErrorSpec("ERR_GRUPO_INVALIDO", "El identificador del grupo no es válido.", ErrorKind.VALIDATION)),
    // CULM-001 A.2-15: matricula manual sin correo de un documento que no resuelve a un estudiante matriculable (400, correo).
    ERR_CORREO_REQUERIDO_MATRICULA(new ErrorSpec("FIELD_REQUIRED", "El correo es obligatorio: no hay un estudiante registrado que se pueda matricular con ese documento.", ErrorKind.VALIDATION));

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
