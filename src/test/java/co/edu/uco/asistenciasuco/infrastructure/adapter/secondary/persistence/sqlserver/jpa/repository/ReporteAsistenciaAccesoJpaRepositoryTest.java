package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.9-a (F-TRX-021): usp_validar_acceso_reporte_grupo decide el 403 del reporte Excel. */
class ReporteAsistenciaAccesoJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private ReporteAsistenciaJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new ReporteAsistenciaJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void grupo_en_ambito_no_lanza_y_envia_ejecutor_correlacion_y_grupo() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "", "", true}));

        assertDoesNotThrow(() -> repository.validarAccesoGrupo(ACTOR, GRUPO));

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_validar_acceso_reporte_grupo"));
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idGrupo", GRUPO);
    }

    @Test
    void grupo_inexistente_o_ajeno_sec_002_es_403() {
        when(query.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{CORRELATION, "No tiene autorizacion.", "DBCODE=SEC_002|Titularidad denegada", false}));

        assertThrows(ForbiddenException.class, () -> repository.validarAccesoGrupo(ACTOR, GRUPO));
    }
}
