package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAsignaturasAsociablesProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaPlanPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM: orquestacion de las asignaturas por plan; el ambito vive en el SP, aqui solo se validan entradas. */
class AsignaturaPlanUseCaseImplTest {

    private final AsignaturaPlanPort port = mock(AsignaturaPlanPort.class);
    private final AsignaturaPlanUseCaseImpl useCase = new AsignaturaPlanUseCaseImpl(port);
    private final UUID usuario = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();
    private final UUID asignatura = UUID.randomUUID();

    @Test
    void constructor_exige_el_puerto() {
        assertThrows(NullPointerException.class, () -> new AsignaturaPlanUseCaseImpl(null));
    }

    @Test
    void asociar_delega_en_el_puerto() {
        useCase.asociar(usuario, plan, asignatura, 3, "OPT");
        verify(port).asociar(usuario, plan, asignatura, 3, "OPT");
    }

    @Test
    void asociar_rechaza_entradas_nulas() {
        assertThrows(CrosscuttingException.class, () -> useCase.asociar(null, plan, asignatura, 3, null));
        assertThrows(CrosscuttingException.class, () -> useCase.asociar(usuario, null, asignatura, 3, null));
        assertThrows(CrosscuttingException.class, () -> useCase.asociar(usuario, plan, null, 3, null));
        assertThrows(CrosscuttingException.class, () -> useCase.asociar(usuario, plan, asignatura, null, null));
        verify(port, never()).asociar(any(), any(), any(), any(), any());
    }

    @Test
    void desvincular_delega_y_valida() {
        useCase.desvincular(usuario, plan, asignatura);
        verify(port).desvincular(usuario, plan, asignatura);
        assertThrows(CrosscuttingException.class, () -> useCase.desvincular(null, plan, asignatura));
        assertThrows(CrosscuttingException.class, () -> useCase.desvincular(usuario, null, asignatura));
        assertThrows(CrosscuttingException.class, () -> useCase.desvincular(usuario, plan, null));
    }

    @Test
    void asociables_acota_la_paginacion() {
        final var pagina = new PaginaAsignaturasAsociablesProjection(List.of(), 0);
        when(port.consultarAsociables(eq(usuario), eq(plan), eq("x"), anyInt(), anyInt())).thenReturn(pagina);
        assertEquals(pagina, useCase.consultarAsociables(usuario, plan, "x", 0, 500));
        verify(port).consultarAsociables(usuario, plan, "x", 1, 100);
        useCase.consultarAsociables(usuario, plan, "x", 2, 0);
        verify(port).consultarAsociables(usuario, plan, "x", 2, 1);
        assertThrows(CrosscuttingException.class, () -> useCase.consultarAsociables(null, plan, null, 1, 10));
        assertThrows(CrosscuttingException.class, () -> useCase.consultarAsociables(usuario, null, null, 1, 10));
    }

    @Test
    void caracteres_y_programas_del_grupo_delegan() {
        final UUID grupo = UUID.randomUUID();
        useCase.consultarCaracteres();
        verify(port).consultarCaracteres();
        useCase.consultarProgramasGrupo(usuario, grupo);
        verify(port).consultarProgramasGrupo(usuario, grupo);
        assertThrows(CrosscuttingException.class, () -> useCase.consultarProgramasGrupo(null, grupo));
        assertThrows(CrosscuttingException.class, () -> useCase.consultarProgramasGrupo(usuario, null));
    }
}
