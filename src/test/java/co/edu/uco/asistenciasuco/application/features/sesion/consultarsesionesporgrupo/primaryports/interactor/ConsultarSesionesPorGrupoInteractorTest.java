package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.ConsultarSesionesPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain.ConsultarSesionesPorGrupoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarSesionesPorGrupoInteractorTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ConsultarSesionesPorGrupoUseCase useCase = mock(ConsultarSesionesPorGrupoUseCase.class);
    private final ConsultarSesionesPorGrupoInteractor interactor = new ConsultarSesionesPorGrupoInteractor(useCase);

    @Test
    void constructor_rechaza_caso_de_uso_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultarSesionesPorGrupoInteractor(null));
    }

    @Test
    void execute_delega_el_dominio_al_caso_de_uso_y_mapea_la_respuesta() {
        final UUID sesion = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 3, 2, 8, 0);
        when(useCase.execute(any())).thenReturn(List.of(new SesionConsultadaEntity(
                sesion, GRUPO, "Tema", 1, "SES-01", 1, "G1", "Grupo 1", inicio, inicio.plusHours(2))));

        final List<SesionConsultadaDTO> resultado =
                interactor.execute(new ConsultarSesionesPorGrupoDTO(GRUPO, USUARIO));

        final ArgumentCaptor<ConsultarSesionesPorGrupoDomain> domain =
                ArgumentCaptor.forClass(ConsultarSesionesPorGrupoDomain.class);
        verify(useCase).execute(domain.capture());
        assertEquals(GRUPO, domain.getValue().getGrupo());
        assertEquals(USUARIO, domain.getValue().getUsuarioEjecutor());
        assertEquals(1, resultado.size());
        assertEquals(sesion, resultado.getFirst().getSesion());
    }

    @Test
    void execute_devuelve_lista_vacia_cuando_el_caso_de_uso_no_encuentra_sesiones() {
        when(useCase.execute(any())).thenReturn(List.of());

        assertTrue(interactor.execute(new ConsultarSesionesPorGrupoDTO(GRUPO, USUARIO)).isEmpty());
    }

    @Test
    void execute_con_dto_nulo_falla_antes_de_invocar_el_caso_de_uso() {
        assertThrows(CrosscuttingException.class, () -> interactor.execute(null));

        verifyNoInteractions(useCase);
    }
}
