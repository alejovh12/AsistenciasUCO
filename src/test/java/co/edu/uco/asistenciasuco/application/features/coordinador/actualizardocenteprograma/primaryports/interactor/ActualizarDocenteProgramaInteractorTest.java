package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto.ActualizarDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.ActualizarDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain.ActualizarDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActualizarDocenteProgramaInteractorTest {

    @Test
    void rechaza_caso_de_uso_nulo() {
        assertThrows(NullPointerException.class, () -> new ActualizarDocenteProgramaInteractor(null));
    }

    @Test
    void traduce_dto_a_dominio_y_devuelve_dto() {
        final ActualizarDocenteProgramaUseCase useCase = mock(ActualizarDocenteProgramaUseCase.class);
        final UUID actor = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        final UUID tipo = UUID.randomUUID();
        when(useCase.execute(any())).thenReturn(new DocenteProgramaDomain(UUID.randomUUID(), docente, UUID.randomUUID(), "CC",
                1, "Nuevo", null, "Apellido", null, "Nuevo Apellido", "x@uco.edu.co", tipo, "PLA", "Planta",
                Instant.parse("2026-10-22T10:00:00Z"), 1, true));

        final DocenteProgramaDTO dto = new ActualizarDocenteProgramaInteractor(useCase)
                .execute(new ActualizarDocenteProgramaDTO(actor, docente, tipo, "Nuevo", "S", "Apellido", "T"));

        final ArgumentCaptor<ActualizarDocenteProgramaDomain> captor = ArgumentCaptor.forClass(ActualizarDocenteProgramaDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(actor, captor.getValue().actorUsuarioId());
        assertEquals(docente, captor.getValue().idDocente());
        assertEquals(tipo, captor.getValue().idTipoVinculacion());
        assertEquals("S", captor.getValue().segundoNombre());
        assertEquals("T", captor.getValue().segundoApellido());
        assertEquals("Nuevo Apellido", dto.nombreCompleto());
    }
}
