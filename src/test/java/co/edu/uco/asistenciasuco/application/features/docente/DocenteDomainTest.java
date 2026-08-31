package co.edu.uco.asistenciasuco.application.features.docente;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocenteporid.usecase.domain.ConsultarDocentePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.usecase.domain.RegistrarDocenteDesdeUsuarioDomain;
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
    void registrar_docente_desde_usuario_valida_usuario() {
        assertValidation("ERR_USUARIO_REQUERIDO", () -> RegistrarDocenteDesdeUsuarioDomain.crear(null));
        assertValidation("ERR_USUARIO_INVALIDO", () -> RegistrarDocenteDesdeUsuarioDomain.crear(EMPTY_UUID));
    }

    @Test
    void asignar_docente_a_grupo_valida_docente_y_grupo() {
        assertValidation("ERR_DOCENTE_REQUERIDO", () -> AsignarDocenteAGrupoDomain.crear(null, GRUPO));
        assertValidation("ERR_DOCENTE_INVALIDO", () -> AsignarDocenteAGrupoDomain.crear(EMPTY_UUID, GRUPO));
        assertValidation("ERR_GRUPO_REQUERIDO", () -> AsignarDocenteAGrupoDomain.crear(DOCENTE, null));
        assertValidation("ERR_GRUPO_INVALIDO", () -> AsignarDocenteAGrupoDomain.crear(DOCENTE, EMPTY_UUID));
    }

    @Test
    void construcciones_validas_preservan_valores() {
        final DocenteIdentidadEntity identidad =
                new DocenteIdentidadEntity(DOCENTE, USUARIO, 123456789, " Ana Perez ", true);
        final AsignarDocenteAGrupoDomain asignacion = AsignarDocenteAGrupoDomain.crear(
                DOCENTE,
                GRUPO
        );

        assertEquals(DOCENTE, identidad.getId());
        assertEquals(USUARIO, identidad.getIdUsuario());
        assertEquals("Ana Perez", identidad.getNombreCompleto());
        assertEquals(DOCENTE, asignacion.getDocente());
        assertEquals(GRUPO, asignacion.getGrupo());
    }

    private void assertValidation(final String expectedCode, final Executable executable) {
        final ValidationException exception = assertThrows(ValidationException.class, executable);

        assertEquals(expectedCode, exception.getCode());
    }
}
