package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CERT-CULM001 (HU026C): el catalogo de la BD usa marcadores secuenciales {@code {}}. El adaptador SQL Server debe
 * sustituirlos igual que los adaptadores de Azure y de MessageUcoLab (MC-SEQ-01..04).
 */
class MessageCatalogJpaMarcadoresSecuencialesTest {

    private final EntityManager manager = mock(EntityManager.class);

    @SuppressWarnings("unchecked")
    private void catalogoDevuelve(final String contenido) {
        final TypedQuery<String> query = mock(TypedQuery.class);
        when(manager.createQuery(anyString(), eq(String.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(contenido));
    }

    @Test
    void MC_SEQ_01_sustituye_los_marcadores_secuenciales_en_orden() {
        catalogoDevuelve("Llevas {}% de inasistencia en {}. Si llegas al {}% podrías perder la asignatura.");

        assertEquals("Llevas 53.47% de inasistencia en Arquitectura de Software. Si llegas al 20.00% podrías perder la asignatura.",
                new MessageCatalogJpaRepository(manager).getUserMessage("ALR_010", "53.47", "Arquitectura de Software", "20.00"));
    }

    @Test
    void MC_SEQ_02_un_argumento_con_signos_de_expresion_regular_se_inserta_literal() {
        catalogoDevuelve("Asignatura {}: {}");

        assertEquals("Asignatura C$1 \\ (A+B): $0",
                new MessageCatalogJpaRepository(manager).getUserMessage("X_001", "C$1 \\ (A+B)", "$0"));
    }

    @Test
    void MC_SEQ_03_con_menos_argumentos_deja_los_marcadores_sobrantes_y_un_nulo_no_rompe() {
        catalogoDevuelve("Uno {} dos {} tres {}");

        assertEquals("Uno a dos null tres {}", new MessageCatalogJpaRepository(manager).getTechnicalMessage("X_002", "a", null));
    }

    @Test
    void MC_SEQ_04_los_marcadores_indexados_siguen_funcionando() {
        catalogoDevuelve("El grupo {0} tiene {1} cupos");

        assertEquals("El grupo G1 tiene 5 cupos", new MessageCatalogJpaRepository(manager).getUserMessage("GRP_001", "G1", 5));
    }
}
