package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarSesionesMateriaEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionMateriaEstudianteQueryPort queryPort = mock(SesionMateriaEstudianteQueryPort.class);
    private final ConsultarSesionesMateriaEstudianteUseCaseImpl useCase =
            new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_estudiante() {
        final UUID actor = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor, asignatura));
    }

    @Test
    void execute_consulta_sesiones_del_estudiante_resuelto() {
        final UUID actor = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(estudiante, asignatura)).thenReturn(List.of(
                new SesionMateriaEstudianteProjection(id, "Sesion 1", 1, "SES-01", 1, UUID.randomUUID(), "G1", "Grupo 1",
                        LocalDateTime.of(2026, 1, 20, 8, 0), LocalDateTime.of(2026, 1, 20, 10, 0))));

        final List<SesionMateriaEstudianteDomain> result = useCase.execute(actor, asignatura);

        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().id());
    }
}
