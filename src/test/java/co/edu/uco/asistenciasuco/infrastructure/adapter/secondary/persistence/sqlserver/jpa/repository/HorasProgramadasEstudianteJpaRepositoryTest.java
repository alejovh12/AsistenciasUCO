package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorasProgramadasEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU019: horas programadas del estudiante leidas por nombre de columna (HP-JPA-01..04). */
class HorasProgramadasEstudianteJpaRepositoryTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final HorasProgramadasEstudianteJpaRepository repository = new HorasProgramadasEstudianteJpaRepository(entityManager);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Tuple fila(final Object idGrupo, final Object sesiones, final Object horas) {
        final Map<String, Object> f = new HashMap<>();
        f.put("idEstudianteGrupo", UUID.randomUUID().toString());
        f.put("idGrupo", idGrupo);
        f.put("sesionesProgramadas", sesiones);
        f.put("horasProgramadas", horas);
        return TomaAsistenciaJpaRepositoryTest.tupla(f);
    }

    @Test
    void HP_JPA_01_ejecuta_el_sp_con_el_usuario_y_la_correlacion_y_lee_las_columnas_por_nombre() {
        when(query.getResultList()).thenReturn(List.of(
                fila(GRUPO.toString(), 5, new BigDecimal("8.966666")),
                fila(UUID.randomUUID(), 0, BigDecimal.ZERO)));

        final List<HorasProgramadasEstudianteProjection> filas = repository.consultarHorasProgramadas(USUARIO);

        assertEquals(2, filas.size());
        assertEquals(new HorasProgramadasEstudianteProjection(GRUPO, 5, new BigDecimal("8.97")), filas.getFirst());
        assertEquals(0, filas.get(1).sesionesProgramadas());
        assertEquals(new BigDecimal("0.00"), filas.get(1).horasProgramadas());
        verify(entityManager).createNativeQuery(HorasProgramadasEstudianteJpaRepository.SQL_HORAS_PROGRAMADAS, Tuple.class);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        assertTrue(HorasProgramadasEstudianteJpaRepository.SQL_HORAS_PROGRAMADAS.contains("usp_consultar_horas_programadas_estudiante"));
    }

    @Test
    void HP_JPA_02_sin_filas_devuelve_una_lista_vacia() {
        when(query.getResultList()).thenReturn(List.of());

        assertEquals(List.of(), repository.consultarHorasProgramadas(USUARIO));
    }

    @Test
    void HP_JPA_03_el_ejecutor_es_obligatorio_y_no_se_consulta_sin_el() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarHorasProgramadas(null));
        verify(entityManager, never()).createNativeQuery(anyString(), eq(Tuple.class));
        assertThrows(NullPointerException.class, () -> new HorasProgramadasEstudianteJpaRepository(null));
    }

    @Test
    void HP_JPA_04_traduce_el_codigo_de_la_base_de_datos() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=VAL_001|El campo UsuarioEjecutor es obligatorio."));

        assertThrows(ValidationException.class, () -> repository.consultarHorasProgramadas(USUARIO));
    }
}
