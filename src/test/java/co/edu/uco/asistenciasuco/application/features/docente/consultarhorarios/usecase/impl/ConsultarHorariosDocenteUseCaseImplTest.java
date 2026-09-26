package co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.domain.HorarioDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHorariosDocenteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final HorarioDocenteQueryPort queryPort = mock(HorarioDocenteQueryPort.class);
    private final ConsultarHorariosDocenteUseCaseImpl useCase =
            new ConsultarHorariosDocenteUseCaseImpl(scopePort, queryPort);

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_docente() {
        final UUID actor = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(actor));
    }

    @Test
    void execute_consulta_horarios_del_docente_resuelto() {
        final UUID actor = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(docente));
        when(queryPort.consultarHorarioDocente(docente)).thenReturn(List.of(new HorarioDocenteProjection(
                id, docente, UUID.randomUUID(), "MAT-01", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), 25)));

        final List<HorarioDocenteDomain> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals(id, result.getFirst().id());
    }
}
