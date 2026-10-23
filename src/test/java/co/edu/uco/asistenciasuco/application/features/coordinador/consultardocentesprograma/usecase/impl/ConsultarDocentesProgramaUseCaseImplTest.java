package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocentesProgramaPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.ConsultarDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU062/HU089/HU063: directorio paginado de docentes del programa del coordinador. */
class ConsultarDocentesProgramaUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DocenteProgramaQueryPort queryPort = mock(DocenteProgramaQueryPort.class);
    private final ConsultarDocentesProgramaUseCaseImpl useCase = new ConsultarDocentesProgramaUseCaseImpl(scopePort, queryPort);

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarDocentesProgramaUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarDocentesProgramaUseCaseImpl(scopePort, null));
    }

    @Test
    void sin_programa_del_coordinador_lanza_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(ACTOR, new ConsultarDocentesProgramaDomain(1, 10, null, null, null)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void T_062_01_resuelve_programa_por_ambito_traslada_filtros_y_mapea_pagina() {
        final UUID tipo = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(any(), any())).thenReturn(new DocentesProgramaPagina(List.of(fila()), 12));

        final DocentesProgramaPaginaDomain pagina = useCase.execute(ACTOR,
                new ConsultarDocentesProgramaDomain(2, 5, "ros", true, tipo));

        final ArgumentCaptor<DocentesProgramaFiltro> filtro = ArgumentCaptor.forClass(DocentesProgramaFiltro.class);
        verify(queryPort).consultar(org.mockito.ArgumentMatchers.eq(ACTOR), filtro.capture());
        assertEquals(PROGRAMA, filtro.getValue().idPrograma());
        assertEquals(2, filtro.getValue().pagina());
        assertEquals(5, filtro.getValue().tamano());
        assertEquals("ros", filtro.getValue().busqueda());
        assertEquals(Boolean.TRUE, filtro.getValue().estaActivo());
        assertEquals(tipo, filtro.getValue().idTipoVinculacion());
        assertEquals(12, pagina.total());
        assertEquals(1, pagina.elementos().size());
        final DocenteProgramaDomain item = pagina.elementos().get(0);
        assertEquals("Jorge Henao", item.nombreCompleto());
        assertEquals("CAT", item.codigoTipoVinculacion());
        assertTrue(item.estaActivo());
    }

    private static DocenteProgramaProjection fila() {
        return new DocenteProgramaProjection(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "CC", 1017555444,
                "Jorge", null, "Henao", null, "Jorge Henao", "jorge@uco.edu.co", UUID.randomUUID(), "CAT", "Cátedra",
                Instant.parse("2026-10-22T10:00:00Z"), 0, true);
    }
}
