package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionesMateriaEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.interactor.ConsultarSesionesMateriaEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W2B: el interactor conserva el orden de la base y el total real; las firmas anteriores siguen devolviendo la lista. */
class ConsultarSesionesMateriaEstudianteInteractorTest {

    private final ConsultarSesionesMateriaEstudianteUseCase useCase = mock(ConsultarSesionesMateriaEstudianteUseCase.class);
    private final ConsultarSesionesMateriaEstudianteInteractor interactor = new ConsultarSesionesMateriaEstudianteInteractor(useCase);
    private final UUID actor = UUID.randomUUID();
    private final UUID materia = UUID.randomUUID();

    private static SesionMateriaEstudianteDomain dominio(final UUID id, final int numero) {
        return new SesionMateriaEstudianteDomain(id, "Sesion " + numero, numero, "SES-" + numero, numero, UUID.randomUUID(), "1", "Grupo 1",
                java.time.LocalDateTime.of(2026, 8, 17, 13, 0), java.time.LocalDateTime.of(2026, 8, 17, 15, 0), null, null,
                co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion.INDETERMINADA,
                EstadoAsistenciaSesion.PRESENTE, null, false);
    }

    @Test
    void consultarPagina_mantiene_el_orden_y_el_total_real() {
        final UUID grupo = UUID.randomUUID();
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        when(useCase.consultarPagina(actor, materia, grupo, 2, 2)).thenReturn(new PaginaResultado<>(List.of(dominio(a, 5), dominio(b, 3)), 2, 2, 11));

        final SesionesMateriaEstudiantePaginaDTO pagina = interactor.consultarPagina(actor, materia, grupo, 2, 2);

        assertEquals(11L, pagina.totalRegistros());
        assertEquals(List.of(a, b), pagina.sesiones().stream().map(s -> s.id()).toList());
        verify(useCase).consultarPagina(actor, materia, grupo, 2, 2);
    }

    @Test
    void las_firmas_anteriores_devuelven_solo_la_lista_de_la_pagina() {
        final UUID a = UUID.randomUUID();
        when(useCase.consultarPagina(actor, materia, null, null, null)).thenReturn(new PaginaResultado<>(List.of(dominio(a, 1)), 1, 100, 1));
        when(useCase.consultarPagina(actor, materia, null, 3, 4)).thenReturn(new PaginaResultado<>(List.of(), 3, 4, 0));

        assertEquals(1, interactor.execute(actor, materia).size());
        assertEquals(0, interactor.execute(actor, materia, 3, 4).size());
    }

    @Test
    void la_pagina_del_dto_es_inmutable_y_nunca_nula() {
        final var vacia = new SesionesMateriaEstudiantePaginaDTO(null, 0);

        assertEquals(List.of(), vacia.sesiones());
        final var sesiones = vacia.sesiones();
        assertThrows(UnsupportedOperationException.class, () -> sesiones.add(null));
    }

    @Test
    void el_caso_de_uso_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ConsultarSesionesMateriaEstudianteInteractor(null));
    }
}
