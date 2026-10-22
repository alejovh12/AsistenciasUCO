package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPlanEstudioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsignaturaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lectura con ambito de facultad para el decano (hallazgo del recorrido de vistas). Se verifica que cada consulta
 * filtre por {@code :idFacultad} atravesando {@code UvProgramaEntity} (la vista {@code uv_programa} trae la facultad).
 */
class LecturaPorFacultadJpaPatternTest {

    private static final UUID FACULTAD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DECANO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROGRAMA = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    private final TypedQuery<Object> typedQuery = mock(TypedQuery.class);

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void preparar() {
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn((TypedQuery) typedQuery);
        when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());
    }

    @Test
    void planes_por_facultad_filtran_por_los_programas_de_la_facultad() {
        final PlanEstudioJpaRepository repository = new PlanEstudioJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));

        assertTrue(repository.consultarPlanesPorFacultad(FACULTAD).isEmpty());

        verify(entityManager).createQuery(PlanEstudioJpaRepository.HQL_POR_FACULTAD, UvPlanEstudioEntity.class);
        verify(typedQuery).setParameter("idFacultad", FACULTAD);
        assertTrue(PlanEstudioJpaRepository.HQL_POR_FACULTAD.contains("UvProgramaEntity"));
        assertTrue(PlanEstudioJpaRepository.HQL_POR_FACULTAD.contains(":idFacultad"));
    }

    @Test
    void asignaturas_por_facultad_filtran_por_los_programas_de_la_facultad() {
        final AsignaturaJpaRepository repository = new AsignaturaJpaRepository(entityManager, new JpaProcedureExecutor(entityManager));

        assertTrue(repository.consultarAsignaturasPorFacultad(FACULTAD).isEmpty());

        verify(entityManager).createQuery(AsignaturaJpaRepository.HQL_POR_FACULTAD, AsignaturaQueryRow.class);
        verify(typedQuery).setParameter("idFacultad", FACULTAD);
        assertTrue(AsignaturaJpaRepository.HQL_POR_FACULTAD.contains("UvProgramaEntity"));
        assertTrue(AsignaturaJpaRepository.HQL_POR_FACULTAD.contains(":idFacultad"));
    }

    @Test
    void grupos_por_facultad_filtran_por_los_programas_de_la_facultad() {
        final GrupoJpaRepository repository = new GrupoJpaRepository(
                entityManager, mock(TransactionOperations.class), new JpaProcedureExecutor(entityManager));

        assertTrue(repository.consultarGruposPorFacultad(FACULTAD).isEmpty());

        verify(entityManager).createQuery(GrupoJpaRepository.HQL_GRUPOS_POR_FACULTAD, UvGrupoEntity.class);
        verify(typedQuery).setParameter("idFacultad", FACULTAD);
        assertTrue(GrupoJpaRepository.HQL_GRUPOS_POR_FACULTAD.contains("UvProgramaEntity"));
        assertTrue(GrupoJpaRepository.HQL_GRUPOS_POR_FACULTAD.contains(":idFacultad"));
    }

    @Test
    void el_decano_accede_a_un_programa_solo_si_es_decano_activo_de_su_facultad() {
        when(typedQuery.getSingleResult()).thenReturn(1L, 0L);
        final InstitutionalScopeJpaRepository repository = new InstitutionalScopeJpaRepository(entityManager);

        assertTrue(repository.canDecanoAccessPrograma(DECANO, PROGRAMA));
        assertFalse(repository.canDecanoAccessPrograma(DECANO, UUID.randomUUID()));

        verify(entityManager, org.mockito.Mockito.times(2)).createQuery(eq(InstitutionalScopeJpaRepository.HQL_DECANO_DE_PROGRAMA), eq(Long.class));
        verify(typedQuery).setParameter("usuarioId", DECANO);
        verify(typedQuery).setParameter("programaId", PROGRAMA);
        assertEquals(true, InstitutionalScopeJpaRepository.HQL_DECANO_DE_PROGRAMA.contains("estaActivoDecano = 1"));
    }
}
