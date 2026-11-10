package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.interactor.ConsultarPlanEstudioEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.AsignaturaPlanEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.PlanEstudioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.SemestrePlanEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaPlanEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU018: estructura curricular por semestres del estudiante (PE-UC-01..06). */
class ConsultarPlanEstudioEstudianteUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA_1 = UUID.randomUUID();
    private static final UUID PLAN_1 = UUID.randomUUID();
    private static final UUID PROGRAMA_2 = UUID.randomUUID();
    private static final UUID PLAN_2 = UUID.randomUUID();
    private static final UUID SEMESTRE_1 = UUID.randomUUID();
    private static final UUID SEMESTRE_2 = UUID.randomUUID();
    private static final UUID SEMESTRE_8 = UUID.randomUUID();
    private static final UUID SEMESTRE_OTRO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final PlanEstudioEstudianteQueryPort port = mock(PlanEstudioEstudianteQueryPort.class);
    private final ConsultarPlanEstudioEstudianteUseCaseImpl useCase = new ConsultarPlanEstudioEstudianteUseCaseImpl(scopePort, port);

    private static AsignaturaPlanEstudianteProjection fila(final UUID programa, final UUID plan, final UUID semestre, final Integer numero,
                                                           final String nombre, final Integer creditos, final String componente,
                                                           final boolean cursando) {
        return new AsignaturaPlanEstudianteProjection(programa, "Programa " + numero, plan, 2024, semestre, numero,
                nombre == null ? null : UUID.randomUUID(), nombre == null ? null : "COD-" + nombre, nombre, creditos, componente,
                nombre == null ? null : "OBL", nombre == null ? null : "Obligatoria", cursando, cursando ? GRUPO : null,
                cursando ? "Grupo 1" : null);
    }

    private void conEstudiante() {
        when(scopePort.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Test
    void PE_UC_01_agrupa_las_filas_por_plan_y_semestre_en_el_orden_del_sp() {
        conEstudiante();
        when(port.consultarPlanEstudio(ACTOR)).thenReturn(List.of(
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Zoologia", 3, "ESP", true),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Algebra", 4, "BAS", false),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_2, 2, "Fisica", 2, "INS", false),
                fila(PROGRAMA_2, PLAN_2, SEMESTRE_OTRO, 1, "Etica", 2, "INS", false)));

        final List<PlanEstudioEstudianteDomain> planes = useCase.execute(ACTOR);

        assertEquals(2, planes.size());
        final PlanEstudioEstudianteDomain plan = planes.getFirst();
        assertEquals(PLAN_1, plan.idPlanEstudio());
        assertEquals(PROGRAMA_1, plan.idPrograma());
        assertEquals(2024, plan.inp());
        assertEquals(List.of(1, 2), plan.semestres().stream().map(SemestrePlanEstudianteDomain::numeroSemestre).toList());
        assertEquals(List.of("Zoologia", "Algebra"),
                plan.semestres().getFirst().asignaturas().stream().map(AsignaturaPlanEstudianteDomain::nombre).toList());
        assertEquals(PLAN_2, planes.get(1).idPlanEstudio());
        assertEquals(1, planes.get(1).totalAsignaturas());
    }

    @Test
    void PE_UC_02_los_totales_salen_de_las_asignaturas_y_el_total_de_semestres_es_el_mas_alto() {
        conEstudiante();
        when(port.consultarPlanEstudio(ACTOR)).thenReturn(List.of(
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Zoologia", 3, "ESP", true),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Algebra", 4, "ESP", false),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_8, 8, "Arquitectura", 2, "ESP", true),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_8, 8, "Sin creditos", null, "ESP", false)));

        final PlanEstudioEstudianteDomain plan = useCase.execute(ACTOR).getFirst();

        assertEquals(8, plan.totalSemestres());
        assertEquals(4, plan.totalAsignaturas());
        assertEquals(9, plan.totalCreditos());
        assertEquals(2, plan.asignaturasCursando());
        assertEquals(5, plan.creditosCursando());
        assertEquals(2, plan.semestres().getFirst().totalAsignaturas());
        assertEquals(7, plan.semestres().getFirst().totalCreditos());
        assertEquals(2, plan.semestres().get(1).totalCreditos());
    }

    @Test
    void PE_UC_03_un_semestre_sin_asignaturas_queda_con_la_lista_vacia() {
        conEstudiante();
        when(port.consultarPlanEstudio(ACTOR)).thenReturn(List.of(
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Zoologia", 3, "ESP", false),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_2, 2, null, null, null, false)));

        final PlanEstudioEstudianteDomain plan = useCase.execute(ACTOR).getFirst();

        assertEquals(2, plan.semestres().size());
        assertEquals(List.of(), plan.semestres().get(1).asignaturas());
        assertEquals(0, plan.semestres().get(1).totalCreditos());
        assertEquals(2, plan.totalSemestres());
        assertEquals(1, plan.totalAsignaturas());
        assertEquals(0, plan.asignaturasCursando());
    }

    @Test
    void PE_UC_04_traduce_el_componente_y_solo_informa_el_grupo_de_lo_que_cursa() {
        conEstudiante();
        final UUID grupoFantasma = UUID.randomUUID();
        when(port.consultarPlanEstudio(ACTOR)).thenReturn(List.of(
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Propia", 3, "ESP", true),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Basica", 3, "BAS", false),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Institucional", 3, "INS", false),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_1, 1, "Otro", 3, "XYZ", false),
                new AsignaturaPlanEstudianteProjection(PROGRAMA_1, "Programa 1", PLAN_1, 2024, SEMESTRE_1, 1, UUID.randomUUID(), "SC",
                        "Sin componente", 1, null, "OPT", "Optativa", false, grupoFantasma, "No deberia salir")));

        final List<AsignaturaPlanEstudianteDomain> asignaturas = useCase.execute(ACTOR).getFirst().semestres().getFirst().asignaturas();

        assertEquals(List.of("ESPECIFICA", "BASICA", "INSTITUCIONAL", "XYZ"),
                asignaturas.stream().limit(4).map(AsignaturaPlanEstudianteDomain::tipoAsignatura).toList());
        assertTrue(asignaturas.getFirst().cursando());
        assertEquals(GRUPO, asignaturas.getFirst().idGrupoCursando());
        assertEquals("Grupo 1", asignaturas.getFirst().nombreGrupoCursando());
        assertEquals("COD-Propia", asignaturas.getFirst().codigo());
        assertEquals("OBL", asignaturas.getFirst().codigoCaracter());
        final AsignaturaPlanEstudianteDomain sinComponente = asignaturas.get(4);
        assertNull(sinComponente.tipoAsignatura());
        assertFalse(sinComponente.cursando());
        assertNull(sinComponente.idGrupoCursando());
        assertNull(sinComponente.nombreGrupoCursando());
        assertEquals("Optativa", sinComponente.nombreCaracter());
    }

    @Test
    void PE_UC_05_sin_estudiante_responde_prohibido_y_no_consulta_el_plan() {
        when(scopePort.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));

        verify(port, never()).consultarPlanEstudio(any());
    }

    @Test
    void PE_UC_06_sin_filas_no_hay_planes_y_las_dependencias_son_obligatorias() {
        conEstudiante();
        when(port.consultarPlanEstudio(ACTOR)).thenReturn(List.of());

        assertEquals(List.of(), useCase.execute(ACTOR));
        assertThrows(NullPointerException.class, () -> new ConsultarPlanEstudioEstudianteUseCaseImpl(null, port));
        assertThrows(NullPointerException.class, () -> new ConsultarPlanEstudioEstudianteUseCaseImpl(scopePort, null));
        assertEquals(0, new PlanEstudioEstudianteDomain(PLAN_1, 2024, PROGRAMA_1, "P", List.of()).totalSemestres());
    }

    @Test
    void PE_UC_07_el_interactor_entrega_el_contrato_con_los_totales() {
        conEstudiante();
        when(port.consultarPlanEstudio(ACTOR)).thenReturn(List.of(
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_8, 8, "Arquitectura", 3, "ESP", true),
                fila(PROGRAMA_1, PLAN_1, SEMESTRE_8, 8, "Electiva", 2, "ESP", false)));

        final List<PlanEstudioEstudianteDTO> planes = new ConsultarPlanEstudioEstudianteInteractor(useCase).execute(ACTOR);

        final PlanEstudioEstudianteDTO plan = planes.getFirst();
        assertEquals(PLAN_1, plan.idPlanEstudio());
        assertEquals("Programa 8", plan.nombrePrograma());
        assertEquals(8, plan.totalSemestres());
        assertEquals(2, plan.totalAsignaturas());
        assertEquals(5, plan.totalCreditos());
        assertEquals(1, plan.asignaturasCursando());
        assertEquals(3, plan.creditosCursando());
        assertEquals(SEMESTRE_8, plan.semestres().getFirst().idSemestrePlanEstudio());
        assertEquals(2, plan.semestres().getFirst().totalAsignaturas());
        assertEquals(5, plan.semestres().getFirst().totalCreditos());
        assertEquals("Arquitectura", plan.semestres().getFirst().asignaturas().getFirst().nombre());
        assertEquals("ESPECIFICA", plan.semestres().getFirst().asignaturas().getFirst().tipoAsignatura());
        assertTrue(plan.semestres().getFirst().asignaturas().getFirst().cursando());
        assertEquals(GRUPO, plan.semestres().getFirst().asignaturas().getFirst().idGrupoCursando());
        assertThrows(NullPointerException.class, () -> new ConsultarPlanEstudioEstudianteInteractor(null));
    }
}
