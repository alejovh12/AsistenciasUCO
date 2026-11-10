package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort.CrearCoordinadorConProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort.CrearDecanoConFacultadCommand;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DEC-065 (5): {@code usp_crear_decano_con_facultad} y {@code usp_crear_coordinador_con_programa} contra SQL Server real, invocados
 * por los adaptadores JPA con {@code EXEC} directo. Cada prueba corre en una transaccion que se revierte al terminar (los SP usan
 * savepoint cuando el llamador ya tiene transaccion), de modo que no queda ningun dato. Se verifica: ids y nombres devueltos, filas
 * creadas, y que un rechazo (facultad o programa repetido, ejecutor sin permiso, institucion inexistente, entrada invalida) no deja la
 * persona creada.
 */
@Tag("integration")
@SpringBootTest
@Transactional
@MockitoBean(types = JwtDecoder.class)
class AltaCompuestaSpIT {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ0123456";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DecanoJpaRepository decanos;
    @Autowired
    private CoordinadorJpaRepository coordinadores;

    private UUID admin;
    private UUID decanoEjecutor;
    private UUID facultad;
    private UUID institucion;
    private UUID tipoPrograma;
    private String nombreFacultadExistente;
    private String nombreProgramaExistente;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        admin = uuid("SELECT CAST(id AS NVARCHAR(36)) FROM dbo.Usuario WHERE correo = ?", AcademicUserParityFixture.EMAIL_ADMIN_EJECUTOR);
        decanoEjecutor = uuid("SELECT CAST(id AS NVARCHAR(36)) FROM dbo.Usuario WHERE correo = ?", AcademicUserParityFixture.EMAIL_DECANO_EJECUTOR);
        facultad = uuid("""
                SELECT CAST(f.id AS NVARCHAR(36)) FROM dbo.Facultad f
                INNER JOIN dbo.Decano d ON d.id = f.decano
                INNER JOIN dbo.Usuario u ON u.id = d.usuario WHERE u.correo = ?
                """, AcademicUserParityFixture.EMAIL_DECANO_EJECUTOR);
        institucion = uuid("SELECT TOP 1 CAST(id AS NVARCHAR(36)) FROM dbo.uv_institucion ORDER BY id");
        tipoPrograma = uuid("SELECT TOP 1 CAST(id AS NVARCHAR(36)) FROM dbo.uv_tipo_programa WHERE estaActivoTipoPrograma = 1 ORDER BY id");
        nombreFacultadExistente = jdbc.queryForObject("SELECT nombreFacultad FROM dbo.uv_facultad WHERE id = ?", String.class, facultad.toString());
        nombreProgramaExistente = jdbc.queryForObject(
                "SELECT TOP 1 nombrePrograma FROM dbo.uv_programa WHERE idFacultad = ? ORDER BY id", String.class, facultad.toString());
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    // ---- decano con facultad

