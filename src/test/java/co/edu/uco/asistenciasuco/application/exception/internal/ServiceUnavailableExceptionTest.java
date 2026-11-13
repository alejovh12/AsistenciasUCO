package co.edu.uco.asistenciasuco.application.exception.internal;

import co.edu.uco.asistenciasuco.application.features.archivo.exception.ArchivoErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * VAL H3 y MSG-03: la excepcion de dependencia no disponible conserva la causa, solo acepta definiciones 503 y sigue siendo un
 * fallo interno; ERR_CORREO_REQUERIDO_MATRICULA conserva el codigo publico FIELD_REQUIRED y busca su texto por su propio codigo.
 */
class ServiceUnavailableExceptionTest {

    @Test
    void conserva_definicion_codigo_y_causa() {
        final RuntimeException causa = new IllegalStateException("clamd caido");

        final ServiceUnavailableException error =
                new ServiceUnavailableException(ArchivoErrorCode.ERR_ANTIVIRUS_NO_DISPONIBLE, causa);

        assertEquals("ERR_ANTIVIRUS_NO_DISPONIBLE", error.getCode());
        assertSame(ArchivoErrorCode.ERR_ANTIVIRUS_NO_DISPONIBLE, error.getErrorDefinition().orElseThrow());
        assertSame(causa, error.getCause());
        assertEquals(ErrorKind.SERVICE_UNAVAILABLE, ArchivoErrorCode.ERR_ANTIVIRUS_NO_DISPONIBLE.kind());
        assertInstanceOf(InternalApplicationException.class, error, "Sigue siendo un fallo interno (fail-closed)");
    }

    @Test
    void rechaza_definiciones_que_no_son_503() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceUnavailableException(CommonErrorCode.INTERNAL_ERROR, new RuntimeException()));
        assertThrows(NullPointerException.class, () -> new ServiceUnavailableException(null, new RuntimeException()));
    }

    @Test
    void la_matricula_sin_correo_conserva_field_required_y_busca_su_texto_por_su_codigo() {
        assertEquals("FIELD_REQUIRED", GrupoErrorCode.ERR_CORREO_REQUERIDO_MATRICULA.code());
        assertEquals("ERR_CORREO_REQUERIDO_MATRICULA", GrupoErrorCode.ERR_CORREO_REQUERIDO_MATRICULA.messageCode());
        assertEquals("ERR_GRUPO_REQUERIDO", GrupoErrorCode.ERR_GRUPO_REQUERIDO.messageCode());
    }
}
