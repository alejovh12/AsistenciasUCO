package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Certifica el borde REAL entre el backend y {@code gestionasistenciadb}:
 * {@code Java -> AsistenciaRepositoryPort -> CanonicalStoredProcedureExecutor -> SQL Server ->
 * usp_registrar_asistencias_sesion}.
 *
 * <p>No mockea JDBC/DataSource/adapter: usa el contexto Spring real contra SQL Server real (perfil
 * {@code integration}). Reutiliza fixture DEV estable (un grupo habilitado con >=3 estudiantes
 * matriculados y su docente titular, resueltos dinamicamente via {@code assumeTrue}, sin
 * hardcodear IDs) en vez de crear Usuario/Docente/Grupo nuevos: {@code DocenteRepositoryPort
 * .registrarDocenteDesdeUsuario} esta deshabilitado en este backend (bloqueado por DB, sin
 * command publico), por lo que un Docente nuevo no puede crearse via adapter en esta fase.</p>
 *
 * <p>Cada test crea una {@code Sesion} nueva y unica (via {@code SesionRepositoryPort
 * .crearSesion}, tambien real) y la limpia en {@link #limpiarFixture()}. {@code
 * usp_registrar_asistencias_sesion} gestiona su propia transaccion (COMMIT/ROLLBACK internos);
 * por eso el cleanup es DELETE explicito y no un rollback de Spring, que no revertiria el commit
 * ya ejecutado dentro del SP (ver instrucciones de la microfase).</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AsistenciaRepositorySqlServerIT {

    @Autowired
    private AsistenciaRepositoryPort asistenciaRepositoryPort;

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID grupoId;
    private UUID docenteId;
    private UUID docenteAUsuarioId;
    private UUID docenteBUsuarioId;
    private List<UUID> estudiantes;
    private UUID sesionId;

    @BeforeEach
    void prepararFixtureYSesion() {
        final Optional<GrupoConDocente> grupo = grupoHabilitadoConAlMenos3Matriculados();
        assumeTrue(grupo.isPresent(), "No hay grupo habilitado con >=3 estudiantes matriculados para certificar esta IT.");
        this.grupoId = grupo.get().grupoId();
        this.docenteId = grupo.get().docenteId();
        this.docenteAUsuarioId = grupo.get().usuarioDocente();

        final Optional<UUID> docenteAjeno = docenteActivoDistintoDe(docenteId);
        assumeTrue(docenteAjeno.isPresent(), "No hay un segundo docente activo distinto del titular para probar el caso ajeno.");
        this.docenteBUsuarioId = docenteAjeno.get();

        this.estudiantes = estudiantesMatriculados(grupoId, 3);
        assumeTrue(estudiantes.size() == 3, "No fue posible resolver 3 estudiantes matriculados en el grupo candidato.");

        CorrelationIdContext.set(UUID.randomUUID());
        this.sesionId = crearSesionDePrueba();
    }

    @AfterEach
    void limpiarFixture() {
        if (sesionId != null) {
            jdbcTemplate.update(
                    "DELETE FROM dbo.DetalleAsistencia WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion = ?)",
                    sesionId.toString()
            );
            jdbcTemplate.update("DELETE FROM dbo.Asistencia WHERE sesion = ?", sesionId.toString());
            jdbcTemplate.update("DELETE FROM dbo.Sesion WHERE id = ?", sesionId.toString());
        }
        CorrelationIdContext.clear();
    }

    @Test
    void ownerBatch_docente_propietario_registra_AN_SJC_EX_exitosamente() {
        CorrelationIdContext.set(UUID.randomUUID());

        asistenciaRepositoryPort.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                sesionId,
                List.of(
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN"),
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(1), "SJC"),
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(2), "EX")
                ),
                docenteAUsuarioId
        ));

        final Map<UUID, AsistenciaRepositoryProjection> resultado = consultarPorEstudiante();
        assertEquals(3, resultado.size(), "El lote completo debe quedar persistido para los 3 estudiantes.");
    }

    @Test
    void roundtrip_AN_SJC_EX_exacto_incluyendo_EX_no_reconstruido_como_SJC() {
        CorrelationIdContext.set(UUID.randomUUID());
        asistenciaRepositoryPort.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                sesionId,
                List.of(
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN"),
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(1), "SJC"),
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(2), "EX")
                ),
                docenteAUsuarioId
        ));

        final Map<UUID, AsistenciaRepositoryProjection> resultado = consultarPorEstudiante();

        assertEquals("AN", resultado.get(estudiantes.get(0)).getEstado());
        assertEquals("SJC", resultado.get(estudiantes.get(1)).getEstado());

        final AsistenciaRepositoryProjection excusado = resultado.get(estudiantes.get(2));
        assertEquals("EX", excusado.getEstado());
        assertFalse(excusado.isPresente(), "EX debe reportar presente=false.");
        assertNotEquals("SJC", excusado.getEstado(), "EX no debe reconstruirse como SJC en la salida real del adapter.");
    }

    @Test
    void usuarioEjecutor_usado_es_usuario_id_y_no_coincide_con_docente_id() {
        assertNotEquals(
                docenteId,
                docenteAUsuarioId,
                "Usuario.id y Docente.id del fixture no deben coincidir; de lo contrario esta IT no distinguiria un bypass real."
        );
    }

    @Test
    void docente_ajeno_es_rechazado_por_la_db_sin_persistir_cambios() {
        CorrelationIdContext.set(UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> asistenciaRepositoryPort.registrarAsistenciasSesion(
                new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN")),
                        docenteBUsuarioId
                )
        ));

        assertEquals(0, consultarPorEstudiante().size(), "Un docente ajeno no debe dejar cambios persistidos, ni siquiera parciales.");
    }

    @Test
    void estado_invalido_ABC_es_rechazado_como_validacion_y_no_crea_razonCausa_dinamica() {
        CorrelationIdContext.set(UUID.randomUUID());
        final Integer razonCausaAntes = contarRazonCausa("ABC");

        final ValidationException exception = assertThrows(ValidationException.class, () -> asistenciaRepositoryPort.registrarAsistenciasSesion(
                new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "ABC")),
                        docenteAUsuarioId
                )
        ));

        assertEquals(AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO.code(), exception.getCode());
        assertEquals(0, consultarPorEstudiante().size(), "Un estado invalido no debe persistir ningun cambio.");
        assertEquals(razonCausaAntes, contarRazonCausa("ABC"), "El backend no debe crear codigos de RazonCausa dinamicamente.");
    }

    @Test
    void lote_mixto_con_un_estado_invalido_es_atomico_cero_cambios_parciales() {
        CorrelationIdContext.set(UUID.randomUUID());
        final Map<UUID, AsistenciaRepositoryProjection> antes = consultarPorEstudiante();
        assertEquals(0, antes.size(), "Estado previo esperado vacio para esta sesion recien creada; se compara explicitamente igual.");

        assertThrows(RuntimeException.class, () -> asistenciaRepositoryPort.registrarAsistenciasSesion(
                new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(
                                new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN"),
                                new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(1), "ABC"),
                                new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(2), "EX")
                        ),
                        docenteAUsuarioId
                )
        ));

        final Map<UUID, AsistenciaRepositoryProjection> despues = consultarPorEstudiante();
        assertEquals(antes.size(), despues.size(), "El lote mixto debe fallar completo: ni AN ni EX deben quedar persistidos.");
        assertTrue(despues.isEmpty());
    }

    private Map<UUID, AsistenciaRepositoryProjection> consultarPorEstudiante() {
        return asistenciaRepositoryPort.consultarAsistenciasPorGrupo(
                        new ConsultarAsistenciasPorGrupoRepositoryDTO(grupoId, sesionId)
                ).stream()
                .filter(projection -> estudiantes.contains(projection.getEstudiante()))
                .collect(java.util.stream.Collectors.toMap(AsistenciaRepositoryProjection::getEstudiante, p -> p));
    }

    private UUID crearSesionDePrueba() {
        final String nombreUnico = "IT-Asist-" + UUID.randomUUID().toString().substring(0, 12);
        final LocalDateTime inicio = LocalDateTime.now().minusHours(1);
        sesionRepositoryPort.crearSesion(new CrearSesionRepositoryDTO(
                grupoId,
                nombreUnico,
                "Sesion de certificacion de integracion (IT), eliminada al finalizar.",
                inicio,
                inicio.plusHours(2),
                null,
                "PRESENCIAL",
                docenteId,
                docenteAUsuarioId
        ));

        final List<UUID> encontrada = jdbcTemplate.query(
                "SELECT id FROM dbo.Sesion WHERE grupo = ? AND nombre = ?",
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id"))),
                grupoId.toString(),
                nombreUnico
        );
        assertEquals(1, encontrada.size(), "La sesion de prueba recien creada debe ser resoluble de forma unica por nombre.");
        return encontrada.get(0);
    }

    private Integer contarRazonCausa(final String codigo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.RazonCausa WHERE codigo = ?",
                Integer.class,
                codigo
        );
    }

    private Optional<GrupoConDocente> grupoHabilitadoConAlMenos3Matriculados() {
        final List<GrupoConDocente> candidatos = jdbcTemplate.query("""
                        SELECT g.id AS grupoId, g.idDocente AS docenteId, di.idUsuario AS usuarioDocente
                        FROM dbo.uv_grupo g
                        INNER JOIN dbo.uv_docente_identidad di ON di.id = g.idDocente
                        WHERE g.grupoEstaHablitado = 1
                          AND (
                              SELECT COUNT(*) FROM dbo.uv_estudiante_grupo eg
                              WHERE eg.idGrupo = g.id AND eg.codigoEstadoEstudiante = 'A'
                          ) >= 3
                        ORDER BY g.id
                        """,
                (resultSet, rowNumber) -> new GrupoConDocente(
                        UUID.fromString(String.valueOf(resultSet.getObject("grupoId"))),
                        UUID.fromString(String.valueOf(resultSet.getObject("docenteId"))),
                        UUID.fromString(String.valueOf(resultSet.getObject("usuarioDocente")))
                )
        );
        return candidatos.stream().findFirst();
    }

    private Optional<UUID> docenteActivoDistintoDe(final UUID docenteTitularId) {
        final List<UUID> candidatos = jdbcTemplate.query(
                """
                        SELECT TOP 1 idUsuario
                        FROM dbo.uv_docente_identidad
                        WHERE id <> ? AND estaActivoUsuario = 1
                        ORDER BY id
                        """,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("idUsuario"))),
                docenteTitularId.toString()
        );
        return candidatos.stream().findFirst();
    }

    private List<UUID> estudiantesMatriculados(final UUID grupo, final int maximo) {
        return jdbcTemplate.query(
                "SELECT TOP (?) idEstudiante FROM dbo.uv_estudiante_grupo WHERE idGrupo = ? AND codigoEstadoEstudiante = 'A' ORDER BY idEstudiante",
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("idEstudiante"))),
                maximo,
                grupo.toString()
        );
    }

    private record GrupoConDocente(UUID grupoId, UUID docenteId, UUID usuarioDocente) {
    }
}
