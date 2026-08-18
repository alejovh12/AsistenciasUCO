package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrarEstudianteHttpMapperTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void toApplicationDTO_combina_path_y_body_sin_grupoId_en_request_http() {
        final RegistrarEstudianteRequest request = new RegistrarEstudianteRequest();
        request.setTipoIdentificacionId(TIPO_IDENTIFICACION);
        request.setNumeroIdentificacion(123456789);
        request.setPrimerApellido("Perez");
        request.setSegundoApellido("Gomez");
        request.setPrimerNombre("Ana");
        request.setSegundoNombre("Maria");
        request.setCorreo("ana.perez@uco.edu.co");
        request.setPassword("Clave123!");

        final RegistrarEstudianteDTO dto = RegistrarEstudianteHttpMapper.toApplicationDTO(GRUPO, request);

        assertEquals(TIPO_IDENTIFICACION, dto.getTipoIdentificacionId());
        assertEquals(123456789, dto.getNumeroIdentificacion());
        assertEquals("Perez", dto.getPrimerApellido());
        assertEquals("Gomez", dto.getSegundoApellido());
        assertEquals("Ana", dto.getPrimerNombre());
        assertEquals("Maria", dto.getSegundoNombre());
        assertEquals("ana.perez@uco.edu.co", dto.getCorreo());
        assertEquals("Clave123!", dto.getPassword());
        assertEquals(GRUPO, dto.getGrupoId());
    }
}
