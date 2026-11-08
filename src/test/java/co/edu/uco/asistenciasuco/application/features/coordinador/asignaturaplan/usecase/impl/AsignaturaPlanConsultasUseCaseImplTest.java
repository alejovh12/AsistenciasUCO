package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaPlanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAsignaturasPlanProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaGruposFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPlanesEstudioProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM: consultas paginadas por SP (malla, asignaturas del coordinador, planes, grupos del decano). */
class AsignaturaPlanConsultasUseCaseImplTest {

    private final AsignaturaPlanPort port = mock(AsignaturaPlanPort.class);
    private final AsignaturaPlanUseCaseImpl useCase = new AsignaturaPlanUseCaseImpl(port);
    private final UUID usuario = UUID.randomUUID();
    private final UUID plan = UUID.randomUUID();
    private final UUID programa = UUID.randomUUID();
    private final UUID periodo = UUID.randomUUID();

    @Test
    void malla_acota_pagina_y_tamano_y_delega() {
        final var pagina = new PaginaAsignaturasPlanProjection(List.of(), 0);
        when(port.consultarMalla(usuario, plan, 2, "OPT", "x", 1, 100)).thenReturn(pagina);

        assertSame(pagina, useCase.consultarMalla(usuario, plan, 2, "OPT", "x", 0, 5000));
        verify(port).consultarMalla(usuario, plan, 2, "OPT", "x", 1, 100);
        assertThrows(CrosscuttingException.class, () -> useCase.consultarMalla(null, plan, null, null, null, 1, 10));
        assertThrows(CrosscuttingException.class, () -> useCase.consultarMalla(usuario, null, null, null, null, 1, 10));
    }

    @Test
    void asignaturas_del_coordinador_traslada_el_programa_elegido() {
        final var pagina = new PaginaAsignaturasPlanProjection(List.of(), 0);
        when(port.consultarAsignaturasCoordinador(usuario, programa, "b", true, 3, 10)).thenReturn(pagina);

        assertSame(pagina, useCase.consultarAsignaturasCoordinador(usuario, programa, "b", true, 3, 10));
        assertThrows(CrosscuttingException.class, () -> useCase.consultarAsignaturasCoordinador(null, programa, null, null, 1, 10));
    }

    @Test
    void planes_traslada_programa_y_estado() {
        final var pagina = new PaginaPlanesEstudioProjection(List.of(), 0);
        when(port.consultarPlanes(usuario, programa, false, 1, 1)).thenReturn(pagina);

        assertSame(pagina, useCase.consultarPlanes(usuario, programa, false, -4, 0));
        assertThrows(CrosscuttingException.class, () -> useCase.consultarPlanes(null, null, null, 1, 10));
    }

    @Test
    void grupos_de_la_facultad_traslada_periodo_programa_y_busqueda() {
        final var pagina = new PaginaGruposFacultadProjection(List.of(), 0);
        when(port.consultarGruposFacultad(usuario, periodo, programa, "calc", 2, 20)).thenReturn(pagina);

        assertSame(pagina, useCase.consultarGruposFacultad(usuario, periodo, programa, "calc", 2, 20));
        assertThrows(CrosscuttingException.class, () -> useCase.consultarGruposFacultad(null, null, null, null, 1, 10));
    }
}
