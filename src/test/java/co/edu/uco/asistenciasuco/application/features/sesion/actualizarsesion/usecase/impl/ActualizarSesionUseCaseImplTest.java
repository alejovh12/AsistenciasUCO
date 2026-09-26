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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato TARGET (LB-001B.4A): solo Usuario.id como actor; findDocenteIdByUsuario(usuarioEjecutor)
 * es unicamente chequeo de rol docente y no se propaga al DTO del repositorio.
 * RED esperado: falla la compilacion (constructor de 5 parametros / record de 5 componentes).
 */
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
    void execute_lanza_forbidden_y_no_persiste_cuando_el_usuario_no_es_docente() {
        final UUID usuario = UUID.randomUUID();
        final ActualizarSesionDomain domain = new ActualizarSesionDomain(
                UUID.randomUUID(), "Sesion", LocalDateTime.of(2026, 1, 20, 8, 0),
                LocalDateTime.of(2026, 1, 20, 10, 0), usuario);
        when(institutionalScopePort.findDocenteIdByUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
        verify(sesionRepositoryPort, never()).actualizarSesion(any());
    }

    @Test
    void execute_verifica_rol_docente_con_usuarioEjecutor_y_persiste_sin_docente() {
        final UUID usuario = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 1, 20, 8, 0);
        final LocalDateTime fin = LocalDateTime.of(2026, 1, 20, 10, 0);
        final ActualizarSesionDomain domain = new ActualizarSesionDomain(sesion, "Sesion", inicio, fin, usuario);
        when(institutionalScopePort.findDocenteIdByUsuario(usuario)).thenReturn(Optional.of(UUID.randomUUID()));

        useCase.execute(domain);

        verify(institutionalScopePort).findDocenteIdByUsuario(usuario);
        final ArgumentCaptor<ActualizarSesionRepositoryDTO> captor = ArgumentCaptor.forClass(ActualizarSesionRepositoryDTO.class);
        verify(sesionRepositoryPort).actualizarSesion(captor.capture());
        assertEquals(new ActualizarSesionRepositoryDTO(sesion, "Sesion", inicio, fin, usuario), captor.getValue());
    }
}
