package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error;

import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbExceptionTranslatorTest {

    private static final String USER_MESSAGE = "Mensaje publico.";
    private static final String TECHNICAL_MESSAGE = "SQLException password token stackTrace";
    private static final String CORRELATION_ID = "93641bab-e3cd-485c-b275-47e7b731e18c";
    private static final String OPERATION = "operacionPrueba";
    private static final String SAFE_INTERNAL_MESSAGE = "No fue posible completar la operacion.";

    @ParameterizedTest
    @CsvSource({
            "La matricula ya se encuentra registrada para este grupo., ERR_MATRICULA_DUPLICADA",
            "No hay cupos disponibles para el grupo., ERR_CUPO_SUPERADO",
            "Existe cruce de horario para el estudiante., ERR_CRUCE_HORARIO_ESTUDIANTE",
            "Existe cruce de horario para el docente., ERR_CRUCE_HORARIO_DOCENTE",
            "El correo ya se encuentra registrado., ERR_UNICIDAD_CORREO",
            "El numero de identificacion ya existe., ERR_UNICIDAD_DOCUMENTO"
    })
    void mensajes_conocidos_de_conflicto_lanzan_conflictException_con_codigo_especifico(
            final String userMessage,
            final String expectedCode
    ) {
        final ConflictException exception = assertThrows(ConflictException.class, () -> translateFailure(userMessage, OPERATION));

        assertEquals(expectedCode, exception.getCode());
        assertEquals(userMessage, exception.getMessage());
    }

    @Test
    void cruce_horario_sin_actor_en_operacion_asignar_docente_lanza_codigo_docente() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("Existe cruce de horario.", "asignarDocenteAGrupo")
        );

        assertEquals("ERR_CRUCE_HORARIO_DOCENTE", exception.getCode());
    }

    @Test
    void cruce_horario_sin_actor_en_registro_estudiante_lanza_codigo_estudiante() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("Existe cruce de horario.", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_CRUCE_HORARIO_ESTUDIANTE", exception.getCode());
    }

    @Test
    void cupo_lleno_en_mensaje_tecnico_lanza_cupo_superado_sin_exponer_mensaje_tecnico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("No fue posible registrar el estudiante.", "Cupo lleno", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_CUPO_SUPERADO", exception.getCode());
        assertEquals("No fue posible registrar el estudiante.", exception.getMessage());
    }

    @Test
    void mensaje_real_tamaoo_moximo_lanza_cupo_superado() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El grupo supero su tamaoo moximo.", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_CUPO_SUPERADO", exception.getCode());
    }

    @Test
    void grupo_no_habilitado_lanza_conflictException_con_codigo_especifico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El grupo no se encuentra habilitado.", "registrarEstudianteEnGrupo")
        );

        assertEquals("ERR_GRUPO_NO_HABILITADO", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "El usuario se encuentra inactivo., ERR_USUARIO_INACTIVO",
            "El estudiante se encuentra inactivo., ERR_ESTUDIANTE_INACTIVO",
            "El docente se encuentra inactivo., ERR_DOCENTE_INACTIVO",
            "El usuario ya se encuentra registrado como docente., ERR_DOCENTE_YA_REGISTRADO"
    })
    void mensajes_reales_inactividad_y_docente_duplicado_lanzan_conflictException_con_codigo_especifico(
            final String userMessage,
            final String expectedCode
    ) {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure(userMessage, "asignarDocenteAGrupo")
        );

        assertEquals(expectedCode, exception.getCode());
        assertEquals(userMessage, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "El primer nombre contiene caracteres no permitidos.",
            "El primer apellido tiene formato invalido."
    })
    void mensajes_reales_nombre_persona_invalido_lanzan_validationException(final String userMessage) {
        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> translateFailure(userMessage, "registrarDocenteDesdeUsuario")
        );

        assertEquals("ERR_NOMBRE_PERSONA_INVALIDO", exception.getCode());
        assertEquals(userMessage, exception.getMessage());
    }

    @Test
    void mensaje_real_tipo_identificacion_con_interrogacion_lanza_notFound_con_codigo_especifico() {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure(
                        "El tipo de identificaci?n seleccionado no existe en el sistema.",
                        "Validacion en uv_tipo_identificacion",
                        "registrarEstudianteEnGrupo"
                )
        );

        assertEquals("ERR_TIPO_IDENTIFICACION_NO_EXISTE", exception.getCode());
    }

    @ParameterizedTest
    @CsvSource({
            "El usuario no existe., ERR_USUARIO_NO_EXISTE",
            "El estudiante no existe., ERR_ESTUDIANTE_NO_EXISTE",
            "El docente no existe., ERR_DOCENTE_NO_EXISTE",
            "El grupo no existe., ERR_GRUPO_NO_EXISTE",
            "El tipo de identificacion no existe., ERR_TIPO_IDENTIFICACION_NO_EXISTE",
            "La sesion no existe., ERR_SESION_NO_EXISTE"
    })
    void mensajes_conocidos_no_encontrado_lanzan_resourceNotFoundException_con_codigo_especifico(
            final String userMessage,
            final String expectedCode
    ) {
        final ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> translateFailure(userMessage, OPERATION)
        );

        assertEquals(expectedCode, exception.getCode());
    }

    @Test
    void mensaje_forbidden_lanza_forbiddenException_con_codigo_especifico() {
        final ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> translateFailure("El estudiante no pertenece a la sesion.", OPERATION)
        );

        assertEquals("ERR_ESTUDIANTE_NO_PERTENECE_SESION", exception.getCode());
    }

    @Test
    void mensaje_desconocido_lanza_internalApplicationException_con_err_db_unclassified() {
        final InternalApplicationException exception = assertThrows(
                InternalApplicationException.class,
                () -> translateFailure("La operacion no pudo completarse por una regla nueva.", OPERATION)
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(SAFE_INTERNAL_MESSAGE, exception.getMessage());
    }

    @Test
    void mensaje_null_lanza_internalApplicationException_con_err_db_unclassified() {
        final InternalApplicationException exception = assertThrows(
                InternalApplicationException.class,
                () -> translateFailure(null, OPERATION)
        );

        assertEquals("ERR_DB_UNCLASSIFIED", exception.getCode());
        assertEquals(SAFE_INTERNAL_MESSAGE, exception.getMessage());
    }

    @Test
    void mensaje_usuario_null_o_vacio_usa_fallback_seguro_sin_mensaje_tecnico() {
        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> translateFailure("El correo ya existe.", OPERATION)
        );

        assertEquals("ERR_UNICIDAD_CORREO", exception.getCode());
        assertEquals("El correo ya existe.", exception.getMessage());
    }

    @Test
    void mensaje_tecnico_no_se_usa_como_mensaje_publico_de_la_excepcion() {
        final InternalApplicationException exception = assertThrows(
                InternalApplicationException.class,
                () -> translateFailure(null, OPERATION)
        );

        assertEquals(SAFE_INTERNAL_MESSAGE, exception.getMessage());
    }

    @Test
    void estado_exitoso_no_lanza_excepcion() {
        assertDoesNotThrow(() -> DbExceptionTranslator.throwIfFailed(
                true,
                USER_MESSAGE,
                TECHNICAL_MESSAGE,
                CORRELATION_ID,
                OPERATION
        ));
    }

    private void translateFailure(final String userMessage, final String operation) {
        translateFailure(userMessage, TECHNICAL_MESSAGE, operation);
    }

    private void translateFailure(final String userMessage, final String technicalMessage, final String operation) {
        DbExceptionTranslator.throwIfFailed(
                false,
                userMessage,
                technicalMessage,
                CORRELATION_ID,
                operation
        );
    }
}
