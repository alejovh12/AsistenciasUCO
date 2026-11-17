package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;



import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreViewJpaQueryErrorSemanticsTest {

    @Test
    void errores_de_provider_conservan_database_operation_exception_en_las_seis_capabilities() {
        final EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.createQuery(anyString(), eq(UvSesionEntity.class))).thenThrow(new PersistenceException("db"));
        // CULM-001 B.1: la lectura de sesiones se une a uv_sesion_estado y consulta Object[].
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvGrupoEntity.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvUsuarioEntity.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenThrow(new PersistenceException("db"));
        when(entityManager.createQuery(anyString(), eq(UvTipoIdentificacionEntity.class))).thenThrow(new PersistenceException("db"));
        // CULM-001 A.2-1/A.2-2: grupos y directorio de estudiantes se consultan por SP (consulta nativa con ejecutor).
        when(entityManager.createNativeQuery(anyString())).thenThrow(new PersistenceException("db"));
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID());

        final var sesionJpaRepository = new SesionJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));
        final var uuid3 = UUID.randomUUID();
        assertThrows(DatabaseOperationException.class,
                () -> sesionJpaRepository.consultarSesionesPorGrupo(uuid3));
        final var grupoJpaRepository = new GrupoJpaRepository(entityManager,
                        mock(org.springframework.transaction.support.TransactionOperations.class),
                        new JpaProcedureExecutor(entityManager));
        final var uuid2 = UUID.randomUUID();
        assertThrows(DatabaseOperationException.class,
                () -> grupoJpaRepository.consultarGrupos(uuid2));
        final var usuarioJpaRepository = new UsuarioJpaRepository(entityManager);
        final var uuid = UUID.randomUUID();
        assertThrows(DatabaseOperationException.class,
                () -> usuarioJpaRepository.consultarUsuarioPorId(uuid));
        final var estudianteJpaRepository = new EstudianteJpaRepository(entityManager);
        final var emptyPage2 = emptyPage();
        assertThrows(DatabaseOperationException.class,
                () -> estudianteJpaRepository.consultarEstudiantes(emptyPage2));
        final var tipoIdentificacionJpaRepository = new TipoIdentificacionJpaRepository(entityManager);
        assertThrows(DatabaseOperationException.class,
                () -> tipoIdentificacionJpaRepository.consultarTiposIdentificacion());
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.clear();
    }

    @Test
    void mapper_de_sesion_proyecta_el_instante_en_utc_sin_depender_del_timezone_default() {
        final TimeZone original = TimeZone.getDefault();
        final Instant instant = Instant.parse("2026-08-17T23:00:00Z");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
            assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
                    CoreViewJpaProjectionMapper.toUtcLocalDateTime(Date.from(instant)));
            assertEquals(LocalDateTime.ofInstant(instant.plusSeconds(3600), ZoneOffset.UTC),
                    CoreViewJpaProjectionMapper.toUtcLocalDateTime(Date.from(instant.plusSeconds(3600))));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static ConsultarEstudiantesRepositoryDTO emptyPage() {
        return new ConsultarEstudiantesRepositoryDTO(
                null, null, null, null, null, null, null, null, null, 0, 10, UUID.randomUUID());
    }
}




