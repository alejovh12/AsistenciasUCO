package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearSesionUseCaseImplTest {

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final CrearSesionUseCaseImpl useCase =
            new CrearSesionUseCaseImpl(sesionRepositoryPort, institutionalScopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_docente_autenticado() {
        final UUID usuarioDocente = UUID.randomUUID();
        final CrearSesionDomain domain = new CrearSesionDomain(
                UUID.randomUUID(), "Tema principal", "Descripcion valida", LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                "Aula 1", "PRESENCIAL", usuarioDocente, usuarioDocente
        );
        when(institutionalScopePort.findDocenteIdByUsuario(usuarioDocente)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_reescribe_docente_con_el_id_resuelto_y_preserva_usuarioEjecutor() {
        final UUID usuarioDocente = UUID.randomUUID();
        final UUID docenteId = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final CrearSesionDomain domain = new CrearSesionDomain(
                grupo, "Tema principal", "Descripcion valida", LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                "Aula 1", "PRESENCIAL", usuarioDocente, usuarioDocente
        );
        when(institutionalScopePort.findDocenteIdByUsuario(usuarioDocente)).thenReturn(Optional.of(docenteId));

        useCase.execute(domain);

        final ArgumentCaptor<CrearSesionRepositoryDTO> captor = ArgumentCaptor.forClass(CrearSesionRepositoryDTO.class);
        verify(sesionRepositoryPort).crearSesion(captor.capture());
        assertEquals(grupo, captor.getValue().getGrupo());
        assertEquals(docenteId, captor.getValue().getDocente());
        assertEquals(usuarioDocente, captor.getValue().getUsuarioEjecutor());
        assertNotEquals(captor.getValue().getDocente(), captor.getValue().getUsuarioEjecutor());
    }
}
