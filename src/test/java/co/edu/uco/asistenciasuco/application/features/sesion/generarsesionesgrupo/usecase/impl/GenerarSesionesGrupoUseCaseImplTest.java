package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GenerarSesionesGrupoUseCaseImplTest {

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final GenerarSesionesGrupoUseCaseImpl useCase = new GenerarSesionesGrupoUseCaseImpl(sesionRepositoryPort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_propaga_grupo_y_usuarioEjecutor_al_repositorio() {
        final UUID grupo = UUID.randomUUID();
        final UUID usuarioEjecutor = UUID.randomUUID();
        final GenerarSesionesGrupoDomain domain = new GenerarSesionesGrupoDomain(grupo, usuarioEjecutor);

        useCase.execute(domain);

        final ArgumentCaptor<GenerarSesionesGrupoRepositoryDTO> captor =
                ArgumentCaptor.forClass(GenerarSesionesGrupoRepositoryDTO.class);
        verify(sesionRepositoryPort).generarSesionesGrupo(captor.capture());
        assertEquals(grupo, captor.getValue().grupo());
        assertEquals(usuarioEjecutor, captor.getValue().usuarioEjecutor());
    }
}
