package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.AsignaturaPlanUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM: el interactor mapea proyecciones a los DTO del contrato (campos propios, nunca listas dentro de nombrePrograma). */
class AsignaturaPlanConsultasInteractorTest {

    private final AsignaturaPlanUseCase useCase = mock(AsignaturaPlanUseCase.class);
    private final AsignaturaPlanInteractor interactor = new AsignaturaPlanInteractor(useCase);
    private final UUID u = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    @Test
    void malla_y_asignaturas_del_coordinador_mapean_cada_campo() {
        final var fila = new AsignaturaPlanProjection(id, "MAT1", "Calculo", 3, id, "Ciencias", id, "Basico", id, 2024, id, "Sistemas",
                id, 2, "S2", "OPT", "Optativa", true, true, true);
        when(useCase.consultarMalla(u, id, 2, "OPT", null, 1, 20)).thenReturn(new PaginaAsignaturasPlanProjection(List.of(fila), 9));
        when(useCase.consultarAsignaturasCoordinador(u, id, null, null, 1, 20)).thenReturn(new PaginaAsignaturasPlanProjection(List.of(fila), 9));

        for (final var pagina : List.of(interactor.consultarMalla(u, id, 2, "OPT", null, 1, 20),
                interactor.consultarAsignaturasCoordinador(u, id, null, null, 1, 20))) {
            final var dto = pagina.elementos().getFirst();
            assertEquals(9, pagina.total());
            assertEquals("MAT1", dto.codigo());
            assertEquals("Sistemas", dto.nombrePrograma());
            assertEquals(2024, dto.inpPlanEstudio());
            assertEquals("OPT", dto.codigoCaracter());
            assertEquals("Optativa", dto.nombreCaracter());
            assertEquals(2, dto.numeroSemestre());
            assertTrue(dto.compartida());
            assertTrue(dto.estaActivoVinculo());
        }
    }

    @Test
    void planes_mapea_totales_y_estado() {
        when(useCase.consultarPlanes(u, id, true, 1, 20)).thenReturn(new PaginaPlanesEstudioProjection(
                List.of(new PlanEstudioProjection(id, id, "Sistemas", "2024", true, "Activo", null, 10, 40, 120)), 1));

        final var plan = interactor.consultarPlanes(u, id, true, 1, 20).elementos().getFirst();

        assertEquals("Sistemas", plan.nombrePrograma());
        assertEquals("Activo", plan.estaActivoTextoPlanEstudio());
        assertEquals(10, plan.totalSemestres());
        assertEquals(40, plan.totalAsignaturas());
        assertEquals(120, plan.totalCreditos());
    }

    @Test
    void grupos_de_facultad_exponen_programas_como_lista_propia() {
        when(useCase.consultarGruposFacultad(u, null, null, null, 1, 20)).thenReturn(new PaginaGruposFacultadProjection(
                List.of(new GrupoFacultadProjection(id, 931001, "G1", id, id, "Calculo", id, 30, 12, 18, true, List.of("Sistemas", "Industrial"))), 1));

        final var grupo = interactor.consultarGruposFacultad(u, null, null, null, 1, 20).elementos().getFirst();

        assertEquals(List.of("Sistemas", "Industrial"), grupo.programas());
        assertEquals(931001, grupo.codigo());
        assertEquals(18, grupo.cuposDisponibles());
        assertTrue(grupo.grupoEstaHablitado());
    }
}
