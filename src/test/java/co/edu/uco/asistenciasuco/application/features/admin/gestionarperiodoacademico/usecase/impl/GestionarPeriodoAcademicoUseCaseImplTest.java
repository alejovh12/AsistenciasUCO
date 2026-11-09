package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.interactor.GestionarPeriodoAcademicoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.domain.PeriodoAcademicoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoCommandPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** HU096/HU097/HU098: validacion de entrada y delegacion al provider DB de periodos academicos. */
class GestionarPeriodoAcademicoUseCaseImplTest {

    private static final LocalDate INICIO = LocalDate.of(2027, 2, 1);
    private static final LocalDate FIN = LocalDate.of(2027, 6, 30);

    private final PeriodoAcademicoCommandPort commandPort = mock(PeriodoAcademicoCommandPort.class);
    private final co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort queryPort =
            mock(co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort.class);
    private final GestionarPeriodoAcademicoUseCaseImpl useCase = new GestionarPeriodoAcademicoUseCaseImpl(commandPort, queryPort);

    /** CULM-001 D.9-a (F-ADM-030): crear devuelve el id generado y consultar aplica los limites de pagina. */
    @Test
    void culm001_d9_crear_devuelve_el_id_que_envia_a_la_base_de_datos() {
        final UUID usuario = UUID.randomUUID();

        final UUID creado = useCase.crear(new PeriodoAcademicoDomain(null, "2027-1", INICIO, FIN, usuario));

        final ArgumentCaptor<UUID> id = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).crearPeriodoAcademico(id.capture(), eq("2027-1"), eq(INICIO), eq(FIN), eq(usuario));
        org.junit.jupiter.api.Assertions.assertEquals(id.getValue(), creado);
    }

    @Test
    void culm001_d9_consultar_usa_pagina_1_y_tamano_10_por_defecto_y_exige_usuario() {
        final UUID usuario = UUID.randomUUID();
        final var pagina = new co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPeriodosAdminProjection(java.util.List.of(), 0);
        org.mockito.Mockito.when(queryPort.consultarPeriodosAdministrador(usuario, 1, 10)).thenReturn(pagina);

        org.junit.jupiter.api.Assertions.assertSame(pagina, useCase.consultar(usuario, null, null));
        org.junit.jupiter.api.Assertions.assertThrows(co.edu.uco.asistenciasuco.application.exception.validation.ValidationException.class,
                () -> useCase.consultar(null, 1, 10));
        org.junit.jupiter.api.Assertions.assertThrows(co.edu.uco.asistenciasuco.application.exception.validation.ValidationException.class,
                () -> useCase.consultar(usuario, 0, 10));
        org.junit.jupiter.api.Assertions.assertThrows(co.edu.uco.asistenciasuco.application.exception.validation.ValidationException.class,
                () -> useCase.consultar(usuario, 1, 101));
    }

    @Test
    void crear_genera_id_recorta_el_nombre_y_propaga_el_ejecutor() {
        final UUID usuario = UUID.randomUUID();

        useCase.crear(new PeriodoAcademicoDomain(null, "  2027-1  ", INICIO, FIN, usuario));

        final ArgumentCaptor<UUID> id = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).crearPeriodoAcademico(id.capture(), eq("2027-1"), eq(INICIO), eq(FIN), eq(usuario));
        assertNotNull(id.getValue());
    }

    @Test
    void actualizar_conserva_el_id_del_periodo() {
        final UUID usuario = UUID.randomUUID();
        final UUID periodo = UUID.randomUUID();

        useCase.actualizar(new PeriodoAcademicoDomain(periodo, "2027-1", INICIO, FIN, usuario));

        verify(commandPort).actualizarPeriodoAcademico(periodo, "2027-1", INICIO, FIN, usuario);
    }

    @Test
    void actualizar_sin_id_se_rechaza() {
        assertThrows(ValidationException.class,
                () -> useCase.actualizar(new PeriodoAcademicoDomain(null, "2027-1", INICIO, FIN, UUID.randomUUID())));
        verifyNoInteractions(commandPort);
    }

    @Test
    void entradas_invalidas_se_rechazan_sin_llamar_a_la_db() {
        final UUID usuario = UUID.randomUUID();
        assertThrows(CrosscuttingException.class, () -> useCase.crear(null));
        assertThrows(ValidationException.class, () -> useCase.crear(new PeriodoAcademicoDomain(null, " ", INICIO, FIN, usuario)));
        assertThrows(ValidationException.class, () -> useCase.crear(new PeriodoAcademicoDomain(null, null, INICIO, FIN, usuario)));
        assertThrows(ValidationException.class, () -> useCase.crear(new PeriodoAcademicoDomain(null, "2027-1", null, FIN, usuario)));
        assertThrows(ValidationException.class, () -> useCase.crear(new PeriodoAcademicoDomain(null, "2027-1", INICIO, null, usuario)));
        assertThrows(ValidationException.class, () -> useCase.crear(new PeriodoAcademicoDomain(null, "2027-1", FIN, INICIO, usuario)));
        assertThrows(ValidationException.class, () -> useCase.crear(new PeriodoAcademicoDomain(null, "2027-1", INICIO, INICIO, usuario)));
        verifyNoInteractions(commandPort);
    }

    @Test
    void eliminar_delega_y_exige_el_periodo() {
        final UUID usuario = UUID.randomUUID();
        final UUID periodo = UUID.randomUUID();

        useCase.eliminar(periodo, usuario);

        verify(commandPort).eliminarPeriodoAcademico(periodo, usuario);
        assertThrows(ValidationException.class, () -> useCase.eliminar(null, usuario));
    }

    @Test
    void interactor_mapea_el_dto_y_delega_en_el_caso_de_uso() {
        final GestionarPeriodoAcademicoInteractor interactor = new GestionarPeriodoAcademicoInteractor(useCase);
        final UUID usuario = UUID.randomUUID();
        final UUID periodo = UUID.randomUUID();

        interactor.crear(new GuardarPeriodoAcademicoDTO(null, "2027-1", INICIO, FIN, usuario));
        interactor.actualizar(new GuardarPeriodoAcademicoDTO(periodo, "2027-2", INICIO, FIN, usuario));
        interactor.eliminar(periodo, usuario);

        verify(commandPort).crearPeriodoAcademico(any(UUID.class), eq("2027-1"), eq(INICIO), eq(FIN), eq(usuario));
        verify(commandPort).actualizarPeriodoAcademico(periodo, "2027-2", INICIO, FIN, usuario);
        verify(commandPort).eliminarPeriodoAcademico(periodo, usuario);
        assertThrows(CrosscuttingException.class, () -> interactor.crear(null));
    }
}
