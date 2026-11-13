package co.edu.uco.asistenciasuco.application.features.asistencia;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.mapper.ConsultarDetalleReclamoDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.DetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.mapper.ConsultarDetalleReclamoDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamosEstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.mapper.ConsultarReclamosEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ReclamosEstudiantePaginaDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.mapper.ConsultarReclamosEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.mapper.ConsultarSesionesMateriaEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.TomaSesionEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper.DetalleReclamoDocenteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.DetalleReclamoDocenteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.SesionMateriaEstudianteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.mapper.ReclamoEstudianteHttpMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.response.ReclamoEstudianteResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PC (puntos de control): los datos de toma, minutos y resultado de la aprobacion viajan de la proyeccion a la respuesta HTTP. */
class PuntosControlMappingTest {

    private static final UUID TOMA = UUID.randomUUID();
    private static final Instant PUNTO = Instant.parse("2026-11-10T13:40:00Z");

    private static SolicitudRevisionEstudianteProjection reclamoEstudiante(final UUID toma, final String resultado) {
        return new SolicitudRevisionEstudianteProjection(UUID.randomUUID(), "REV-1", LocalDate.of(2026, 11, 10), "A", "Aprobada",
                "Cita", "Ok", UUID.randomUUID(), 4, null, null, UUID.randomUUID(), 1, "G1", UUID.randomUUID(), "MAT", "Calculo",
                null, null, null, null, null, 1, null, null, List.of(), toma, toma == null ? null : 2, toma == null ? null : PUNTO, resultado);
    }

    @Test
    void el_reclamo_del_estudiante_lleva_la_toma_y_traduce_el_resultado_a_presente_o_justificada() {
        final var domain = ConsultarReclamosEstudianteRepositoryMapper.toDomain(reclamoEstudiante(TOMA, "AN"));
        final ReclamosEstudiantePaginaDTO pagina = ConsultarReclamosEstudianteMapper.toDTO(
                new ReclamosEstudiantePaginaDomain(List.of(domain), 1));
        final ReclamoEstudianteDTO dto = pagina.reclamos().getFirst();

        assertEquals(TOMA, dto.idToma());
        assertEquals(2, dto.numeroToma());
        assertEquals(PUNTO, dto.fechaPuntoControlUtc());
        assertEquals("PRESENTE", dto.resultadoAprobacion());

        final ReclamoEstudianteResponse response = ReclamoEstudianteHttpMapper.toResponse(dto);
        assertEquals(TOMA, response.idToma());
        assertEquals(2, response.numeroToma());
        assertTrue(response.fechaPuntoControlUtc().startsWith("2026-11-10T13:40:00"));
        assertEquals("PRESENTE", response.resultadoAprobacion());

        final var sinToma = ConsultarReclamosEstudianteMapper.toDTO(new ReclamosEstudiantePaginaDomain(
                List.of(ConsultarReclamosEstudianteRepositoryMapper.toDomain(reclamoEstudiante(null, "EX"))), 1)).reclamos().getFirst();
        assertNull(sinToma.idToma());
        assertNull(sinToma.fechaPuntoControlUtc());
        assertEquals("JUSTIFICADA", sinToma.resultadoAprobacion());
        assertNull(ReclamoEstudianteHttpMapper.toResponse(sinToma).fechaPuntoControlUtc());
    }

    private static DetalleSolicitudRevisionDocenteProjection detalle(final UUID toma, final String marca) {
        return new DetalleSolicitudRevisionDocenteProjection(UUID.randomUUID(), "REV-1", LocalDate.of(2026, 11, 10), "P", "Pendiente",
                "Cita", "", UUID.randomUUID(), "Ana", UUID.randomUUID(), 4, null, null, UUID.randomUUID(), 1, "G1", UUID.randomUUID(),
                "MAT", "Calculo", Boolean.FALSE, "SJC", "Sin justa causa", null, null, null, null, null, null, null, List.of(),
                toma, toma == null ? null : 2, toma == null ? null : PUNTO, null, marca, toma == null ? null : 120, toma == null ? null : 40);
    }

