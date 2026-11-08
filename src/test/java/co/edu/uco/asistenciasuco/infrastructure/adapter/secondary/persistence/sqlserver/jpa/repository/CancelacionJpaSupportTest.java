package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadAlerta;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadResolutores;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CULM-001 C.1: lectura de las novedades JSON del motor y del job, construccion de EXEC sin nulos y conversiones de filas nativas. */
class CancelacionJpaSupportTest {

    @Test
    void exec_omite_los_parametros_nulos() {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", UUID.randomUUID());
        parametros.put("idEstudianteGrupo", null);
        parametros.put("idCorrelacion", UUID.randomUUID());
        assertEquals("EXEC dbo.usp_x @idGrupo = :idGrupo, @idCorrelacion = :idCorrelacion", CancelacionRowSupport.exec("usp_x", parametros));
        assertEquals("EXEC dbo.usp_y", CancelacionRowSupport.exec("usp_y", new LinkedHashMap<>()));
    }

    @Test
    void conversiones_de_valores_nativos() {
        final UUID id = UUID.randomUUID();
        assertNull(CancelacionRowSupport.texto(null));
        assertEquals("x", CancelacionRowSupport.texto("x"));
        assertNull(CancelacionRowSupport.uuid(null));
        assertEquals(id, CancelacionRowSupport.uuid(id.toString()));
        assertFalse(CancelacionRowSupport.bit(null));
        assertTrue(CancelacionRowSupport.bit(Boolean.TRUE));
        assertEquals(0, CancelacionRowSupport.entero(null));
        assertEquals(7, CancelacionRowSupport.entero(7));
        assertNull(CancelacionRowSupport.decimal(null));
        assertEquals(new BigDecimal("1.5"), CancelacionRowSupport.decimal(new BigDecimal("1.5")));
        assertEquals(new BigDecimal("2"), CancelacionRowSupport.decimal(2));
        assertEquals(new BigDecimal("3.25"), CancelacionRowSupport.decimal("3.25"));
        assertNull(CancelacionRowSupport.instante(null));
        assertEquals(Instant.parse("2026-11-08T10:00:00Z"), CancelacionRowSupport.instante(LocalDateTime.of(2026, 11, 8, 10, 0)));
        assertEquals(Instant.parse("2026-11-08T10:00:00Z"), CancelacionRowSupport.instante("2026-11-08 10:00:00"));
        assertTrue(CancelacionRowSupport.instante(Timestamp.valueOf(LocalDateTime.of(2026, 11, 8, 10, 0))) != null);
    }

    @Test
    void lee_las_novedades_del_motor() {
        final UUID alerta = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID eg = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        final String json = "[{\"idAlerta\":\"" + alerta + "\",\"codigoTipo\":\"PERDIDA\",\"nivel\":2,\"esPrincipal\":true,\"porcentajeInasistencia\":21.5,"
                + "\"umbral\":20,\"umbralPerdida\":20,\"horasFalta\":5,\"horasDictadas\":20,\"idEstudianteGrupo\":\"" + eg + "\",\"idGrupo\":\"" + grupo
                + "\",\"codigoGrupo\":3,\"nombreGrupo\":\"G03\",\"nombreAsignatura\":\"Bases\",\"tipoPlantillaEstudiante\":\"ALERTA_PERDIDA_INASISTENCIA\","
                + "\"estudiante\":{\"idUsuario\":\"" + usuario + "\",\"nombreCompleto\":\"Est\",\"correo\":\"e@x.co\"},"
                + "\"docente\":{\"idUsuario\":\"" + UUID.randomUUID() + "\",\"nombreCompleto\":\"Doc\"},\"idRevisionCreada\":null}]";
        final List<NovedadAlerta> novedades = AlertaInasistenciaJpaRepository.novedades(json);
        assertEquals(1, novedades.size());
        final NovedadAlerta n = novedades.getFirst();
        assertEquals(alerta, n.idAlerta());
        assertTrue(n.esPrincipal());
        assertEquals(new BigDecimal("21.5"), n.porcentaje());
        assertEquals(usuario, n.estudiante().idUsuario());
        assertNull(n.docente().correo());
        assertNull(n.idRevisionCreada());
        assertTrue(AlertaInasistenciaJpaRepository.novedades("[]").isEmpty());
        assertTrue(AlertaInasistenciaJpaRepository.novedades(null).isEmpty());
        assertTrue(AlertaInasistenciaJpaRepository.novedades(" ").isEmpty());
        assertThrows(DatabaseOperationException.class, () -> AlertaInasistenciaJpaRepository.novedades("{}"));
        assertThrows(DatabaseOperationException.class, () -> AlertaInasistenciaJpaRepository.novedades("[{"));
    }

    @Test
    void lee_las_novedades_de_resolutores() {
        final UUID revision = UUID.randomUUID();
        final String json = "[{\"idRevision\":\"" + revision + "\",\"idGrupo\":\"" + UUID.randomUUID() + "\",\"nombreGrupo\":\"G01\",\"nombreAsignatura\":\"Bases\","
                + "\"idEstudianteGrupo\":\"" + UUID.randomUUID() + "\",\"perfilResolutor\":\"DECANO\",\"evento\":\"ESCALAR\",\"porcentajeInasistencia\":22,\"umbral\":20,"
                + "\"motivo\":\"m\",\"nombreCompletoEstudiante\":\"Est\",\"nombreCompletoDocente\":\"Doc\",\"idUsuarioDocente\":\"" + UUID.randomUUID()
                + "\",\"idUsuarioEstudiante\":\"" + UUID.randomUUID() + "\",\"resolutores\":[{\"idUsuario\":\"" + UUID.randomUUID()
                + "\",\"codigoPerfil\":\"DECANO\",\"esEscalamiento\":true,\"correo\":\"d@x.co\",\"nombreCompleto\":\"Decano\"}]}]";
        final List<NovedadResolutores> novedades = AlertaInasistenciaJpaRepository.novedadesResolutores(json);
        assertEquals(revision, novedades.getFirst().idRevision());
        assertEquals("DECANO", novedades.getFirst().resolutores().getFirst().codigoPerfil());
        assertTrue(novedades.getFirst().resolutores().getFirst().esEscalamiento());
    }
}
