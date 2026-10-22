package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static co.edu.uco.asistenciasuco.application.features.decano.common.ProgramaTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProgramaUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ProgramaAcademicoCommandPort commandPort = mock(ProgramaAcademicoCommandPort.class);
    private final ProgramaAcademicoQueryPort queryPort = mock(ProgramaAcademicoQueryPort.class);
    private final CrearProgramaUseCaseImpl useCase = new CrearProgramaUseCaseImpl(scopePort, commandPort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
    }

    private static CrearProgramaDomain domain(final String nombre, final UUID tipo, final UUID coordinador) {
        return new CrearProgramaDomain(ACTOR, nombre, tipo, coordinador, MODALIDAD, JORNADA);
    }

    @Test
    void T_076_01_crea_en_la_facultad_del_decano_con_id_generado_y_relee_el_programa() {
        when(queryPort.consultarPorId(any(), any())).thenReturn(Optional.of(projection()));

        final ProgramaAcademicoDomain result = useCase.execute(domain("  Ingenieria de Sistemas  ", TIPO, COORDINADOR));

        final ArgumentCaptor<ProgramaAcademicoCommandPort.CrearProgramaCommand> command =
                ArgumentCaptor.forClass(ProgramaAcademicoCommandPort.CrearProgramaCommand.class);
        verify(commandPort).crear(command.capture());
        assertNotNull(command.getValue().id());
        assertEquals(FACULTAD, command.getValue().idFacultad());
        assertEquals("Ingenieria de Sistemas", command.getValue().nombre());
        assertEquals(TIPO, command.getValue().idTipoPrograma());
        assertEquals(COORDINADOR, command.getValue().idCoordinador());
        assertEquals(MODALIDAD, command.getValue().idModalidad());
        assertEquals(JORNADA, command.getValue().idJornada());
        assertEquals(ACTOR, command.getValue().actorUsuarioId());
        verify(queryPort).consultarPorId(command.getValue().id(), FACULTAD);
        assertEquals(PROGRAMA, result.id());
        assertEquals("Presencial", result.nombreModalidad());
        assertTrue(result.estaActivo());
    }

    @Test
    void modalidad_y_jornada_son_opcionales() {
        when(queryPort.consultarPorId(any(), any())).thenReturn(Optional.of(projection()));

        useCase.execute(new CrearProgramaDomain(ACTOR, "Sistemas", TIPO, COORDINADOR, null, null));

        final ArgumentCaptor<ProgramaAcademicoCommandPort.CrearProgramaCommand> command =
                ArgumentCaptor.forClass(ProgramaAcademicoCommandPort.CrearProgramaCommand.class);
        verify(commandPort).crear(command.capture());
        assertNull(command.getValue().idModalidad());
        assertNull(command.getValue().idJornada());
    }

    @Test
    void T_SEC_01_sin_facultad_de_decano_responde_forbidden_sin_tocar_la_db() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain("Sistemas", TIPO, COORDINADOR)));

        verifyNoInteractions(commandPort, queryPort);
    }

    @Test
    void T_076_12_valida_forma_antes_de_llamar_al_puerto() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(null, TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain("   ", TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain("X".repeat(51), TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain("Sistemas", null, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain("Sistemas", TIPO, null)));
        verify(commandPort, never()).crear(any());
    }

    @Test
    void acepta_un_nombre_de_exactamente_50_caracteres() {
        when(queryPort.consultarPorId(any(), any())).thenReturn(Optional.of(projection()));

        useCase.execute(domain("X".repeat(50), TIPO, COORDINADOR));

        verify(commandPort).crear(any());
    }

    @Test
    void si_el_programa_no_se_puede_releer_tras_crear_es_un_error_interno() {
        when(queryPort.consultarPorId(any(), any())).thenReturn(Optional.empty());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain("Sistemas", TIPO, COORDINADOR)));
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new CrearProgramaUseCaseImpl(null, commandPort, queryPort));
        assertThrows(NullPointerException.class, () -> new CrearProgramaUseCaseImpl(scopePort, null, queryPort));
        assertThrows(NullPointerException.class, () -> new CrearProgramaUseCaseImpl(scopePort, commandPort, null));
    }
}
