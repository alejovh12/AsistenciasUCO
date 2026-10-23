package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.dto.CambiarEstadoDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.CambiarEstadoDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.domain.CambiarEstadoDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CambiarEstadoDocenteProgramaInteractorTest {

    @Test
    void rechaza_caso_de_uso_nulo() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDocenteProgramaInteractor(null));
    }

    @Test
    void traduce_dto_a_dominio_y_devuelve_dto() {
        final CambiarEstadoDocenteProgramaUseCase useCase = mock(CambiarEstadoDocenteProgramaUseCase.class);
        final UUID actor = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        when(useCase.execute(any())).thenReturn(new DocenteProgramaDomain(UUID.randomUUID(), docente, UUID.randomUUID(), "CC",
                1, "Ana", null, "Perez", null, "Ana Perez", "x@uco.edu.co", UUID.randomUUID(), "CAT", "Cátedra",
                Instant.parse("2026-10-22T10:00:00Z"), 0, false));

        final DocenteProgramaDTO dto = new CambiarEstadoDocenteProgramaInteractor(useCase)
                .execute(new CambiarEstadoDocenteProgramaDTO(actor, docente, false));

        final ArgumentCaptor<CambiarEstadoDocenteProgramaDomain> captor = ArgumentCaptor.forClass(CambiarEstadoDocenteProgramaDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(actor, captor.getValue().actorUsuarioId());
        assertEquals(docente, captor.getValue().idDocente());
        assertFalse(captor.getValue().estaActivo());
        assertFalse(dto.estaActivo());
    }
}
