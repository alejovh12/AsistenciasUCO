package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain.DecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.mapper.ConsultarDecanosRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** CIERRE-W1B (punto 4): el listado de decanos trae los nombres separados y agrupados; el front no parte el nombre completo. */
class ConsultarDecanosMapperTest {

    @Test
    void la_proyeccion_llega_al_dto_con_nombres_y_apellidos_separados_y_agrupados() {
        final UUID id = UUID.randomUUID();
        final DecanoProjection proyeccion = new DecanoProjection(id, UUID.randomUUID(), "1037", "Ana Maria Perez Gomez",
                UUID.randomUUID(), "Ingenieria", true, "Ana", "Maria", "Perez", "Gomez");

        final DecanoDomain dominio = ConsultarDecanosRepositoryMapper.toDomain(proyeccion);
        final DecanoDTO dto = ConsultarDecanosMapper.toDTO(dominio);

        assertEquals("Ana", dto.primerNombre());
        assertEquals("Maria", dto.segundoNombre());
        assertEquals("Perez", dto.primerApellido());
        assertEquals("Gomez", dto.segundoApellido());
        assertEquals("Ana Maria", dto.nombres());
        assertEquals("Perez Gomez", dto.apellidos());
        assertEquals("Ana Maria Perez Gomez", dto.nombreCompleto());
    }

    @Test
    void el_correo_y_el_tipo_de_documento_llegan_al_dto() {
        final DecanoDTO dto = ConsultarDecanosMapper.toDTO(ConsultarDecanosRepositoryMapper.toDomain(
                new DecanoProjection(UUID.randomUUID(), UUID.randomUUID(), "1037", "Ana Perez", null, null, true,
                        "Ana", null, "Perez", null, "ana.perez@uco.edu.co", "CC")));

        assertEquals("ana.perez@uco.edu.co", dto.correo());
        assertEquals("CC", dto.tipoIdentificacion());
    }

    @Test
    void sin_correo_ni_tipo_de_documento_no_se_inventan_valores() {
        final DecanoDTO dto = ConsultarDecanosMapper.toDTO(ConsultarDecanosRepositoryMapper.toDomain(
                new DecanoProjection(UUID.randomUUID(), UUID.randomUUID(), "1", "Luis Gomez", null, null, false)));

        assertNull(dto.correo());
        assertNull(dto.tipoIdentificacion());
    }

    @Test
    void sin_nombres_separados_los_agrupados_son_nulos_y_no_se_deducen_del_nombre_completo() {
        final DecanoDTO dto = ConsultarDecanosMapper.toDTO(ConsultarDecanosRepositoryMapper.toDomain(
                new DecanoProjection(UUID.randomUUID(), UUID.randomUUID(), "1", "Luis Gomez", null, null, false)));

        assertNull(dto.nombres());
        assertNull(dto.apellidos());
        assertEquals("Luis Gomez", dto.nombreCompleto());
    }
}
