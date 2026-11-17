package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatrizAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenSesionesGrupoQueryPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W3M: la matriz y el resumen de sesiones del docente se leen por nombre de columna de los SP nuevos. */
class MatrizAsistenciaJpaRepositoryTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID SESION_1 = UUID.randomUUID();
    private static final UUID SESION_2 = UUID.randomUUID();
    private static final UUID MATRICULA_A = UUID.randomUUID();
    private static final UUID MATRICULA_B = UUID.randomUUID();
    private static final UUID ESTUDIANTE_A = UUID.randomUUID();
    private static final UUID ESTUDIANTE_B = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final ResumenAsistenciaJpaRepository repository = new ResumenAsistenciaJpaRepository(entityManager);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Tuple celda(final UUID matricula, final UUID estudiante, final String nombre, final UUID sesion, final String estado,
                               final int asistidos, final int minutos, final int total) {
        final Map<String, Object> f = new HashMap<>();
        f.put("idEstudianteGrupo", matricula.toString());
        f.put("idEstudiante", estudiante.toString());
        f.put("nombreCompletoEstudiante", nombre);
        f.put("documento", "100");
        f.put("codigoEstadoEstudiante", "A");
        f.put("totalRegistros", total);
        f.put("idSesion", sesion == null ? null : sesion.toString());
        f.put("codigoEstado", estado);
        f.put("minutosAsistidos", asistidos);
        f.put("minutosSesion", minutos);
        return TomaAsistenciaJpaRepositoryTest.tupla(f);
    }

    @Test
    void la_matriz_agrupa_las_filas_por_matricula_conservando_el_orden_y_el_total_de_estudiantes() {
        when(query.getResultList()).thenReturn(List.of(
                celda(MATRICULA_A, ESTUDIANTE_A, "Ana Perez", SESION_1, "PRESENTE", 120, 120, 7),
                celda(MATRICULA_A, ESTUDIANTE_A, "Ana Perez", SESION_2, "PARCIAL", 60, 120, 7),
                celda(MATRICULA_B, ESTUDIANTE_B, "Beto Ruiz", SESION_1, "SIN_REGISTRO", 0, 120, 7),
                celda(MATRICULA_B, ESTUDIANTE_B, "Beto Ruiz", SESION_2, "NO_DISPONIBLE", 0, 120, 7)));

        final PaginaResultado<MatrizAsistenciaQueryPort.FilaMatriz> pagina = repository.consultarMatrizGrupoDocente(GRUPO, 1, 2, "  ", USUARIO);

        assertEquals(7, pagina.totalElementos());
        assertEquals(1, pagina.numeroPagina());
        assertEquals(2, pagina.tamanoPagina());
        assertEquals(List.of(
                new MatrizAsistenciaQueryPort.FilaMatriz(MATRICULA_A, ESTUDIANTE_A, "Ana Perez", "100", "A", List.of(
                        new MatrizAsistenciaQueryPort.CeldaMatriz(SESION_1, "PRESENTE", 120, 120),
                        new MatrizAsistenciaQueryPort.CeldaMatriz(SESION_2, "PARCIAL", 60, 120))),
                new MatrizAsistenciaQueryPort.FilaMatriz(MATRICULA_B, ESTUDIANTE_B, "Beto Ruiz", "100", "A", List.of(
                        new MatrizAsistenciaQueryPort.CeldaMatriz(SESION_1, "SIN_REGISTRO", 0, 120),
                        new MatrizAsistenciaQueryPort.CeldaMatriz(SESION_2, "NO_DISPONIBLE", 0, 120)))), pagina.elementos());
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("numeroPagina", 1);
        verify(query).setParameter("tamanoPagina", 2);
        verify(query).setParameter("busqueda", "  ");
    }

    @Test
    void la_matriz_sin_busqueda_envia_null_y_un_grupo_sin_sesiones_deja_las_matriculas_sin_celdas() {
        when(query.getResultList()).thenReturn(List.of(celda(MATRICULA_A, ESTUDIANTE_A, "Ana Perez", null, null, 0, 0, 1)));

        final PaginaResultado<MatrizAsistenciaQueryPort.FilaMatriz> pagina = repository.consultarMatrizGrupoDocente(GRUPO, 1, 50, null, USUARIO);

        assertEquals(1, pagina.totalElementos());
        assertEquals(List.of(), pagina.elementos().getFirst().celdas());
        verify(query).setParameter("busqueda", null);
    }

    @Test
    void una_matriz_vacia_tiene_total_cero_y_un_rechazo_del_sp_se_traduce_a_403() {
        when(query.getResultList()).thenReturn(List.of());
        final PaginaResultado<MatrizAsistenciaQueryPort.FilaMatriz> vacia = repository.consultarMatrizGrupoDocente(GRUPO, 1, 50, "zzz", USUARIO);
        assertEquals(0, vacia.totalElementos());
        assertTrue(vacia.elementos().isEmpty());

        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_002|ajeno"));
        assertThrows(ForbiddenException.class, () -> repository.consultarMatrizGrupoDocente(GRUPO, 1, 50, null, USUARIO));
    }

    private static Map<String, Object> filaSesion(final UUID sesion, final int numero, final String estado, final Object procedencia) {
        final Map<String, Object> f = new HashMap<>();
        f.put("idSesion", sesion == null ? null : sesion.toString());
        f.put("numero", numero);
        f.put("nombre", "Sesion " + numero);
        f.put("fechaHoraInicio", Timestamp.valueOf("2026-11-10 13:00:00"));
        f.put("fechaHoraFin", Timestamp.valueOf("2026-11-10 15:00:00"));
        f.put("procedenciaTemporal", procedencia);
        f.put("estadoSesion", estado);
        f.put("estadoSolicitud", null);
        f.put("pendienteAprobacion", false);
        f.put("totalEstudiantes", 30);
        f.put("presentes", 20);
        f.put("parciales", 2);
        f.put("justificadas", 3);
        f.put("ausentes", 4);
        f.put("sinRegistro", 1);
        f.put("sesionesProgramadas", 2);
        f.put("sesionesEjecutadas", 1);
        f.put("sesionesCanceladas", 0);
        f.put("sesionesEnCurso", 1);
        f.put("idSesionEnCurso", SESION_2.toString());
        f.put("idSesionProxima", null);
        f.put("estudiantesActivos", 30);
        f.put("estudiantesEnRiesgo", 4);
        f.put("promedioAsistencia", new BigDecimal("87.5000"));
        f.put("umbralAdvertencia", new BigDecimal("15.00"));
        f.put("umbralPerdida", new BigDecimal("20.00"));
        return f;
    }

    @Test
    void el_resumen_de_sesiones_mapea_el_grupo_y_cada_sesion_con_su_horario_confirmado() {
        when(query.getResultList()).thenReturn(List.of(
                TomaAsistenciaJpaRepositoryTest.tupla(filaSesion(SESION_1, 1, "EJECUTADA", "UTC_V2")),
                TomaAsistenciaJpaRepositoryTest.tupla(filaSesion(SESION_2, 2, "EN_CURSO", null))));

        final ResumenSesionesGrupoQueryPort.ResumenGrupo resumen = repository.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO);

        assertEquals(2, resumen.sesionesProgramadas());
        assertEquals(1, resumen.sesionesEjecutadas());
        assertEquals(0, resumen.sesionesCanceladas());
        assertEquals(1, resumen.sesionesEnCurso());
        assertEquals(SESION_2, resumen.idSesionEnCurso());
        assertNull(resumen.idSesionProxima());
        assertEquals(30, resumen.estudiantesActivos());
        assertEquals(4, resumen.estudiantesEnRiesgo());
        assertEquals(new BigDecimal("87.50"), resumen.promedioAsistencia());
        assertEquals(new BigDecimal("15.00"), resumen.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), resumen.umbralPerdida());
        assertEquals(2, resumen.sesiones().size());
        final ResumenSesionesGrupoQueryPort.SesionResumen primera = resumen.sesiones().getFirst();
        assertEquals(new ResumenSesionesGrupoQueryPort.SesionResumen(SESION_1, 1, "Sesion 1", java.time.Instant.parse("2026-11-10T13:00:00Z"),
                java.time.Instant.parse("2026-11-10T15:00:00Z"), true, "EJECUTADA", null, false, 30, 20, 2, 3, 4, 1), primera);
        assertFalse(resumen.sesiones().get(1).horarioConfirmado());
        verify(query).setParameter("idGrupo", GRUPO);
    }

    @Test
    void un_grupo_sin_sesiones_aporta_solo_las_columnas_del_grupo() {
        final Map<String, Object> sinSesion = filaSesion(null, 0, null, null);
        sinSesion.put("numero", null);
        sinSesion.put("promedioAsistencia", null);
        sinSesion.put("sesionesProgramadas", 0);
        sinSesion.put("sesionesEjecutadas", 0);
        sinSesion.put("sesionesEnCurso", 0);
        sinSesion.put("idSesionEnCurso", null);
        sinSesion.put("estudiantesEnRiesgo", 0);
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(sinSesion)));

        final ResumenSesionesGrupoQueryPort.ResumenGrupo resumen = repository.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO);

        assertTrue(resumen.sesiones().isEmpty());
        assertNull(resumen.promedioAsistencia());
        assertNull(resumen.idSesionEnCurso());
        assertEquals(0, resumen.sesionesProgramadas());
        assertEquals(30, resumen.estudiantesActivos());
    }

    @Test
    void sin_filas_el_resumen_es_cero_y_un_rechazo_del_sp_se_traduce_a_403() {
        when(query.getResultList()).thenReturn(List.of());
        final ResumenSesionesGrupoQueryPort.ResumenGrupo vacio = repository.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO);
        assertEquals(0, vacio.sesionesProgramadas());
        assertTrue(vacio.sesiones().isEmpty());
        assertNull(vacio.promedioAsistencia());

        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_002|ajeno"));
        assertThrows(ForbiddenException.class, () -> repository.consultarResumenSesionesGrupoDocente(GRUPO, USUARIO));
    }

    @Test
    void el_resumen_por_horas_envia_la_busqueda_al_sp() {
        when(query.getResultList()).thenReturn(List.of());

        repository.consultarResumenGrupoDocente(GRUPO, 1, 50, "ana", USUARIO);

        verify(query).setParameter("busqueda", "ana");
        assertTrue(ResumenAsistenciaJpaRepository.SQL_GRUPO_DOCENTE.contains("@busqueda = :busqueda"));
    }
}
