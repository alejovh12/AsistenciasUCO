package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.ConsultarAsistenciaProgramaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Revision final BD-9: usp_consultar_grupos_paginado agrega al final nombreDocente (titular del grupo) para que el coordinador de otro
 * programa de un grupo compartido vea el nombre en lugar de "Sin asignar". Se propaga sin transformar.
 */
class GruposProgramaNombreDocenteTest {

    private static final UUID USUARIO = UUID.randomUUID();

    private final ConsultarAsistenciaProgramaUseCase useCase = mock(ConsultarAsistenciaProgramaUseCase.class);
    private final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);

    @Test
    void el_nombre_del_docente_titular_llega_al_dto_del_grupo() {
        final var conDocente = new GrupoProgramaProjection(UUID.randomUUID(), 1, "G1", UUID.randomUUID(), "Calculo", UUID.randomUUID(), 30,
                20, 10, true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5), List.of(), UUID.randomUUID(), "Sistemas", false,
                "Ana Perez");
        when(useCase.consultarGrupos(USUARIO, null, null, null, null)).thenReturn(new Pagina<>(List.of(conDocente), 1));

        final var grupo = interactor.consultarGrupos(USUARIO, null, null, null, null).elementos().getFirst();

        assertEquals("Ana Perez", grupo.nombreDocente());
    }

    @Test
    void la_forma_previa_deja_el_nombre_del_docente_nulo() {
        final var previo = new GrupoProgramaProjection(UUID.randomUUID(), 1, "G1", UUID.randomUUID(), "Calculo", UUID.randomUUID(), 30, 20,
                10, true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5));

        assertNull(previo.nombreDocente());
    }
}
