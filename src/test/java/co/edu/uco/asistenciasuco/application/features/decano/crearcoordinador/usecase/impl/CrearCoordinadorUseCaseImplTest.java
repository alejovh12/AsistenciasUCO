package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCoordinadorUseCaseImplTest {

    private final CoordinadorCommandPort commandPort = mock(CoordinadorCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CrearCoordinadorUseCaseImpl useCase = new CrearCoordinadorUseCaseImpl(commandPort, scopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_facultad_del_decano() {
        final UUID usuario = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", UUID.randomUUID(), "HASH", usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_crea_coordinador_con_facultad_resuelta() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", programa, "HASH", usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.of(facultad));

        useCase.execute(domain);

        final ArgumentCaptor<UUID> facultadCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("123456789"),
                org.mockito.ArgumentMatchers.eq("ANA"), org.mockito.ArgumentMatchers.eq("MARIA"),
                org.mockito.ArgumentMatchers.eq("PEREZ"), org.mockito.ArgumentMatchers.eq("GOMEZ"),
                org.mockito.ArgumentMatchers.eq("ana@uco.edu.co"), org.mockito.ArgumentMatchers.eq(programa),
                facultadCaptor.capture(), org.mockito.ArgumentMatchers.eq("HASH"));
        assertEquals(facultad, facultadCaptor.getValue());
    }
}
