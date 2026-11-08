package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.ConsultarEstudiantesProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.EstudianteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaEstudiantesProgramaProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 B2NM (O2-B2, S4): el directorio de estudiantes del coordinador sale del SP paginado; el ambito vive en la BD. */
class ConsultarEstudiantesProgramaUseCaseImplTest {

    private final EstudianteProgramaQueryPort port = mock(EstudianteProgramaQueryPort.class);
    private final ConsultarEstudiantesProgramaUseCaseImpl useCase = new ConsultarEstudiantesProgramaUseCaseImpl(port);
    private final UUID actor = UUID.randomUUID();
    private final UUID programa = UUID.randomUUID();

    @Test
    void traslada_programa_filtros_y_mapea_las_filas() {
        final UUID id = UUID.randomUUID();
        when(port.consultarDirectorioPrograma(actor, programa, "ana", true, 2, 10)).thenReturn(new PaginaEstudiantesProgramaProjection(
                List.of(new EstudianteProgramaProjection(id, id, "101", "Ana Perez", "ana@uco.edu.co", programa, "Sistemas")), 41));

        final var pagina = useCase.execute(new ConsultarEstudiantesProgramaDomain(actor, programa, " ana ", true, 2, 10));

        assertEquals(41, pagina.total());
        assertEquals("Ana Perez", pagina.elementos().getFirst().nombreCompleto());
        assertEquals(programa, pagina.elementos().getFirst().idPrograma());
        verify(port).consultarDirectorioPrograma(actor, programa, "ana", true, 2, 10);
    }

    @Test
    void sin_filtros_aplica_pagina_uno_tamano_veinte_y_programa_nulo_para_todos_los_del_coordinador() {
        when(port.consultarDirectorioPrograma(actor, null, null, null, 1, 20)).thenReturn(new PaginaEstudiantesProgramaProjection(List.of(), 0));

        useCase.execute(new ConsultarEstudiantesProgramaDomain(actor, null, "   ", null, null, null));

        verify(port).consultarDirectorioPrograma(actor, null, null, null, 1, 20);
    }

    @Test
    void acota_pagina_minima_y_tamano_maximo() {
        when(port.consultarDirectorioPrograma(actor, null, null, null, 1, 100)).thenReturn(new PaginaEstudiantesProgramaProjection(List.of(), 0));

        useCase.execute(new ConsultarEstudiantesProgramaDomain(actor, null, null, null, 0, 5000));

        verify(port).consultarDirectorioPrograma(actor, null, null, null, 1, 100);
    }

    @Test
    void exige_consulta_y_puerto() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarEstudiantesProgramaUseCaseImpl(null));
    }
}
