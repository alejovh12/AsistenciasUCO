package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.interactor.ConsultarFacultadDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.domain.SesionFacultadHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.SesionFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PAG-DEC-SESIONES (RED): la procedencia temporal del cronograma de la facultad se pide una sola vez, acotada a los
 * ids de las sesiones de la pagina (sin nulos, sin repetidos y en el orden de la pagina), en lugar de una consulta por
 * grupo que lee todas las sesiones de cada grupo. La procedencia solo se aplica a la sesion del mismo id y del mismo
 * grupo. El contrato publico (estadoTemporal, instantes UTC, cancelacion, total y orden) no cambia.
 */
class ConsultarFacultadDecanoSesionesAcotadasTest {

    private static final UUID DECANO = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final int TAMANO_MAXIMO = 100;

    private final FacultadDecanoQueryPort queryPort = mock(FacultadDecanoQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionProcedenciaQueryPort procedenciaPort = mock(SesionProcedenciaQueryPort.class);
    private final ConsultarFacultadDecanoUseCaseImpl useCase =
            new ConsultarFacultadDecanoUseCaseImpl(queryPort, scopePort, procedenciaPort);

    private void paginaConSesiones(final List<SesionFacultadProjection> sesiones, final int total) {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(UUID.randomUUID()));
        when(queryPort.consultarSesiones(DECANO, null, null, null, null, 1, TAMANO_MAXIMO))
                .thenReturn(new Pagina<>(sesiones, total));
    }

    private List<SesionFacultadHorarioDomain> consultar() {
        return useCase.consultarSesiones(DECANO, null, null, null, null, 1, TAMANO_MAXIMO).elementos();
    }

    private static SesionFacultadProjection sesion(final UUID id, final UUID grupo) {
        return new SesionFacultadProjection(id, "Sesion", "S-01", 2, LocalDateTime.of(2026, 2, 3, 7, 0),
                LocalDateTime.of(2026, 2, 3, 9, 0), grupo, 1, "Grupo 1", UUID.randomUUID(), "Calculo", PROGRAMA, "Sistemas");
    }

    private static SesionFacultadProjection sesion(final UUID grupo) {
        return sesion(UUID.randomUUID(), grupo);
    }

    /** Procedencia UTC_V2 (CONFIRMADA) cuyo inicio UTC identifica a la sesion: inicio = 12:00 + {@code minutos}. */
    private static SesionProcedenciaRepositoryProjection procedencia(final UUID sesion, final UUID grupo,
                                                                     final int minutos) {
        return new SesionProcedenciaRepositoryProjection(sesion, grupo, "Sesion", 1, "S-01", 2, "1", "Grupo 1",
                LocalDateTime.of(2026, 2, 3, 12, 0).plusMinutes(minutos),
                LocalDateTime.of(2026, 2, 3, 14, 0).plusMinutes(minutos), "UTC_V2");
    }

    private static LocalDateTime inicioUtc(final int minutos) {
        return LocalDateTime.of(2026, 2, 3, 12, 0).plusMinutes(minutos);
    }

    @SuppressWarnings("unchecked")
    private Collection<UUID> idsSolicitados() {
        final ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(procedenciaPort, times(1)).consultarSesionesPorIds(captor.capture());
        return captor.getValue();
    }

    // Detecta: consultar el puerto aunque no haya sesiones que completar.
    @Test
    @DisplayName("una pagina vacia no consulta procedencia por ningun medio")
    void pagina_vacia_no_consulta_procedencia() {
        paginaConSesiones(List.of(), 0);

        assertEquals(List.of(), consultar());
        verifyNoInteractions(procedenciaPort);
    }

    // Detecta: una consulta por grupo (hasta 100 por pagina) o lectura del grupo completo.
    @Test
    @DisplayName("100 sesiones de 100 grupos distintos provocan una unica consulta por ids y ninguna por grupo")
    void cien_sesiones_de_cien_grupos_se_resuelven_con_una_sola_consulta_por_ids() {
        final List<SesionFacultadProjection> pagina = IntStream.range(0, TAMANO_MAXIMO)
                .mapToObj(i -> sesion(UUID.randomUUID())).toList();
        paginaConSesiones(pagina, 500);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(
                IntStream.range(0, TAMANO_MAXIMO).mapToObj(i -> procedencia(pagina.get(i).id(), pagina.get(i).idGrupo(), i)).toList());

        final List<SesionFacultadHorarioDomain> resultado = consultar();

        assertEquals(pagina.stream().map(SesionFacultadProjection::id).toList(), new ArrayList<>(idsSolicitados()));
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
        assertEquals(TAMANO_MAXIMO, resultado.size());
        for (int i = 0; i < TAMANO_MAXIMO; i++) {
            assertEquals(EstadoHorarioSesion.CONFIRMADA, resultado.get(i).estadoTemporal());
            assertEquals(inicioUtc(i), resultado.get(i).fechaHoraInicioUtc());
        }
    }

