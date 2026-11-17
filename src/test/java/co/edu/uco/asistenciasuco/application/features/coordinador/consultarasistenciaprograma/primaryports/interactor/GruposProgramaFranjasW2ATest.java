package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.ConsultarAsistenciaProgramaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.FranjaHorarioRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CIERRE-W2A (punto 1): el grupo del programa entrega su horario semanal y la pagina conserva total, numero y tamano. */
class GruposProgramaFranjasW2ATest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final ConsultarAsistenciaProgramaUseCase useCase = mock(ConsultarAsistenciaProgramaUseCase.class);
    private final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);

    private static GrupoProgramaProjection grupo(final UUID id, final List<FranjaHorarioRepositoryProjection> franjas) {
        return new GrupoProgramaProjection(id, 1, "G1", UUID.randomUUID(), "Calculo", UUID.randomUUID(), 30, 20, 10, true,
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5), franjas);
    }

    @Test
    void las_franjas_salen_como_dto_con_hora_hh_mm_y_la_pagina_conserva_sus_metadatos() {
        when(useCase.consultarGrupos(USUARIO, null, "calc", 2, 5)).thenReturn(new Pagina<>(List.of(grupo(GRUPO, List.of(
                new FranjaHorarioRepositoryProjection(GRUPO, "LU", "Lunes", LocalTime.of(8, 0), LocalTime.of(10, 30))))), 11));

        final var pagina = interactor.consultarGrupos(USUARIO, null, "calc", 2, 5);

        assertEquals(11, pagina.total());
        assertEquals(2, pagina.pagina());
        assertEquals(5, pagina.tamano());
        final var franja = pagina.elementos().getFirst().franjas().getFirst();
        assertEquals("LU", franja.codigoDia());
        assertEquals("Lunes", franja.nombreDia());
        assertEquals("08:00", franja.horaInicio());
        assertEquals("10:30", franja.horaFin());
    }

    @Test
    void un_grupo_sin_horario_entrega_lista_vacia_y_los_valores_por_defecto_de_pagina() {
        when(useCase.consultarGrupos(USUARIO, null, null, null, null)).thenReturn(new Pagina<>(List.of(grupo(GRUPO, null)), 1));

        final var pagina = interactor.consultarGrupos(USUARIO, null, null, null, null);

        assertTrue(pagina.elementos().getFirst().franjas().isEmpty());
        assertEquals(1, pagina.pagina());
        assertEquals(10, pagina.tamano());
    }
}
