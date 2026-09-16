package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActualizarSesionUseCaseImplTest {

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final ActualizarSesionUseCaseImpl useCase =
            new ActualizarSesionUseCaseImpl(sesionRepositoryPort, institutionalScopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_docente_autenticado() {
        final UUID usuarioDocente = UUID.randomUUID();
        final ActualizarSesionDomain domain = new ActualizarSesionDomain(
                UUID.randomUUID(), "Sesion", LocalDateTime.of(2026, 1, 20, 8, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0), "Aula 1", "Descripcion", usuarioDocente, usuarioDocente);
        when(institutionalScopePort.findDocenteIdByUsuario(usuarioDocente)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_reescribe_docente_con_el_id_resuelto_y_preserva_usuarioEjecutor() {
        final UUID usuarioDocente = UUID.randomUUID();
        final UUID docenteId = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final ActualizarSesionDomain domain = new ActualizarSesionDomain(
                sesion, "Sesion", LocalDateTime.of(2026, 1, 20, 8, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0), "Aula 1", "Descripcion", usuarioDocente, usuarioDocente);
        when(institutionalScopePort.findDocenteIdByUsuario(usuarioDocente)).thenReturn(Optional.of(docenteId));

        useCase.execute(domain);

        final ArgumentCaptor<ActualizarSesionRepositoryDTO> captor = ArgumentCaptor.forClass(ActualizarSesionRepositoryDTO.class);
        verify(sesionRepositoryPort).actualizarSesion(captor.capture());
        assertEquals(sesion, captor.getValue().sesion());
        assertEquals(docenteId, captor.getValue().docente());
        assertEquals(usuarioDocente, captor.getValue().usuarioEjecutor());
    }
}
