package co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.domain.ActualizarProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ActualizarProgramaUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ProgramaAcademicoCommandPort commandPort = mock(ProgramaAcademicoCommandPort.class);
    private final ProgramaAcademicoQueryPort queryPort = mock(ProgramaAcademicoQueryPort.class);
    private final ActualizarProgramaUseCaseImpl useCase = new ActualizarProgramaUseCaseImpl(scopePort, commandPort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
    }

    private static ActualizarProgramaDomain domain(final UUID id, final String nombre, final UUID tipo, final UUID coordinador) {
        return new ActualizarProgramaDomain(ACTOR, id, nombre, tipo, coordinador, MODALIDAD, JORNADA);
    }

    @Test
    void T_077_01_envia_el_reemplazo_completo_y_devuelve_el_programa_releido() {
        when(queryPort.consultarPorId(PROGRAMA, FACULTAD)).thenReturn(Optional.of(projection()));

        final ProgramaAcademicoDomain result = useCase.execute(domain(PROGRAMA, " Sistemas ", TIPO, COORDINADOR));

        final ArgumentCaptor<ProgramaAcademicoCommandPort.ActualizarProgramaCommand> command =
                ArgumentCaptor.forClass(ProgramaAcademicoCommandPort.ActualizarProgramaCommand.class);
        verify(commandPort).actualizar(command.capture());
        assertEquals(PROGRAMA, command.getValue().id());
        assertEquals("Sistemas", command.getValue().nombre());
        assertEquals(TIPO, command.getValue().idTipoPrograma());
        assertEquals(COORDINADOR, command.getValue().idCoordinador());
        assertEquals(MODALIDAD, command.getValue().idModalidad());
        assertEquals(JORNADA, command.getValue().idJornada());
        assertEquals(ACTOR, command.getValue().actorUsuarioId());
        assertEquals(PROGRAMA, result.id());
    }

    @Test
    void T_SEC_01_sin_facultad_de_decano_responde_forbidden() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(PROGRAMA, "Sistemas", TIPO, COORDINADOR)));

        verifyNoInteractions(commandPort, queryPort);
    }

    @Test
    void valida_forma_antes_de_llamar_al_puerto() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(null, "Sistemas", TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(PROGRAMA, " ", TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(PROGRAMA, null, TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(PROGRAMA, "X".repeat(51), TIPO, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(PROGRAMA, "Sistemas", null, COORDINADOR)));
        assertThrows(ValidationException.class, () -> useCase.execute(domain(PROGRAMA, "Sistemas", TIPO, null)));
        verify(commandPort, never()).actualizar(any());
    }

    @Test
    void si_el_programa_no_aparece_tras_actualizar_responde_no_encontrado() {
        when(queryPort.consultarPorId(PROGRAMA, FACULTAD)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(domain(PROGRAMA, "Sistemas", TIPO, COORDINADOR)));
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ActualizarProgramaUseCaseImpl(null, commandPort, queryPort));
        assertThrows(NullPointerException.class, () -> new ActualizarProgramaUseCaseImpl(scopePort, null, queryPort));
        assertThrows(NullPointerException.class, () -> new ActualizarProgramaUseCaseImpl(scopePort, commandPort, null));
    }
}