    @Test
    void decano_con_facultad_crea_persona_rol_y_facultad_y_devuelve_los_ids() {
        final String correo = correoUnico();
        final String nombre = "Facultad IT " + sufijo();

        final var resultado = decanos.crearDecanoConFacultad(decano(correo, nombre, institucion, admin));

        assertNotNull(resultado.idDecano());
        assertNotNull(resultado.idFacultad());
        assertEquals(nombre, resultado.nombreFacultad());
        assertEquals(1, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
        assertEquals(1, contar("""
                SELECT COUNT(*) FROM dbo.Facultad f INNER JOIN dbo.Decano d ON d.id = f.decano INNER JOIN dbo.Usuario u ON u.id = d.usuario
                WHERE f.id = ? AND d.id = ? AND u.correo = ?
                """, resultado.idFacultad().toString(), resultado.idDecano().toString(), correo));
    }

    @Test
    void decano_con_facultad_repetida_es_conflicto_y_no_deja_persona() {
        final String correo = correoUnico();

        assertThrows(ConflictException.class,
                () -> decanos.crearDecanoConFacultad(decano(correo, nombreFacultadExistente, institucion, admin)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    @Test
    void decano_con_facultad_e_institucion_inexistente_es_404_y_no_deja_persona() {
        final String correo = correoUnico();

        assertThrows(ResourceNotFoundException.class,
                () -> decanos.crearDecanoConFacultad(decano(correo, "Facultad IT " + sufijo(), UUID.randomUUID(), admin)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    @Test
    void decano_con_facultad_de_nombre_corto_es_validacion_y_no_deja_persona() {
        final String correo = correoUnico();

        assertThrows(ValidationException.class, () -> decanos.crearDecanoConFacultad(decano(correo, "AB", institucion, admin)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    @Test
    void decano_con_facultad_por_quien_no_es_administrador_es_prohibido_y_no_deja_persona() {
        final String correo = correoUnico();

        assertThrows(ForbiddenException.class,
                () -> decanos.crearDecanoConFacultad(decano(correo, "Facultad IT " + sufijo(), institucion, decanoEjecutor)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    // ---- coordinador con programa

    @Test
    void coordinador_con_programa_crea_persona_rol_y_programa_y_devuelve_los_ids() {
        final String correo = correoUnico();
        final String nombre = "Programa IT " + sufijo();

        final var resultado = coordinadores.crearCoordinadorConPrograma(coordinador(correo, nombre, facultad, decanoEjecutor));

        assertNotNull(resultado.idCoordinador());
        assertNotNull(resultado.idPrograma());
        assertEquals(nombre, resultado.nombrePrograma());
        assertEquals(1, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
        assertEquals(1, contar("""
                SELECT COUNT(*) FROM dbo.Programa p INNER JOIN dbo.Coordinador c ON c.id = p.coordinador INNER JOIN dbo.Usuario u ON u.id = c.usuario
                WHERE p.id = ? AND c.id = ? AND p.facultad = ? AND u.correo = ?
                """, resultado.idPrograma().toString(), resultado.idCoordinador().toString(), facultad.toString(), correo));
    }

    @Test
    void coordinador_con_programa_repetido_es_conflicto_y_no_deja_persona() {
        final String correo = correoUnico();

        assertThrows(ConflictException.class,
                () -> coordinadores.crearCoordinadorConPrograma(coordinador(correo, nombreProgramaExistente, facultad, decanoEjecutor)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    @Test
    void coordinador_con_programa_por_quien_no_es_decano_es_prohibido_y_no_deja_persona() {
        final String correo = correoUnico();

        assertThrows(ForbiddenException.class,
                () -> coordinadores.crearCoordinadorConPrograma(coordinador(correo, "Programa IT " + sufijo(), facultad, admin)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    @Test
    void coordinador_con_programa_en_otra_facultad_es_prohibido_y_no_deja_persona() {
        final String correo = correoUnico();
        // Otra facultad real, creada en la misma transaccion revertida con el propio alta compuesta del administrador.
        final UUID otraFacultad = decanos.crearDecanoConFacultad(
                decano(correoUnico(), "Facultad IT " + sufijo(), institucion, admin)).idFacultad();

        assertThrows(ForbiddenException.class,
                () -> coordinadores.crearCoordinadorConPrograma(coordinador(correo, "Programa IT " + sufijo(), otraFacultad, decanoEjecutor)));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    @Test
    void coordinador_con_programa_de_tipo_inexistente_es_validacion_y_no_deja_persona() {
        final String correo = correoUnico();
        final var comando = new CrearCoordinadorConProgramaCommand(UUID.randomUUID(), null, String.valueOf(numero()), "Ana", null, "Perez", null, correo,
                HASH, "Programa IT " + sufijo(), UUID.randomUUID(), null, null, facultad, decanoEjecutor);

        assertThrows(ValidationException.class, () -> coordinadores.crearCoordinadorConPrograma(comando));

        assertEquals(0, contar("SELECT COUNT(*) FROM dbo.Usuario WHERE correo = ?", correo));
    }

    // ---- utilidades

    private CrearDecanoConFacultadCommand decano(final String correo, final String nombreFacultad, final UUID idInstitucion,
                                                 final UUID ejecutor) {
        return new CrearDecanoConFacultadCommand(UUID.randomUUID(), null, numero(), "Ana", null, "Perez", null, correo, HASH,
                nombreFacultad, null, idInstitucion, ejecutor);
    }

    private CrearCoordinadorConProgramaCommand coordinador(final String correo, final String nombrePrograma, final UUID idFacultad,
                                                           final UUID ejecutor) {
        return new CrearCoordinadorConProgramaCommand(UUID.randomUUID(), null, String.valueOf(numero()), "Ana", null, "Perez", null,
                correo, HASH, nombrePrograma, tipoPrograma, null, null, idFacultad, ejecutor);
    }

    private UUID uuid(final String sql, final Object... argumentos) {
        return UUID.fromString(jdbc.queryForObject(sql, String.class, argumentos));
    }

    private int contar(final String sql, final Object... argumentos) {
        return jdbc.queryForObject(sql, Integer.class, argumentos);
    }

    private static int numero() {
        return ThreadLocalRandom.current().nextInt(700_000_000, 799_999_999);
    }

    private static String sufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String correoUnico() {
        return "it-dec065-" + sufijo() + "@example.test";
    }
}
