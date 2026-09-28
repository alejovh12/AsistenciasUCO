package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato TARGET (LB-001B.4A): CrearSesionDomain(grupo, nombre, fechaHoraInicio, fechaHoraFin,
 * usuarioEjecutor). Sin tema ni docente. Errores de texto: ERR_NOMBRE_SESION_REQUERIDO /
 * ERR_NOMBRE_SESION_LONGITUD_INVALIDA (los codigos ERR_TEMA_* dejan de existir).
 * RED esperado: falla la compilacion (constructor de 5 parametros / getNombre inexistentes).
 */
class CrearSesionDomainTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO_EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 8, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Test
    void construye_domain_valido_y_normaliza_nombre() {
        final CrearSesionDomain domain =
                new CrearSesionDomain(GRUPO, "  Nombre principal  ", INICIO, FIN, USUARIO_EJECUTOR);

        assertEquals(GRUPO, domain.getGrupo());
        assertEquals("Nombre principal", domain.getNombre());
        assertEquals(INICIO, domain.getFechaHoraInicio());
        assertEquals(FIN, domain.getFechaHoraFin());
        assertEquals(USUARIO_EJECUTOR, domain.getUsuarioEjecutor());
    }

    @Test
    void rechaza_grupo_fechas_y_usuario_invalidos() {
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(
                null, "Nombre valido", INICIO, FIN, USUARIO_EJECUTOR));
        assertThrows(ValidationException.class, () -> new CrearSesionDomain(
                GRUPO, "Nombre valido", INICIO, FIN, null));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> new CrearSesionDomain(
                GRUPO, "Nombre valido", FIN, INICIO, USUARIO_EJECUTOR));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> new CrearSesionDomain(
                GRUPO, "Nombre valido", INICIO, INICIO, USUARIO_EJECUTOR));
    }

    @Test
    void nombre_ausente_o_en_blanco_usa_codigo_de_nombre() {
        assertCode("ERR_NOMBRE_SESION_REQUERIDO", () -> new CrearSesionDomain(
                GRUPO, null, INICIO, FIN, USUARIO_EJECUTOR));
        assertCode("ERR_NOMBRE_SESION_REQUERIDO", () -> new CrearSesionDomain(
                GRUPO, "   ", INICIO, FIN, USUARIO_EJECUTOR));
    }

    @Test
    void nombre_excesivo_usa_codigo_de_longitud_de_nombre() {
        assertCode("ERR_NOMBRE_SESION_LONGITUD_INVALIDA", () -> new CrearSesionDomain(
                GRUPO, "X".repeat(51), INICIO, FIN, USUARIO_EJECUTOR));
    }

    /** LB-001B.4B (TD-048): dbo.Sesion.nombre = NVARCHAR(50) -> acepta 1..50, rechaza 51+. */
    @Test
    void nombre_acepta_limites_1_y_50_y_rechaza_51_con_mensaje_50() {
        assertEquals("A", new CrearSesionDomain(GRUPO, "A", INICIO, FIN, USUARIO_EJECUTOR).getNombre());
        assertEquals("X".repeat(50),
                new CrearSesionDomain(GRUPO, "X".repeat(50), INICIO, FIN, USUARIO_EJECUTOR).getNombre());
        final ValidationException ex = assertThrows(ValidationException.class, () -> new CrearSesionDomain(
                GRUPO, "X".repeat(51), INICIO, FIN, USUARIO_EJECUTOR));
        assertEquals("ERR_NOMBRE_SESION_LONGITUD_INVALIDA", ex.getCode());
        assertEquals("El nombre de la sesion debe tener entre 1 y 50 caracteres.", ex.getMessage());
    }

    private static void assertCode(final String code, final Executable action) {
        assertEquals(code, assertThrows(ValidationException.class, action).getCode());
    }
}
