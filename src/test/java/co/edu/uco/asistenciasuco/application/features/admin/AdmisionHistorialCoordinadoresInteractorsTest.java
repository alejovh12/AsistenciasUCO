package co.edu.uco.asistenciasuco.application.features.admin;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmisionEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmitirEstudianteCommandDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.interactor.AdmitirEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.AdmitirEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmisionEstudianteResultadoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmitirEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.mapper.ConsultarHistorialCierresMasivosMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.domain.CierrePeriodoReporteDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.impl.ConsultarHistorialCierresMasivosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.interactor.ConsultarCoordinadoresInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.EstadoCuentaAprovisionada;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialCierrePeriodoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CierrePeriodoReporteProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** F-ADM-006 (historial de cierres), D.4 (admision de estudiantes) y F-DEC-004 (coordinadores paginados): orquestacion y mapeo. */
class AdmisionHistorialCoordinadoresInteractorsTest {

    // ---- admision ----

    @Test
    void admitir_traduce_el_comando_y_expone_el_estado_de_la_cuenta_como_texto() {
        final AdmitirEstudianteUseCase useCase = mock(AdmitirEstudianteUseCase.class);
        final UUID estudiante = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        when(useCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(
                new AdmisionEstudianteResultadoDomain(estudiante, usuario, true, EstadoCuentaAprovisionada.ACTIVA, false));
        final UUID tipo = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID admin = UUID.randomUUID();

        final AdmisionEstudianteDTO dto = new AdmitirEstudianteInteractor(useCase).execute(new AdmitirEstudianteCommandDTO(
                tipo, 1017123456, "Ana", null, "Perez", "Gomez", "ana@uco.edu.co", "Clave123!", programa, admin));

        final ArgumentCaptor<AdmitirEstudianteDomain> domain = ArgumentCaptor.forClass(AdmitirEstudianteDomain.class);
        verify(useCase).execute(domain.capture());
        assertEquals(tipo, domain.getValue().tipoIdentificacionId());
        assertEquals(1017123456, domain.getValue().numeroIdentificacion());
        assertEquals("Ana", domain.getValue().primerNombre());
        assertNull(domain.getValue().segundoNombre());
        assertEquals("Clave123!", domain.getValue().password());
        assertEquals(programa, domain.getValue().idPrograma());
        assertEquals(admin, domain.getValue().usuarioEjecutor());
        assertEquals(estudiante, dto.idEstudiante());
        assertEquals(usuario, dto.idUsuario());
        assertTrue(dto.esUsuarioNuevo());
        assertEquals("ACTIVA", dto.estadoCuenta());
        assertEquals(false, dto.cuentaReparada());
        assertTrue(!domain.getValue().toString().contains("Clave123!"));
    }

    @Test
    void admitir_exige_caso_de_uso_y_comando() {
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteInteractor(null));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteInteractor(mock(AdmitirEstudianteUseCase.class)).execute(null));
    }

    // ---- historial de cierres ----

    private static CierrePeriodoReporteProjection proyeccion(final UUID id) {
        return new CierrePeriodoReporteProjection(id, "2026-2", OffsetDateTime.parse("2026-11-08T10:00:00Z"), 40, 7, 35, 5, "COMPLETADO", "admin-1");
    }

    @Test
    void el_caso_de_uso_del_historial_copia_cada_columna_en_orden() {
        final HistorialCierrePeriodoQueryPort puerto = mock(HistorialCierrePeriodoQueryPort.class);
        final UUID id = UUID.randomUUID();
        when(puerto.consultarHistorialCierres()).thenReturn(List.of(proyeccion(id), proyeccion(UUID.randomUUID())));

        final List<CierrePeriodoReporteDomain> historial = new ConsultarHistorialCierresMasivosUseCaseImpl(puerto).execute();

        assertEquals(2, historial.size());
        assertEquals(new CierrePeriodoReporteDomain(id, "2026-2", OffsetDateTime.parse("2026-11-08T10:00:00Z"), 40, 7, 35, 5, "COMPLETADO", "admin-1"), historial.getFirst());
    }

    @Test
    void el_caso_de_uso_del_historial_sin_cierres_devuelve_lista_vacia_y_exige_su_puerto() {
        final HistorialCierrePeriodoQueryPort puerto = mock(HistorialCierrePeriodoQueryPort.class);
        when(puerto.consultarHistorialCierres()).thenReturn(List.of());
        assertTrue(new ConsultarHistorialCierresMasivosUseCaseImpl(puerto).execute().isEmpty());
        assertThrows(NullPointerException.class, () -> new ConsultarHistorialCierresMasivosUseCaseImpl(null));
    }

    @Test
    void el_mapper_del_historial_copia_el_dominio_al_dto_y_tolera_nulo() {
        final UUID id = UUID.randomUUID();
        final CierrePeriodoReporteDTO dto = ConsultarHistorialCierresMasivosMapper.toDTO(
                new CierrePeriodoReporteDomain(id, "2026-2", OffsetDateTime.parse("2026-11-08T10:00:00Z"), 40, 7, 35, 5, "COMPLETADO", "admin-1"));
        assertEquals(new CierrePeriodoReporteDTO(id, "2026-2", OffsetDateTime.parse("2026-11-08T10:00:00Z"), 40, 7, 35, 5, "COMPLETADO", "admin-1"), dto);
        assertNull(ConsultarHistorialCierresMasivosMapper.toDTO(null));
    }

    // ---- coordinadores paginados ----

    private static List<CoordinadorDomain> coordinadores(final int cantidad) {
        final List<CoordinadorDomain> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            final CoordinadorDomain c = mock(CoordinadorDomain.class);
            when(c.id()).thenReturn(UUID.randomUUID());
            when(c.nombreCompleto()).thenReturn("Coordinador " + i);
            lista.add(c);
        }
        return lista;
    }

    @Test
    void coordinadores_sin_paginar_devuelve_todos_mapeados() {
        final ConsultarCoordinadoresUseCase useCase = mock(ConsultarCoordinadoresUseCase.class);
        final UUID decano = UUID.randomUUID();
        final List<CoordinadorDomain> tres = coordinadores(3);
        when(useCase.execute(decano)).thenReturn(tres);

        final List<CoordinadorDTO> todos = new ConsultarCoordinadoresInteractor(useCase).execute(decano);

        assertEquals(3, todos.size());
        assertEquals("Coordinador 1", todos.get(1).nombreCompleto());
    }

    @Test
    void coordinadores_pagina_en_servidor_con_valores_por_defecto_y_limites() {
        final ConsultarCoordinadoresUseCase useCase = mock(ConsultarCoordinadoresUseCase.class);
        final UUID decano = UUID.randomUUID();
        final List<CoordinadorDomain> veinticinco = coordinadores(25);
        when(useCase.execute(decano)).thenReturn(veinticinco);
        final ConsultarCoordinadoresInteractor interactor = new ConsultarCoordinadoresInteractor(useCase);

        final PaginaResultado<CoordinadorDTO> porDefecto = interactor.execute(decano, null, null);
        assertEquals(1, porDefecto.numeroPagina());
        assertEquals(10, porDefecto.tamanoPagina());
        assertEquals(25, porDefecto.totalElementos());
        assertEquals(10, porDefecto.elementos().size());

        final PaginaResultado<CoordinadorDTO> ultima = interactor.execute(decano, 3, 10);
        assertEquals(5, ultima.elementos().size());
        assertEquals("Coordinador 20", ultima.elementos().getFirst().nombreCompleto());

        final PaginaResultado<CoordinadorDTO> fueraDeRango = interactor.execute(decano, 9, 10);
        assertTrue(fueraDeRango.elementos().isEmpty());
        assertEquals(25, fueraDeRango.totalElementos());

        assertThrows(ValidationException.class, () -> interactor.execute(decano, 0, 10));
        assertThrows(ValidationException.class, () -> interactor.execute(decano, 1, 101));
        assertThrows(NullPointerException.class, () -> new ConsultarCoordinadoresInteractor(null));
    }
}
