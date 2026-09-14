package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.ActualizarGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.CrearGrupoRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrupoHttpMapperContractTest {

    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID PERIOD = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void createBindsCanonicalFieldsAndPreservesPriorityOverAliases() {
        final CrearGrupoRequest request = json.readValue("""
                {"idAsignatura":"%s","asignaturaId":"%s","idPeriodoAcademico":"%s",
                 "periodoAcademicoId":"%s","codigo":12,"nombre":"  Grupo A  ",
                 "idDocente":"%s","docenteId":"%s","aula":"  A101  ",
                 "generarSesionesAutomaticas":false,"crearSesionesAutomaticamente":true}
                """.formatted(SUBJECT, UUID.randomUUID(), PERIOD, UUID.randomUUID(), TEACHER, UUID.randomUUID()),
                CrearGrupoRequest.class);

        final CrearGrupoDTO mapped = GrupoHttpMapper.toApplicationDTO(request);
        assertEquals(SUBJECT, mapped.idAsignatura());
        assertEquals(PERIOD, mapped.idPeriodoAcademico());
        assertEquals(12, mapped.codigo());
        assertEquals("Grupo A", mapped.nombre());
        assertEquals(TEACHER, mapped.idDocente());
        assertEquals("A101", mapped.aula());
        assertEquals(false, mapped.generarSesionesAutomaticas());
    }

    @Test
    void createAcceptsAliasesAndLegacySectionAndRoomNames() {
        final CrearGrupoRequest request = json.readValue("""
                {"asignaturaId":"%s","periodoAcademicoId":"%s","codigo":13,
                 "section":"Grupo B","docenteId":"%s","room":"B202",
                 "crearSesionesAutomaticamente":true}
                """.formatted(SUBJECT, PERIOD, TEACHER), CrearGrupoRequest.class);

        final CrearGrupoDTO mapped = GrupoHttpMapper.toApplicationDTO(request);
        assertEquals(SUBJECT, mapped.idAsignatura());
        assertEquals(PERIOD, mapped.idPeriodoAcademico());
        assertEquals("Grupo B", mapped.nombre());
        assertEquals("B202", mapped.aula());
        assertEquals(TEACHER, mapped.idDocente());
        assertTrue(mapped.generarSesionesAutomaticas());
    }

    @Test
    void updateBindsOptionalFieldsAndTeacherAlias() {
        final ActualizarGrupoRequest request = json.readValue("""
                {"codigo":14,"section":"  Grupo actualizado  ","docenteId":"%s",
                 "cupoMaximo":25,"room":"  C303  "}
                """.formatted(TEACHER), ActualizarGrupoRequest.class);

        final ActualizarGrupoDTO mapped = GrupoHttpMapper.toApplicationDTO(GROUP, request);
        assertEquals(GROUP, mapped.idGrupo());
        assertEquals(14, mapped.codigo());
        assertEquals("Grupo actualizado", mapped.nombre());
        assertEquals(TEACHER, mapped.idDocente());
        assertEquals(25, mapped.cupoMaximo());
        assertEquals("C303", mapped.aula());

        final ActualizarGrupoRequest empty = json.readValue("{}", ActualizarGrupoRequest.class);
        assertNull(GrupoHttpMapper.toApplicationDTO(GROUP, empty).nombre());
        assertNull(GrupoHttpMapper.toApplicationDTO(GROUP, empty).aula());
    }

    @Test
    void rejectsMissingCreateFieldsAndMissingUpdateIdentity() {
        assertThrows(NullPointerException.class, () -> GrupoHttpMapper.toApplicationDTO((CrearGrupoRequest) null));
        assertThrows(NullPointerException.class, () -> GrupoHttpMapper.toApplicationDTO(GROUP, null));
        assertThrows(NullPointerException.class,
                () -> GrupoHttpMapper.toApplicationDTO(null, new ActualizarGrupoRequest()));

        final CrearGrupoRequest request = json.readValue("{}", CrearGrupoRequest.class);
        assertEquals("ERR_CAMPO_OBLIGATORIO",
                assertThrows(ValidationException.class, () -> GrupoHttpMapper.toApplicationDTO(request)).getCode());
        request.setCodigo(1);
        assertEquals("ERR_CAMPO_OBLIGATORIO",
                assertThrows(ValidationException.class, () -> GrupoHttpMapper.toApplicationDTO(request)).getCode());
        request.setNombre("Grupo");
        assertEquals("ERR_CAMPO_OBLIGATORIO",
                assertThrows(ValidationException.class, () -> GrupoHttpMapper.toApplicationDTO(request)).getCode());
    }

    @Test
    void scheduleFieldsAreParsedButRejectedUntilPublicDbCommandExists() {
        final CrearGrupoRequest create = basicCreate();
        create.setHoraInicio("08:00");
        assertThrows(FeatureUnavailableException.class, () -> GrupoHttpMapper.toApplicationDTO(create));
        create.setHoraInicio("25:00");
        assertEquals("ERR_FECHA_HORA_INVALIDA",
                assertThrows(ValidationException.class, () -> GrupoHttpMapper.toApplicationDTO(create)).getCode());

        final ActualizarGrupoRequest update = new ActualizarGrupoRequest();
        update.setHoraFin("09:00:00");
        assertThrows(FeatureUnavailableException.class, () -> GrupoHttpMapper.toApplicationDTO(GROUP, update));
        update.setHoraFin("24:00");
        assertEquals("ERR_FECHA_HORA_INVALIDA",
                assertThrows(ValidationException.class, () -> GrupoHttpMapper.toApplicationDTO(GROUP, update)).getCode());
        update.setHoraFin(null);
        update.setDias(java.util.List.of("LUNES"));
        assertThrows(FeatureUnavailableException.class, () -> GrupoHttpMapper.toApplicationDTO(GROUP, update));
    }

    private static CrearGrupoRequest basicCreate() {
        final CrearGrupoRequest request = new CrearGrupoRequest();
        request.setCodigo(1);
        request.setNombre("Grupo");
        request.setAula("A101");
        return request;
    }
}
