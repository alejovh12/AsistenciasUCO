package co.edu.uco.asistenciasuco.application.features.docente;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocenteDomainTest {

    private static final UUID DOCENTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    @Test
    void consultar_docente_por_id_valida_docente() {
        assertValidation("ERR_DOCENTE_REQUERIDO", () -> ConsultarDocentePorIdDomain.crear(null));
        assertValidation("ERR_DOCENTE_INVALIDO", () -> ConsultarDocentePorIdDomain.crear(EMPTY_UUID));
    }

    @Test
    void consultar_asignaciones_valida_docente() {
        assertValidation("ERR_DOCENTE_REQUERIDO", () -> ConsultarAsignacionesAcademicasDocenteDomain.crear(null));
        assertValidation("ERR_DOCENTE_INVALIDO", () -> ConsultarAsignacionesAcademicasDocenteDomain.crear(EMPTY_UUID));
    }

    @Test
    void construcciones_validas_preservan_valores() {
        final DocenteIdentidadEntity identidad =
                new DocenteIdentidadEntity(DOCENTE, USUARIO, 123456789, " Ana Perez ", true);

        assertEquals(DOCENTE, identidad.getId());
        assertEquals(USUARIO, identidad.getIdUsuario());
        assertEquals("Ana Perez", identidad.getNombreCompleto());
    }

    private void assertValidation(final String expectedCode, final Executable executable) {
        final ValidationException exception = assertThrows(ValidationException.class, executable);

        assertEquals(expectedCode, exception.getCode());
    }
}
