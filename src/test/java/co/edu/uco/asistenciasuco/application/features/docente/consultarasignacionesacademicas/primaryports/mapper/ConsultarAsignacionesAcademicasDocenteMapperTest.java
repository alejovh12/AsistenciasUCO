package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.entity.DocenteAsignacionAcademicaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarAsignacionesAcademicasDocenteMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> ConsultarAsignacionesAcademicasDocenteMapper.toDomain(null));
    }

    @Test
    void toDomain_con_docente_valido_crea_dominio() {
        final ConsultarAsignacionesAcademicasDocenteDTO dto =
                new ConsultarAsignacionesAcademicasDocenteDTO(UUID.randomUUID());

        final ConsultarAsignacionesAcademicasDocenteDomain dominio = ConsultarAsignacionesAcademicasDocenteMapper.toDomain(dto);

        assertEquals(dto.getDocente(), dominio.getDocente());
    }

    @Test
    void toDomain_con_docente_nulo_en_dto_lanza_validationException() {
        final ConsultarAsignacionesAcademicasDocenteDTO dto = new ConsultarAsignacionesAcademicasDocenteDTO(null);

        assertThrows(ValidationException.class, () -> ConsultarAsignacionesAcademicasDocenteMapper.toDomain(dto));
    }

    @Test
    void toDTOs_con_lista_nula_retorna_lista_vacia() {
        assertTrue(ConsultarAsignacionesAcademicasDocenteMapper.toDTOs(null).isEmpty());
    }

    @Test
    void toDTOs_mapea_entidad_completa() {
        final UUID id = UUID.randomUUID();
        final DocenteAsignacionAcademicaEntity entity = new DocenteAsignacionAcademicaEntity(
                id, UUID.randomUUID(), 123456789, "Ana Perez", true,
                UUID.randomUUID(), "UCO", UUID.randomUUID(), "Ingenieria",
                UUID.randomUUID(), "Sistemas", UUID.randomUUID(), "INP-01",
                UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1",
                UUID.randomUUID(), "DOC", "Docente", true, "Activo"
        );

        final List<DocenteAsignacionAcademicaDTO> resultado = ConsultarAsignacionesAcademicasDocenteMapper.toDTOs(List.of(entity));

        assertEquals(1, resultado.size());
        assertEquals(id, resultado.getFirst().getId());
        assertEquals("Ana Perez", resultado.getFirst().getNombreCompleto());
        assertTrue(resultado.getFirst().isEstaActivoDocente());
    }
}
