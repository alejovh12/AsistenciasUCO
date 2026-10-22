package co.edu.uco.asistenciasuco.application.features.decano;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.dto.ActualizarProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.primaryports.interactor.ActualizarProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.ActualizarProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.dto.CambiarEstadoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.primaryports.interactor.CambiarEstadoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.CambiarEstadoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ProgramaAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ConsultarProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.interactor.ConsultarProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ConsultarProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramasPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.interactor.CrearProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.CrearProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static co.edu.uco.asistenciasuco.application.features.decano.common.ProgramaTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Los interactores solo mapean DTO -> dominio -> DTO y delegan en el caso de uso (sin logica propia). */
class ProgramaInteractorsTest {

    private static final ProgramaAcademicoDomain DOMINIO = ProgramaAcademicoMapper.toDomain(projection());
    private static final ProgramaAcademicoDTO DTO = ProgramaAcademicoMapper.toDto(DOMINIO);

    @Test
    void el_mapper_comun_conserva_todos_los_campos() {
        assertEquals(PROGRAMA, DTO.id());
        assertEquals("Ingenieria de Sistemas", DTO.nombre());
        assertEquals(TIPO, DTO.idTipoPrograma());
        assertEquals("pregrado", DTO.nombreTipoPrograma());
        assertEquals(MODALIDAD, DTO.idModalidad());
        assertEquals("Presencial", DTO.nombreModalidad());
        assertEquals(JORNADA, DTO.idJornada());
        assertEquals("Diurna", DTO.nombreJornada());
        assertEquals(COORDINADOR, DTO.idCoordinador());
        assertEquals("Ana Perez", DTO.nombreCoordinador());
        assertEquals(FACULTAD, DTO.idFacultad());
        assertEquals("Facultad de Ingenieria", DTO.nombreFacultad());
        assertEquals(true, DTO.estaActivo());
    }

    @Test
    void crear_delega_el_dominio_mapeado_y_devuelve_el_dto() {
        final CrearProgramaUseCase useCase = mock(CrearProgramaUseCase.class);
        when(useCase.execute(any())).thenReturn(DOMINIO);

        final ProgramaAcademicoDTO result = new CrearProgramaInteractor(useCase)
                .execute(new CrearProgramaDTO(ACTOR, "Sistemas", TIPO, COORDINADOR, MODALIDAD, JORNADA));

        final ArgumentCaptor<CrearProgramaDomain> captor = ArgumentCaptor.forClass(CrearProgramaDomain.class);
        verify(useCase).execute(captor.capture());
        assertEquals(new CrearProgramaDomain(ACTOR, "Sistemas", TIPO, COORDINADOR, MODALIDAD, JORNADA), captor.getValue());
        assertEquals(DTO, result);
    }

    @Test
    void actualizar_delega_el_dominio_mapeado_y_devuelve_el_dto() {
        final ActualizarProgramaUseCase useCase = mock(ActualizarProgramaUseCase.class);
        when(useCase.execute(any())).thenReturn(DOMINIO);

        final ProgramaAcademicoDTO result = new ActualizarProgramaInteractor(useCase)
                .execute(new ActualizarProgramaDTO(ACTOR, PROGRAMA, "Sistemas", TIPO, COORDINADOR, null, null));

        verify(useCase).execute(new co.edu.uco.asistenciasuco.application.features.decano.actualizarprograma.usecase.domain
                .ActualizarProgramaDomain(ACTOR, PROGRAMA, "Sistemas", TIPO, COORDINADOR, null, null));
        assertEquals(DTO, result);
    }

    @Test
    void cambiar_estado_delega_el_dominio_mapeado_y_devuelve_el_dto() {
        final CambiarEstadoProgramaUseCase useCase = mock(CambiarEstadoProgramaUseCase.class);
        when(useCase.execute(any())).thenReturn(DOMINIO);

        final ProgramaAcademicoDTO result = new CambiarEstadoProgramaInteractor(useCase)
                .execute(new CambiarEstadoProgramaDTO(ACTOR, PROGRAMA, false));

        verify(useCase).execute(new co.edu.uco.asistenciasuco.application.features.decano.cambiarestadoprograma.usecase.domain
                .CambiarEstadoProgramaDomain(ACTOR, PROGRAMA, false));
        assertEquals(DTO, result);
    }

    @Test
    void consultar_delega_filtros_y_mapea_la_pagina() {
        final ConsultarProgramasUseCase useCase = mock(ConsultarProgramasUseCase.class);
        when(useCase.execute(any())).thenReturn(new ProgramasPaginaDomain(List.of(DOMINIO), 12));

        final ProgramasPaginaDTO result = new ConsultarProgramasInteractor(useCase)
                .execute(new ConsultarProgramasDTO(ACTOR, "Sis", true, TIPO, 2, 5));

        verify(useCase).execute(new ConsultarProgramasDomain(ACTOR, "Sis", true, TIPO, 2, 5));
        assertEquals(12, result.totalRegistros());
        assertEquals(List.of(DTO), result.programas());
    }

    @Test
    void los_interactores_exigen_su_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new CrearProgramaInteractor(null));
        assertThrows(NullPointerException.class, () -> new ActualizarProgramaInteractor(null));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoProgramaInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarProgramasInteractor(null));
    }
}
