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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato TARGET (LB-001B.4A): el caso de uso solo lleva Usuario.id como actor. La resolucion
 * findDocenteIdByUsuario(usuarioEjecutor) es unicamente un chequeo de rol docente: su resultado
 * NO se propaga al DTO del repositorio (que ya no tiene docente).
 * RED esperado: falla la compilacion (constructor de 5 parametros / getNombre inexistentes).
 */
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
    void execute_lanza_forbidden_y_no_persiste_cuando_el_usuario_no_es_docente() {
        final UUID usuario = UUID.randomUUID();
        final CrearSesionDomain domain = new CrearSesionDomain(
                UUID.randomUUID(), "Sesion principal", LocalDateTime.now(), LocalDateTime.now().plusHours(1), usuario);
        when(institutionalScopePort.findDocenteIdByUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
        verify(sesionRepositoryPort, never()).crearSesion(any());
    }

    @Test
    void execute_verifica_rol_docente_con_usuarioEjecutor_y_persiste_sin_docente() {
        final UUID usuario = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 1, 20, 8, 0);
        final LocalDateTime fin = inicio.plusHours(2);
        final CrearSesionDomain domain = new CrearSesionDomain(grupo, "Sesion principal", inicio, fin, usuario);
        when(institutionalScopePort.findDocenteIdByUsuario(usuario)).thenReturn(Optional.of(UUID.randomUUID()));

        useCase.execute(domain);

        verify(institutionalScopePort).findDocenteIdByUsuario(usuario);
        final ArgumentCaptor<CrearSesionRepositoryDTO> captor = ArgumentCaptor.forClass(CrearSesionRepositoryDTO.class);
        verify(sesionRepositoryPort).crearSesion(captor.capture());
        assertEquals(grupo, captor.getValue().getGrupo());
        assertEquals("Sesion principal", captor.getValue().getNombre());
        assertEquals(inicio, captor.getValue().getFechaHoraInicio());
        assertEquals(fin, captor.getValue().getFechaHoraFin());
        assertEquals(usuario, captor.getValue().getUsuarioEjecutor());
    }
}
