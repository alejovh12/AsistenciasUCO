package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ConfiguracionReclamosProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 D.2 (CONTRACT D 3.2/3.3): patron JPA (createNativeQuery + EXEC con binding nombrado) de
 * usp_consultar_configuracion_reclamos y mapeo de su fila unica. La paridad real contra SQL Server la certifica
 * SqlStoredProcedureContractIT.
 */
class ConfiguracionReclamosJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private ConfiguracionReclamosJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new ConfiguracionReclamosJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void consulta_el_sp_con_binding_nombrado_y_mapea_la_fila_unica() {
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{3, 5_242_880L, "application/pdf,image/png,image/jpeg", 10, 5}));

        final ConfiguracionReclamosProjection fila = repository.consultar(USER);

        assertEquals(new ConfiguracionReclamosProjection(3, 5_242_880L, "application/pdf,image/png,image/jpeg", 10, 5), fila);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_configuracion_reclamos"));
        verify(query).setParameter("idUsuarioEjecutor", USER);
        verify(query).setParameter("idCorrelacion", CORRELATION);
    }

    @Test
    void acepta_numericos_de_otros_tipos_y_conserva_los_nulos_para_que_el_caso_de_uso_decida() {
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{BigInteger.valueOf(3), BigInteger.valueOf(5_242_880L), null, null, (short) 5}));

        final ConfiguracionReclamosProjection fila = repository.consultar(USER);

        assertEquals(3, fila.maximoSoportes());
        assertEquals(5_242_880L, fila.tamanioMaximoBytes());
        assertNull(fila.tiposPermitidos());
        assertNull(fila.limiteSubidasPorMinuto());
        assertEquals(5, fila.diasLimiteJustificacion());
    }

    @Test
    void sin_fila_devuelve_nulo() {
        when(query.getResultList()).thenReturn(List.of());
        assertNull(repository.consultar(USER));
    }

    @Test
    void una_fila_con_otro_numero_de_columnas_es_un_fallo_tecnico() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{3, 1L}));
        assertThrows(DatabaseOperationException.class, () -> repository.consultar(USER));
    }

    @Test
    void un_rol_sin_acceso_lanzado_por_el_sp_se_traduce_a_403() {
        when(query.getResultList()).thenThrow(new PersistenceException(
                new RuntimeException("DBCODE=SEC_001|El usuario no tiene el perfil requerido.")));
        assertThrows(ForbiddenException.class, () -> repository.consultar(USER));
    }

    @Test
    void un_fallo_de_persistencia_sin_codigo_conocido_es_tecnico() {
        when(query.getResultList()).thenThrow(new PersistenceException("sin conexion"));
        assertThrows(DatabaseOperationException.class, () -> repository.consultar(USER));
    }

    @Test
    void exige_el_entity_manager() {
        assertThrows(NullPointerException.class, () -> new ConfiguracionReclamosJpaRepository(null));
    }
}