    @Test
    void el_detalle_del_docente_lleva_la_toma_su_estado_actual_y_los_minutos() {
        for (final String[] caso : new String[][]{{"AN", "PRESENTE"}, {"SJC", "AUSENTE"}, {"EX", "JUSTIFICADA"}, {null, "SIN_REGISTRO"}}) {
            final DetalleReclamoDocenteDTO dto = ConsultarDetalleReclamoDocenteMapper.toDTO(
                    ConsultarDetalleReclamoDocenteRepositoryMapper.toDomain(detalle(TOMA, caso[0])));
            assertEquals(caso[1], dto.estadoToma());
        }
        final DetalleReclamoDocenteDomain domain = ConsultarDetalleReclamoDocenteRepositoryMapper.toDomain(detalle(TOMA, "SJC"));
        final DetalleReclamoDocenteDTO dto = ConsultarDetalleReclamoDocenteMapper.toDTO(domain);
        assertEquals(TOMA, dto.idToma());
        assertEquals(120, dto.minutosSesion());
        assertEquals(40, dto.minutosAsistidos());
        assertNull(dto.resultadoAprobacion());

        final DetalleReclamoDocenteResponse response = DetalleReclamoDocenteHttpMapper.toResponse(dto);
        assertEquals(TOMA, response.idToma());
        assertEquals(2, response.numeroToma());
        assertTrue(response.fechaPuntoControlUtc().startsWith("2026-11-10T13:40:00"));
        assertEquals("AUSENTE", response.estadoToma());
        assertEquals(120, response.minutosSesion());
        assertEquals(40, response.minutosAsistidos());

        final DetalleReclamoDocenteDTO legado = ConsultarDetalleReclamoDocenteMapper.toDTO(
                ConsultarDetalleReclamoDocenteRepositoryMapper.toDomain(detalle(null, "SJC")));
        assertNull(legado.estadoToma());
        assertNull(legado.idToma());
    }

    @Test
    void las_sesiones_del_estudiante_llevan_los_minutos_y_las_tomas_hasta_la_respuesta_http() {
        final UUID solicitud = UUID.randomUUID();
        final SesionMateriaEstudianteDomain domain = new SesionMateriaEstudianteDomain(UUID.randomUUID(), "Sesion 1", 1, "S1", 1,
                UUID.randomUUID(), "1", "Grupo", LocalDateTime.of(2026, 11, 10, 13, 0), LocalDateTime.of(2026, 11, 10, 15, 0), null, null,
                EstadoHorarioSesion.INDETERMINADA, EstadoAsistenciaSesion.PARCIAL, null, true, null, null, false, 120, 40, 80, 0, 0,
                List.of(new TomaSesionEstudianteDomain(TOMA, 2, PUNTO, "AUSENTE", true, solicitud, "PENDIENTE", null)));

        final SesionMateriaEstudianteDTO dto = ConsultarSesionesMateriaEstudianteMapper.toDTO(domain);
        final SesionMateriaEstudianteResponse response = SesionMateriaEstudianteResponse.from(dto);

        assertEquals(120, dto.minutosSesion());
        assertEquals(TOMA, dto.tomas().getFirst().idToma());
        assertEquals("PARCIAL", response.estadoAsistencia());
        assertEquals(40, response.minutosAsistidos());
        assertEquals(80, response.minutosFalta());
        assertEquals(0, response.minutosExcusa());
        assertEquals(0, response.minutosSinRegistro());
        final var toma = response.tomas().getFirst();
        assertEquals(TOMA, toma.idToma());
        assertEquals(2, toma.numero());
        assertEquals("2026-11-10T13:40:00Z", toma.fechaPuntoControlUtc());
        assertEquals("AUSENTE", toma.estado());
        assertTrue(toma.reclamable());
        assertEquals(solicitud, toma.idSolicitudRevision());
        assertEquals("PENDIENTE", toma.estadoSolicitudRevision());
        assertNull(toma.resultadoAprobacion());

        // Constructor de compatibilidad: sin minutos ni tomas.
        final var legado = new SesionMateriaEstudianteDomain(domain.id(), "S", 1, "S1", 1, domain.idGrupo(), "1", "G", null, null, null,
                null, EstadoHorarioSesion.INDETERMINADA, EstadoAsistenciaSesion.NO_DISPONIBLE, null, false);
        assertNull(legado.minutosSesion());
        assertTrue(legado.tomas().isEmpty());
        assertTrue(SesionMateriaEstudianteResponse.from(ConsultarSesionesMateriaEstudianteMapper.toDTO(legado)).tomas().isEmpty());
    }
}
