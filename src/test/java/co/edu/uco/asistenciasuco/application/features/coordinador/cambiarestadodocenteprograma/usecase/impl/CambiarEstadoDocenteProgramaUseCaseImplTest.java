package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.domain.CambiarEstadoDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.CambiarEstadoDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU093 (D12): activar/inactivar el VINCULO con el programa. El caso de uso no depende de ningun puerto de identidad:
 * la cuenta de acceso no se deshabilita (la persona puede tener otros roles o programas).
 */
class CambiarEstadoDocenteProgramaUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DocenteProgramaCommandPort commandPort = mock(DocenteProgramaCommandPort.class);
    private final DocenteProgramaQueryPort queryPort = mock(DocenteProgramaQueryPort.class);
    private final CambiarEstadoDocenteProgramaUseCaseImpl useCase = new CambiarEstadoDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort);

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDocenteProgramaUseCaseImpl(null, commandPort, queryPort));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDocenteProgramaUseCaseImpl(scopePort, null, queryPort));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDocenteProgramaUseCaseImpl(scopePort, commandPort, null));
    }

    @Test
    void rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void sin_programa_del_coordinador_lanza_forbidden_y_no_llama_a_la_db() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new CambiarEstadoDocenteProgramaDomain(ACTOR, DOCENTE, false)));

        verifyNoInteractions(commandPort, queryPort);
    }

    @Test
    void T_093_01_inactiva_con_el_programa_del_ambito_y_relee_el_vinculo() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(fila(false)), 1));

        final DocenteProgramaDomain resultado = useCase.execute(new CambiarEstadoDocenteProgramaDomain(ACTOR, DOCENTE, false));

        final ArgumentCaptor<CambiarEstadoDocenteProgramaCommand> comando = ArgumentCaptor.forClass(CambiarEstadoDocenteProgramaCommand.class);
        verify(commandPort).cambiarEstado(comando.capture());
        assertEquals(DOCENTE, comando.getValue().idDocente());
        assertEquals(PROGRAMA, comando.getValue().idPrograma());
        assertFalse(comando.getValue().estaActivo());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());

        final ArgumentCaptor<DocentesProgramaFiltro> filtro = ArgumentCaptor.forClass(DocentesProgramaFiltro.class);
        verify(queryPort).consultar(eq(ACTOR), filtro.capture());
        assertEquals(DOCENTE, filtro.getValue().idDocente());
        assertFalse(resultado.estaActivo());
    }

    @Test
    void T_093_02_reactivar_envia_estaActivo_true() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(fila(true)), 1));

        final DocenteProgramaDomain resultado = useCase.execute(new CambiarEstadoDocenteProgramaDomain(ACTOR, DOCENTE, true));

        final ArgumentCaptor<CambiarEstadoDocenteProgramaCommand> comando = ArgumentCaptor.forClass(CambiarEstadoDocenteProgramaCommand.class);
        verify(commandPort).cambiarEstado(comando.capture());
        assertEquals(true, comando.getValue().estaActivo());
        assertEquals(true, resultado.estaActivo());
    }

    @Test
    void vinculo_no_encontrado_tras_cambiar_es_error_interno() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(), 0));

        assertThrows(InternalApplicationException.class,
                () -> useCase.execute(new CambiarEstadoDocenteProgramaDomain(ACTOR, DOCENTE, false)));
    }

    private static DocenteProgramaProjection fila(final boolean activo) {
        return new DocenteProgramaProjection(UUID.randomUUID(), DOCENTE, UUID.randomUUID(), "CC", 1017555444, "Jorge", null,
                "Henao", null, "Jorge Henao", "x@uco.edu.co", UUID.randomUUID(), "CAT", "Cátedra",
                Instant.parse("2026-10-22T10:00:00Z"), 2, activo);
    }
}
