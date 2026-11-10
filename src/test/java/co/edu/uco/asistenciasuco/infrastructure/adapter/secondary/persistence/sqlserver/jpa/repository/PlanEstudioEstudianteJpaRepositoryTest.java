package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaPlanEstudianteProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU018: plan de estudios del estudiante leido por nombre de columna (PE-JPA-01..04). */
class PlanEstudioEstudianteJpaRepositoryTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID SEMESTRE = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final PlanEstudioEstudianteJpaRepository repository = new PlanEstudioEstudianteJpaRepository(entityManager);

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

    private static Map<String, Object> filaConAsignatura() {
        final Map<String, Object> f = new HashMap<>();
        f.put("idPrograma", PROGRAMA.toString());
        f.put("nombrePrograma", "Ingenieria de Sistemas");
        f.put("idPlanEstudio", PLAN);
        f.put("inpPlanEstudio", 2024);
        f.put("idSemestrePlanEstudio", SEMESTRE.toString());
        f.put("numeroSemestre", 8);
        f.put("idAsignaturaPlanEstudio", UUID.randomUUID().toString());
        f.put("idAsignatura", ASIGNATURA.toString());
        f.put("codigoAsignatura", "ARQ-402");
        f.put("nombreAsignatura", "Arquitectura de Software");
        f.put("creditos", 3);
        f.put("codigoComponente", "ESP");
        f.put("nombreComponente", "especificas");
        f.put("codigoCaracter", "OBL");
        f.put("nombreCaracter", "Obligatoria");
        f.put("cursando", Boolean.TRUE);
        f.put("idGrupoCursando", GRUPO.toString());
        f.put("nombreGrupoCursando", "Grupo 001 - Arq Software");
        return f;
    }

    private static Map<String, Object> filaDeSemestreVacio() {
        final Map<String, Object> f = new HashMap<>();
        f.put("idPrograma", PROGRAMA.toString());
        f.put("nombrePrograma", "Ingenieria de Sistemas");
        f.put("idPlanEstudio", PLAN);
        f.put("inpPlanEstudio", 2024);
        f.put("idSemestrePlanEstudio", UUID.randomUUID().toString());
        f.put("numeroSemestre", 9);
        f.put("cursando", Boolean.FALSE);
        return f;
    }

    @Test
    void PE_JPA_01_ejecuta_el_sp_con_el_usuario_y_la_correlacion_y_lee_las_columnas_por_nombre() {
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(filaConAsignatura())));

        final List<AsignaturaPlanEstudianteProjection> filas = repository.consultarPlanEstudio(USUARIO);

        assertEquals(List.of(new AsignaturaPlanEstudianteProjection(PROGRAMA, "Ingenieria de Sistemas", PLAN, 2024, SEMESTRE, 8,
                ASIGNATURA, "ARQ-402", "Arquitectura de Software", 3, "ESP", "OBL", "Obligatoria", true, GRUPO,
                "Grupo 001 - Arq Software")), filas);
        verify(entityManager).createNativeQuery(PlanEstudioEstudianteJpaRepository.SQL_PLAN_ESTUDIO, Tuple.class);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", CORRELACION);
        assertTrue(PlanEstudioEstudianteJpaRepository.SQL_PLAN_ESTUDIO.contains("usp_consultar_plan_estudio_estudiante"));
    }

    @Test
    void PE_JPA_02_un_semestre_sin_asignaturas_conserva_los_nulos_de_la_asignatura() {
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(filaDeSemestreVacio())));

        final AsignaturaPlanEstudianteProjection fila = repository.consultarPlanEstudio(USUARIO).getFirst();

        assertEquals(9, fila.numeroSemestre());
        assertNull(fila.idAsignatura());
        assertNull(fila.codigoAsignatura());
        assertNull(fila.creditos());
        assertNull(fila.codigoCaracter());
        assertFalse(fila.cursando());
        assertNull(fila.idGrupoCursando());
    }

    @Test
    void PE_JPA_03_el_ejecutor_es_obligatorio_y_sin_filas_devuelve_una_lista_vacia() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarPlanEstudio(null));
        verify(entityManager, never()).createNativeQuery(anyString(), eq(Tuple.class));
        assertThrows(NullPointerException.class, () -> new PlanEstudioEstudianteJpaRepository(null));

        when(query.getResultList()).thenReturn(List.of());
        assertEquals(List.of(), repository.consultarPlanEstudio(USUARIO));
    }

    @Test
    void PE_JPA_04_traduce_el_codigo_de_la_base_de_datos() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=VAL_001|El campo UsuarioEjecutor es obligatorio."));

        assertThrows(ValidationException.class, () -> repository.consultarPlanEstudio(USUARIO));
    }
}
