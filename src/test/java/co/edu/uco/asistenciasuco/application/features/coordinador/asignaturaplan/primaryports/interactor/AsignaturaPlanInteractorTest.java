package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.AsignaturaPlanUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM: el interactor traduce DTO <-> proyecciones y delega en el caso de uso. */
class AsignaturaPlanInteractorTest {

    private final AsignaturaPlanUseCase useCase = mock(AsignaturaPlanUseCase.class);
    private final AsignaturaPlanInteractor interactor = new AsignaturaPlanInteractor(useCase);
    private final UUID u = UUID.randomUUID();
    private final UUID p = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();

    @Test
    void constructor_exige_el_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new AsignaturaPlanInteractor(null));
    }

    @Test
    void asociar_traduce_el_dto() {
        interactor.asociar(u, p, new AsociarAsignaturaPlanDTO(a, 2, "OPT"));
        verify(useCase).asociar(u, p, a, 2, "OPT");
        assertThrows(CrosscuttingException.class, () -> interactor.asociar(u, p, null));
    }

    @Test
    void desvincular_delega() {
        interactor.desvincular(u, p, a);
        verify(useCase).desvincular(u, p, a);
    }

    @Test
    void asociables_mapea_la_pagina() {
        when(useCase.consultarAsociables(u, p, "x", 1, 10)).thenReturn(new PaginaAsignaturasAsociablesProjection(
                List.of(new AsignaturaAsociableProjection(a, "C", "N", 3, "Ar", "Co", "P1")), 7));
        final var r = interactor.consultarAsociables(u, p, "x", 1, 10);
        assertEquals(7, r.total());
        assertEquals("P1", r.elementos().getFirst().planes());
    }

    @Test
    void caracteres_y_programas_se_mapean() {
        final UUID g = UUID.randomUUID();
        when(useCase.consultarCaracteres()).thenReturn(List.of(new CaracterAsignaturaProjection("OBL", "Obligatoria", 1)));
        when(useCase.consultarProgramasGrupo(u, g)).thenReturn(List.of(new ProgramaGrupoProjection(p, "Prog", a, 2024)));
        assertEquals("OBL", interactor.consultarCaracteres().getFirst().codigo());
        assertEquals("Prog", interactor.consultarProgramasGrupo(u, g).getFirst().nombrePrograma());
    }
}
