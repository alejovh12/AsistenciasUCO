package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort.SesionCalendario;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ATA D7: adaptador JPA de los SP del calendario de sesiones (estudiante y docente). */
class SesionCalendarioJpaRepositoryTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DOCENTE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USUARIO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 16);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 22);

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final SesionCalendarioJpaRepository repository = new SesionCalendarioJpaRepository(entityManager);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void el_estudiante_mapea_las_dieciseis_columnas_y_envia_los_parametros() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 16, 13, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{SESION.toString(), GRUPO, 3, "G03", ASIGNATURA, "MAT101", "Calculo", DOCENTE, "Maria Docente", inicio,
                        inicio.plusHours(2), true, false, null, false, null},
                new Object[]{SESION, GRUPO, null, null, ASIGNATURA, null, null, DOCENTE, null, null, null, false, true, "Festivo", true, "Dia X"}));

        final List<SesionCalendario> sesiones = repository.consultarEstudiante(USUARIO, DESDE, HASTA);

        assertEquals(2, sesiones.size());
        assertEquals(new SesionCalendario(SESION, GRUPO, 3, "G03", ASIGNATURA, "MAT101", "Calculo", DOCENTE, "Maria Docente", inicio,
                inicio.plusHours(2), true, false, null, false, null), sesiones.getFirst());
        assertNull(sesiones.get(1).codigoGrupo());
        assertNull(sesiones.get(1).fechaHoraInicioUtc());
        assertEquals("Festivo", sesiones.get(1).motivoCancelacion());
        assertEquals(true, sesiones.get(1).esCancelacionAutomatica());
        verify(entityManager).createNativeQuery(SesionCalendarioJpaRepository.SQL_ESTUDIANTE);
        assertTrue(SesionCalendarioJpaRepository.SQL_ESTUDIANTE.contains("usp_consultar_sesiones_calendario_estudiante"));
        verify(query).setParameter("idUsuario", USUARIO);
        verify(query).setParameter("desde", DESDE);
        verify(query).setParameter("hasta", HASTA);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void el_docente_usa_su_sp() {
        when(query.getResultList()).thenReturn(List.of());

        assertEquals(0, repository.consultarDocente(USUARIO, DESDE, HASTA).size());
        verify(entityManager).createNativeQuery(SesionCalendarioJpaRepository.SQL_DOCENTE);
        assertTrue(SesionCalendarioJpaRepository.SQL_DOCENTE.contains("usp_consultar_sesiones_calendario_docente"));
    }

    @Test
    void traduce_los_dbcode_de_perfil_y_de_rango() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|sin perfil"))
                .thenThrow(new PersistenceException("DBCODE=CAL_001|rango invalido"));

        assertThrows(ForbiddenException.class, () -> repository.consultarDocente(USUARIO, DESDE, HASTA));
        assertEquals("CAL_001", assertThrows(ValidationException.class, () -> repository.consultarEstudiante(USUARIO, HASTA, DESDE)).getCode());
    }

    @Test
    void un_resultado_con_columnas_distintas_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.<Object>of((Object) new Object[]{SESION, GRUPO}));

        final DatabaseOperationException error = assertThrows(DatabaseOperationException.class,
                () -> repository.consultarEstudiante(USUARIO, DESDE, HASTA));
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }

    @Test
    void el_constructor_exige_el_entity_manager() {
        assertThrows(NullPointerException.class, () -> new SesionCalendarioJpaRepository(null));
    }
}