    // Detecta: enviar la misma sesion varias veces al puerto (consulta inflada).
    @Test
    @DisplayName("ids de sesion repetidos en la pagina se piden una sola vez")
    void ids_repetidos_se_piden_una_sola_vez() {
        final SesionFacultadProjection uno = sesion(UUID.randomUUID());
        final SesionFacultadProjection dos = sesion(UUID.randomUUID());
        paginaConSesiones(List.of(uno, dos, uno), 3);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(uno.id(), uno.idGrupo(), 1), procedencia(dos.id(), dos.idGrupo(), 2)));

        final List<SesionFacultadHorarioDomain> resultado = consultar();

        assertEquals(List.of(uno.id(), dos.id()), new ArrayList<>(idsSolicitados()));
        assertEquals(List.of(uno.id(), dos.id(), uno.id()),
                resultado.stream().map(SesionFacultadHorarioDomain::id).toList());
        assertEquals(inicioUtc(1), resultado.get(2).fechaHoraInicioUtc());
    }

    // Detecta: enviar null al puerto o aplicar una procedencia sin grupo a una sesion sin grupo (Objects.equals(null, null)).
    @Test
    @DisplayName("una sesion sin grupo no envia nulos al puerto y nunca recibe procedencia")
    void sesion_sin_grupo_no_envia_nulos_y_queda_indeterminada() {
        final SesionFacultadProjection sinGrupo = sesion(null);
        final SesionFacultadProjection conGrupo = sesion(UUID.randomUUID());
        paginaConSesiones(List.of(sinGrupo, conGrupo), 2);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(sinGrupo.id(), null, 1), procedencia(conGrupo.id(), conGrupo.idGrupo(), 2)));

        final List<SesionFacultadHorarioDomain> resultado = consultar();

        // stream y no contains(null): una coleccion inmutable lanzaria NPE al preguntar por null.
        assertFalse(idsSolicitados().stream().anyMatch(java.util.Objects::isNull));
        assertEquals(EstadoHorarioSesion.INDETERMINADA, resultado.get(0).estadoTemporal());
        assertNull(resultado.get(0).fechaHoraInicioUtc());
        assertNull(resultado.get(0).fechaHoraFinUtc());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, resultado.get(1).estadoTemporal());
        assertEquals(inicioUtc(2), resultado.get(1).fechaHoraInicioUtc());
    }

    // Detecta: aplicar la procedencia por id sin comprobar que pertenece al grupo de la sesion; o ignorar toda fila.
    @Test
    @DisplayName("procedencia de otro grupo, aunque traiga el mismo id, se ignora; las de la pagina y su grupo si se aplican")
    void procedencia_de_grupo_ajeno_se_ignora() {
        final SesionFacultadProjection ajena = sesion(UUID.randomUUID());
        final SesionFacultadProjection propia = sesion(UUID.randomUUID());
        final SesionFacultadProjection fueraDePagina = sesion(UUID.randomUUID());
        paginaConSesiones(List.of(ajena, propia), 2);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                new SesionProcedenciaRepositoryProjection(ajena.id(), UUID.randomUUID(), "Sesion", 1, "S-01", 2, "1",
                        "Grupo 1", inicioUtc(1), inicioUtc(1).plusHours(2), "UTC_V2",
                        new CancelacionSesionProjection("DOCENTE", "Motivo", null, LocalDateTime.of(2026, 2, 1, 9, 0))),
                procedencia(propia.id(), propia.idGrupo(), 2),
                procedencia(fueraDePagina.id(), fueraDePagina.idGrupo(), 3)));

        final List<SesionFacultadHorarioDomain> resultado = consultar();

        assertEquals(2, resultado.size());
        assertEquals(EstadoHorarioSesion.INDETERMINADA, resultado.get(0).estadoTemporal());
        assertNull(resultado.get(0).fechaHoraInicioUtc());
        assertNull(resultado.get(0).fechaHoraFinUtc());
        assertNull(resultado.get(0).cancelacion());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, resultado.get(1).estadoTemporal());
        assertEquals(inicioUtc(2), resultado.get(1).fechaHoraInicioUtc());
        assertEquals(inicioUtc(2).plusHours(2), resultado.get(1).fechaHoraFinUtc());
    }

    // Detecta: perder la cancelacion (o su estado) al cambiar de estrategia de lectura.
    @Test
    @DisplayName("una sesion cancelada expone estado y cancelacion de su procedencia hasta el DTO; la programada no")
    void sesion_cancelada_expone_estado_y_cancelacion() {
        final SesionFacultadProjection cancelada = sesion(UUID.randomUUID());
        final SesionFacultadProjection programada = sesion(UUID.randomUUID());
        paginaConSesiones(List.of(cancelada, programada), 2);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                new SesionProcedenciaRepositoryProjection(cancelada.id(), cancelada.idGrupo(), "Sesion", 1, "S-01", 2, "1",
                        "Grupo 1", inicioUtc(1), inicioUtc(1).plusHours(2), "UTC_V2",
                        new CancelacionSesionProjection("FESTIVO", "Festivo nacional", "Dia del trabajo",
                                LocalDateTime.of(2026, 2, 1, 9, 0))),
                procedencia(programada.id(), programada.idGrupo(), 2)));

        final List<SesionFacultadHorarioDomain> resultado = consultar();

        assertNotNull(resultado.get(0).cancelacion());
        assertEquals("FESTIVO", resultado.get(0).cancelacion().tipo());
        assertEquals("Festivo nacional", resultado.get(0).cancelacion().motivo());
        assertEquals("Dia del trabajo", resultado.get(0).cancelacion().nombreFestivo());
        assertEquals(Instant.parse("2026-02-01T09:00:00Z"), resultado.get(0).cancelacion().fechaCancelacionUtc());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, resultado.get(0).estadoTemporal());
        assertNull(resultado.get(1).cancelacion());

        final var dtos = new ConsultarFacultadDecanoInteractor(useCase)
                .consultarSesiones(DECANO, null, null, null, null, 1, TAMANO_MAXIMO).elementos();
        assertEquals("CANCELADA", dtos.get(0).estado());
        assertEquals("FESTIVO", dtos.get(0).cancelacion().tipo());
        assertEquals("PROGRAMADA", dtos.get(1).estado());
        assertNull(dtos.get(1).cancelacion());
    }

    // Detecta: reordenar la pagina segun el orden de las filas que devuelve el puerto, o asignar la procedencia a otra sesion.
    @Test
    @DisplayName("el orden de la pagina se conserva aunque el puerto devuelva las procedencias en otro orden")
    void orden_de_la_pagina_no_depende_del_orden_del_puerto() {
        final SesionFacultadProjection a = sesion(UUID.randomUUID());
        final SesionFacultadProjection b = sesion(UUID.randomUUID());
        final SesionFacultadProjection c = sesion(UUID.randomUUID());
        paginaConSesiones(List.of(a, b, c), 3);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(c.id(), c.idGrupo(), 30), procedencia(a.id(), a.idGrupo(), 10), procedencia(b.id(), b.idGrupo(), 20)));

        final List<SesionFacultadHorarioDomain> resultado = consultar();

        assertEquals(List.of(a.id(), b.id(), c.id()), resultado.stream().map(SesionFacultadHorarioDomain::id).toList());
        assertEquals(List.of(inicioUtc(10), inicioUtc(20), inicioUtc(30)),
                resultado.stream().map(SesionFacultadHorarioDomain::fechaHoraInicioUtc).toList());
        assertEquals(List.of(a.id(), b.id(), c.id()), new ArrayList<>(idsSolicitados()));
    }

    // Detecta: devolver como total el tamano de la pagina en vez del total de la consulta.
    @Test
    @DisplayName("el total de la pagina se conserva")
    void total_de_la_pagina_se_conserva() {
        final SesionFacultadProjection a = sesion(UUID.randomUUID());
        final SesionFacultadProjection b = sesion(UUID.randomUUID());
        paginaConSesiones(List.of(a, b), 57);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(procedencia(a.id(), a.idGrupo(), 1)));

        final var pagina = useCase.consultarSesiones(DECANO, null, null, null, null, 1, TAMANO_MAXIMO);

        assertEquals(57, pagina.total());
        assertEquals(2, pagina.elementos().size());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, pagina.elementos().get(0).estadoTemporal());
        assertEquals(EstadoHorarioSesion.INDETERMINADA, pagina.elementos().get(1).estadoTemporal());
    }
}
