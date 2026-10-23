package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteCandidatoDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocentesProgramaPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteCandidatoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocentesProgramaPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.interactor.ConsultarCandidatosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.ConsultarCandidatosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.ConsultarDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.interactor.ConsultarDocentesProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.ConsultarDocentesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.ConsultarDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.dto.VincularDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Interactors (puertos de entrada): delegan en el caso de uso y convierten dominio a DTO. */
class DocentesProgramaInteractorsTest {

    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void constructores_rechazan_caso_de_uso_nulo() {
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarDocentesProgramaInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarCandidatosDocenteInteractor(null));
    }

    @Test
    void vincular_traduce_dto_a_dominio_y_devuelve_dto() {
        final VincularDocenteProgramaUseCase useCase = mock(VincularDocenteProgramaUseCase.class);
        final UUID tipo = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        when(useCase.execute(any())).thenReturn(domain(docente));

        final DocenteProgramaDTO dto = new VincularDocenteProgramaInteractor(useCase).execute(
                new VincularDocenteProgramaDTO(ACTOR, docente, null, null, null, null, null, null, null, null, null, tipo));

        final ArgumentCaptor<VincularDocenteProgramaDomain> captor = ArgumentCaptor.forClass(VincularDocenteProgramaDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(docente, captor.getValue().idDocente());
        assertEquals(tipo, captor.getValue().idTipoVinculacion());
        assertEquals(docente, dto.idDocente());
        assertEquals("Jorge Henao", dto.nombreCompleto());
    }

    @Test
    void consultar_docentes_mapea_pagina() {
        final ConsultarDocentesProgramaUseCase useCase = mock(ConsultarDocentesProgramaUseCase.class);
        when(useCase.execute(any(), any())).thenReturn(new DocentesProgramaPaginaDomain(List.of(domain(UUID.randomUUID())), 7));

        final DocentesProgramaPaginaDTO pagina = new ConsultarDocentesProgramaInteractor(useCase)
                .execute(ACTOR, new ConsultarDocentesProgramaDTO(1, 10, "x", null, null));

        final ArgumentCaptor<ConsultarDocentesProgramaDomain> captor = ArgumentCaptor.forClass(ConsultarDocentesProgramaDomain.class);
        verify(useCase).execute(org.mockito.ArgumentMatchers.eq(ACTOR), captor.capture());
        assertEquals("x", captor.getValue().busqueda());
        assertEquals(7, pagina.total());
        assertEquals(1, pagina.elementos().size());
    }

    @Test
    void consultar_candidatos_mapea_lista() {
        final ConsultarCandidatosDocenteUseCase useCase = mock(ConsultarCandidatosDocenteUseCase.class);
        final UUID docente = UUID.randomUUID();
        when(useCase.execute(ACTOR, "ros")).thenReturn(List.of(new DocenteCandidatoDomain(docente, UUID.randomUUID(), "CC", 1, "Maria")));

        final List<DocenteCandidatoDTO> lista = new ConsultarCandidatosDocenteInteractor(useCase).execute(ACTOR, "ros");

        assertEquals(docente, lista.get(0).idDocente());
        assertEquals("Maria", lista.get(0).nombreCompleto());
    }

    private static DocenteProgramaDomain domain(final UUID docente) {
        return new DocenteProgramaDomain(UUID.randomUUID(), docente, UUID.randomUUID(), "CC", 1017555444, "Jorge", null,
                "Henao", null, "Jorge Henao", "jorge@uco.edu.co", UUID.randomUUID(), "CAT", "Cátedra",
                Instant.parse("2026-10-22T10:00:00Z"), 0, true);
    }
}
