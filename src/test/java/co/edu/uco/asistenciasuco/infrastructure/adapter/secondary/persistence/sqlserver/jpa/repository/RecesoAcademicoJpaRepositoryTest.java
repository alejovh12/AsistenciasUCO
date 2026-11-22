package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.DestinatarioReceso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.RecesoCreado;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.RecesoPeriodo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEC-081: adaptador del receso academico. {@code usp_crear_receso_academico} devuelve la tupla canonica con tres columnas extra
 * (sin detalle: {@code @incluirDetalle = 0}); las lecturas rechazan con {@code THROW 51000} y {@code DBCODE}.
 */
class RecesoAcademicoJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID RECESO = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 3);

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final JpaProcedureExecutor ejecutor = new JpaProcedureExecutor(manager);
    private RecesoAcademicoJpaRepository repo;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        repo = new RecesoAcademicoJpaRepository(manager, ejecutor);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] tuplaCrear(final boolean exitoso, final UUID correlacion, final Object idReceso, final Object canceladas,
                                       final Object conAsistencia) {
        return new Object[]{correlacion.toString(), "Se registró el receso académico del 03/12/2026; se cancelaron 2 clases.",
                exitoso ? "OK" : "DBCODE=REC_006|duplicado", exitoso, idReceso, canceladas, conAsistencia};
    }

    @Test
    void crear_envia_los_parametros_sin_pedir_el_detalle_y_lee_la_tupla_con_los_tres_conteos() {
        final UUID idReceso = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of((Object) tuplaCrear(true, CORRELACION, idReceso.toString(), 2, 1)));

        final RecesoCreado creado = repo.crear(idReceso, PERIODO, FECHA, "Jornada institucional", ADMIN);

        verify(manager).createNativeQuery(RecesoAcademicoJpaRepository.SQL_CREAR);
        assertTrue(RecesoAcademicoJpaRepository.SQL_CREAR.contains("@incluirDetalle = 0"));
        verify(query).setParameter("idPeriodo", PERIODO);
        verify(query).setParameter("fecha", FECHA);
        verify(query).setParameter("motivo", "Jornada institucional");
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idUsuarioEjecutor", ADMIN);
        verify(query).setParameter("idReceso", idReceso);
        assertEquals(idReceso, creado.idReceso());
        assertEquals(2, creado.sesionesCanceladas());
        assertEquals(1, creado.sesionesConAsistencia());
        assertEquals("Se registró el receso académico del 03/12/2026; se cancelaron 2 clases.", creado.mensajeUsuario());
    }

    @Test
    void crear_sin_sesiones_canceladas_devuelve_ceros() {
        final UUID idReceso = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of((Object) tuplaCrear(true, CORRELACION, idReceso, 0, 0)));

        final RecesoCreado creado = repo.crear(idReceso, PERIODO, FECHA, "Jornada", ADMIN);

        assertEquals(0, creado.sesionesCanceladas());
        assertEquals(0, creado.sesionesConAsistencia());
    }

    @Test
    void crear_traduce_un_rechazo_de_la_db_a_su_error_semantico() {
        when(query.getResultList()).thenReturn(List.of((Object) tuplaCrear(false, CORRELACION, null, null, null)));

        assertThrows(ConflictException.class, () -> repo.crear(UUID.randomUUID(), PERIODO, FECHA, "Jornada", ADMIN));
    }

    @Test
    void crear_con_correlacion_distinta_forma_invalida_o_error_de_persistencia_es_fallo_tecnico() {
        when(query.getResultList()).thenReturn(List.of((Object) tuplaCrear(true, UUID.randomUUID(), UUID.randomUUID(), 0, 0)));
        assertThrows(DatabaseOperationException.class, () -> repo.crear(UUID.randomUUID(), PERIODO, FECHA, "Jornada", ADMIN));

        org.mockito.Mockito.doReturn(List.of((Object) new Object[]{"x"})).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.crear(UUID.randomUUID(), PERIODO, FECHA, "Jornada", ADMIN));

        org.mockito.Mockito.doThrow(new PersistenceException("sql caido")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.crear(UUID.randomUUID(), PERIODO, FECHA, "Jornada", ADMIN));
    }

    @Test
    void retirar_ejecuta_el_sp_y_devuelve_el_mensaje_de_la_db() {
        when(query.getResultList()).thenReturn(List.of((Object) new Object[]{CORRELACION.toString(),
                "Se retiró el receso académico del 03/12/2026.", "OK", true}));

        assertEquals("Se retiró el receso académico del 03/12/2026.", repo.retirar(RECESO, ADMIN));

        verify(manager).createNativeQuery(RecesoAcademicoJpaRepository.SQL_RETIRAR);
        verify(query).setParameter("idReceso", RECESO);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        verify(query).setParameter("idUsuarioEjecutor", ADMIN);
    }

    @Test
    void retirar_un_dia_aplicado_responde_conflicto() {
        when(query.getResultList()).thenReturn(List.of((Object) new Object[]{CORRELACION.toString(),
                "Este día de receso ya canceló 4 clases y no se puede retirar ni cambiar.", "DBCODE=REC_010|aplicado", false}));

        assertThrows(ConflictException.class, () -> repo.retirar(RECESO, ADMIN));
    }

    @Test
    void consultar_mapea_las_nueve_columnas_por_posicion() {
        final UUID id = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of(
                (Object) new Object[]{id.toString(), PERIODO.toString(), java.sql.Date.valueOf(FECHA), "Jornada", 4, true, false,
                        Timestamp.valueOf(LocalDateTime.of(2026, 11, 21, 15, 0)), 2L},
                (Object) new Object[]{UUID.randomUUID(), PERIODO, FECHA.plusDays(1), "Otro", null, 0, 1, LocalDateTime.of(2026, 11, 21, 15, 5), 2}));

        final List<RecesoPeriodo> recesos = repo.consultar(PERIODO, ADMIN);

        verify(manager).createNativeQuery(RecesoAcademicoJpaRepository.SQL_CONSULTAR);
        verify(query).setParameter("idPeriodo", PERIODO);
        assertEquals(2, recesos.size());
        assertEquals(new RecesoPeriodo(id, PERIODO, FECHA, "Jornada", 4, true, false, LocalDateTime.of(2026, 11, 21, 15, 0)), recesos.get(0));
        assertEquals(0, recesos.get(1).sesionesCanceladas());
        assertEquals(false, recesos.get(1).estaAplicado());
        assertEquals(true, recesos.get(1).puedeRetirarse());
        assertEquals(FECHA.plusDays(1), recesos.get(1).fecha());
    }

    @Test
    void consultar_traduce_los_dbcode_de_la_lectura_y_rechaza_columnas_inesperadas() {
        org.mockito.Mockito.doThrow(new PersistenceException(new RuntimeException("DBCODE=SEC_001|sin perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultar(PERIODO, ADMIN));

        org.mockito.Mockito.doThrow(new PersistenceException(new RuntimeException("DBCODE=PER_001|no existe"))).when(query).getResultList();
        assertThrows(ResourceNotFoundException.class, () -> repo.consultar(PERIODO, ADMIN));

        org.mockito.Mockito.doThrow(new PersistenceException("sql caido")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultar(PERIODO, ADMIN));

        org.mockito.Mockito.doReturn(List.of((Object) new Object[8])).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultar(PERIODO, ADMIN));
    }

    @Test
    void consultar_destinatarios_mapea_las_doce_columnas_y_traduce_rec_009() {
        final UUID usuario = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.of((Object) new Object[]{"ESTUDIANTE", usuario.toString(), "Ana Gómez", "ana@uco.edu.co",
                sesion.toString(), grupo.toString(), "Bases de Datos", "G02", java.sql.Date.valueOf(FECHA),
                Timestamp.valueOf(LocalDateTime.of(2026, 12, 3, 13, 0)), Timestamp.valueOf(LocalDateTime.of(2026, 12, 3, 15, 0)),
                "Receso académico: Jornada"}));

        final List<DestinatarioReceso> filas = repo.consultarDestinatarios(RECESO, ADMIN);

        verify(manager).createNativeQuery(RecesoAcademicoJpaRepository.SQL_DESTINATARIOS);
        verify(query).setParameter("idReceso", RECESO);
        assertEquals(new DestinatarioReceso("ESTUDIANTE", usuario, "Ana Gómez", "ana@uco.edu.co", sesion, grupo, "Bases de Datos", "G02",
                FECHA, LocalDateTime.of(2026, 12, 3, 13, 0), LocalDateTime.of(2026, 12, 3, 15, 0), "Receso académico: Jornada"), filas.getFirst());

        org.mockito.Mockito.doThrow(new PersistenceException(new RuntimeException("DBCODE=REC_009|no existe"))).when(query).getResultList();
        assertThrows(ResourceNotFoundException.class, () -> repo.consultarDestinatarios(RECESO, ADMIN));
    }

    @Test
    void fechaDia_acepta_los_tipos_que_entrega_el_driver() {
        assertNull(RecesoAcademicoJpaRepository.fechaDia(null));
        assertEquals(FECHA, RecesoAcademicoJpaRepository.fechaDia(FECHA));
        assertEquals(FECHA, RecesoAcademicoJpaRepository.fechaDia(FECHA.atStartOfDay()));
        assertEquals(FECHA, RecesoAcademicoJpaRepository.fechaDia(java.sql.Date.valueOf(FECHA)));
        assertEquals(FECHA, RecesoAcademicoJpaRepository.fechaDia(new java.util.Date(java.sql.Date.valueOf(FECHA).getTime())));
        assertEquals(FECHA, RecesoAcademicoJpaRepository.fechaDia("2026-12-03"));
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new RecesoAcademicoJpaRepository(null, ejecutor));
        assertThrows(NullPointerException.class, () -> new RecesoAcademicoJpaRepository(manager, null));
    }
}