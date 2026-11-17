package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenGruposDocenteQueryPort;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W3I: el resumen de grupos del docente se lee por nombre de columna de {@code usp_consultar_resumen_grupos_docente}. */
class ResumenGruposDocenteJpaRepositoryTest {

    private static final UUID USUARIO = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final ResumenGruposDocenteJpaRepository repository = new ResumenGruposDocenteJpaRepository(entityManager);

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

    private static Tuple fila(final int grupos, final int activos, final int cupo, final BigDecimal tasa) {
        final Map<String, Object> f = new HashMap<>();
        f.put("gruposHabilitados", grupos);
        f.put("estudiantesActivos", activos);
        f.put("cupoTotal", cupo);
        f.put("tasaOcupacion", tasa);
        return TomaAsistenciaJpaRepositoryTest.tupla(f);
    }

    @Test
    void mapea_la_fila_del_sp_y_le_envia_el_ejecutor_y_la_correlacion() {
        when(query.getResultList()).thenReturn(List.of(fila(2, 4, 50, new BigDecimal("8.00"))));

        final ResumenGruposDocenteQueryPort.ResumenGrupos resumen = repository.consultarResumenGruposDocente(USUARIO);

        assertEquals(new ResumenGruposDocenteQueryPort.ResumenGrupos(2, 4, 50, new BigDecimal("8.00")), resumen);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_resumen_grupos_docente"), eq(Tuple.class));
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter(eq("idCorrelacion"), any(UUID.class));
    }

    @Test
    void con_cupo_cero_la_tasa_llega_nula_y_nunca_se_inventa() {
        when(query.getResultList()).thenReturn(List.of(fila(0, 0, 0, null)));

        final ResumenGruposDocenteQueryPort.ResumenGrupos resumen = repository.consultarResumenGruposDocente(USUARIO);

        assertEquals(0, resumen.cupoTotal());
        assertNull(resumen.tasaOcupacion());
    }

    @Test
    void sin_filas_devuelve_ceros() {
        when(query.getResultList()).thenReturn(List.of());

        final ResumenGruposDocenteQueryPort.ResumenGrupos resumen = repository.consultarResumenGruposDocente(USUARIO);

        assertEquals(0, resumen.gruposHabilitados());
        assertEquals(0, resumen.estudiantesActivos());
        assertNull(resumen.tasaOcupacion());
    }

    @Test
    void un_rechazo_del_sp_por_perfil_se_traduce_a_403() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|perfil"));

        assertThrows(ForbiddenException.class, () -> repository.consultarResumenGruposDocente(USUARIO));
    }

    @Test
    void el_constructor_exige_el_entity_manager_y_la_consulta_es_de_solo_lectura() {
        assertThrows(NullPointerException.class, () -> new ResumenGruposDocenteJpaRepository(null));
        assertTrue(ResumenGruposDocenteJpaRepository.SQL_RESUMEN_GRUPOS.contains("@idUsuarioEjecutor"));
        assertTrue(ResumenGruposDocenteJpaRepository.SQL_RESUMEN_GRUPOS.contains("@idCorrelacion"));
    }
}
