package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain.CerrarSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CerrarSesionUseCaseImplTest {

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final CerrarSesionUseCaseImpl useCase =
            new CerrarSesionUseCaseImpl(sesionRepositoryPort, institutionalScopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_docente_autenticado() {
        final UUID usuarioDocente = UUID.randomUUID();
        final CerrarSesionDomain domain = new CerrarSesionDomain(
                UUID.randomUUID(), usuarioDocente, "Cierre a tiempo", usuarioDocente
        );
        when(institutionalScopePort.findDocenteIdByUsuario(usuarioDocente)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_reescribe_docente_con_el_id_resuelto_y_preserva_usuarioEjecutor() {
        final UUID usuarioDocente = UUID.randomUUID();
        final UUID docenteId = UUID.randomUUID();
        final UUID sesion = UUID.randomUUID();
        final CerrarSesionDomain domain = new CerrarSesionDomain(sesion, usuarioDocente, "Cierre a tiempo", usuarioDocente);
        when(institutionalScopePort.findDocenteIdByUsuario(usuarioDocente)).thenReturn(Optional.of(docenteId));

        useCase.execute(domain);

        final ArgumentCaptor<CerrarSesionRepositoryDTO> captor = ArgumentCaptor.forClass(CerrarSesionRepositoryDTO.class);
        verify(sesionRepositoryPort).cerrarSesion(captor.capture());
        assertEquals(sesion, captor.getValue().getSesion());
        assertEquals(docenteId, captor.getValue().getDocente());
        assertEquals(usuarioDocente, captor.getValue().getUsuarioEjecutor());
        assertNotEquals(captor.getValue().getDocente(), captor.getValue().getUsuarioEjecutor());
    }
}
