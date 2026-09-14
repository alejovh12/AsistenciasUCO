package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistroAsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarAsistenciasSesionMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> RegistrarAsistenciasSesionMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_registros() {
        final UUID sesion = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        final RegistrarAsistenciasSesionDTO dto = new RegistrarAsistenciasSesionDTO(
                sesion, List.of(new RegistroAsistenciaSesionDTO(estudiante, "ASISTIO")));

        final RegistrarAsistenciasSesionDomain domain = RegistrarAsistenciasSesionMapper.toDomain(dto);

        assertEquals(sesion, domain.getSesion());
        assertEquals(1, domain.getRegistros().size());
        assertEquals(estudiante, domain.getRegistros().getFirst().getEstudiante());
    }

    @Test
    void toDomain_con_registros_nulos_lanza_validationException_por_lista_vacia() {
        final RegistrarAsistenciasSesionDTO dto = new RegistrarAsistenciasSesionDTO(UUID.randomUUID(), null);

        assertThrows(ValidationException.class, () -> RegistrarAsistenciasSesionMapper.toDomain(dto));
    }
}
