package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.domain.CambiarEstadoProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static co.edu.uco.asistenciasuco.application.features.decano.common.ProgramaTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CambiarEstadoProgramaUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ProgramaAcademicoCommandPort commandPort = mock(ProgramaAcademicoCommandPort.class);
    private final ProgramaAcademicoQueryPort queryPort = mock(ProgramaAcademicoQueryPort.class);
    private final CambiarEstadoProgramaUseCaseImpl useCase = new CambiarEstadoProgramaUseCaseImpl(scopePort, commandPort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void T_078_01_envia_el_estado_destino_explicito_y_devuelve_el_programa_releido(final boolean destino) {
        when(queryPort.consultarPorId(PROGRAMA, FACULTAD)).thenReturn(Optional.of(projection()));

        final ProgramaAcademicoDomain result = useCase.execute(new CambiarEstadoProgramaDomain(ACTOR, PROGRAMA, destino));

        verify(commandPort).cambiarEstado(new ProgramaAcademicoCommandPort.CambiarEstadoProgramaCommand(PROGRAMA, destino, ACTOR));
        assertEquals(PROGRAMA, result.id());
    }

    @Test
    void T_SEC_01_sin_facultad_de_decano_responde_forbidden() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new CambiarEstadoProgramaDomain(ACTOR, PROGRAMA, false)));

        verifyNoInteractions(commandPort, queryPort);
    }

    @Test
    void valida_identificador_y_estado_destino() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new CambiarEstadoProgramaDomain(ACTOR, null, true)));
        assertThrows(ValidationException.class, () -> useCase.execute(new CambiarEstadoProgramaDomain(ACTOR, PROGRAMA, null)));
        verify(commandPort, never()).cambiarEstado(any());
    }

    @Test
    void si_el_programa_no_aparece_tras_cambiar_el_estado_responde_no_encontrado() {
        when(queryPort.consultarPorId(PROGRAMA, FACULTAD)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new CambiarEstadoProgramaDomain(ACTOR, PROGRAMA, true)));
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoProgramaUseCaseImpl(null, commandPort, queryPort));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoProgramaUseCaseImpl(scopePort, null, queryPort));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoProgramaUseCaseImpl(scopePort, commandPort, null));
    }
}
