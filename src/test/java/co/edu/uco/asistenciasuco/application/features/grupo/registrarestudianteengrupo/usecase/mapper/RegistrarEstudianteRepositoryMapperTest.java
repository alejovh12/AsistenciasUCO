package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrarEstudianteRepositoryMapperTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void toRepositoryDTO_mapea_datos_funcionales_sin_correlation_id() {
        final RegistrarEstudianteRepositoryDTO dto = RegistrarEstudianteRepositoryMapper.toRepositoryDTO(domainValido());

        assertEquals(TIPO_IDENTIFICACION, dto.getTipoIdentificacionId());
        assertEquals(123456789, dto.getNumeroIdentificacion());
        assertEquals("PEREZ", dto.getPrimerApellido());
        assertEquals("GOMEZ", dto.getSegundoApellido());
        assertEquals("ANA", dto.getPrimerNombre());
        assertEquals("MARIA", dto.getSegundoNombre());
        assertEquals("ana.perez@uco.edu.co", dto.getCorreo());
        assertEquals("Clave123!", dto.getPassword());
        assertEquals(GRUPO, dto.getGrupoId());
    }

    @Test
    void toUseCaseEntity_mapea_mensaje_publico() {
        final RegistrarEstudianteResultadoEntity entity = RegistrarEstudianteRepositoryMapper.toUseCaseEntity(
                new RegistrarEstudianteRepositoryProjection("Estudiante registrado.")
        );

        assertTrue(entity.isExitoso());
        assertEquals("Estudiante registrado.", entity.getMensajeUsuario());
    }

    private RegistrarEstudianteDomain domainValido() {
        return new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                "Gomez",
                "Ana",
                "Maria",
                "ana.perez@uco.edu.co",
                "Clave123!",
                GRUPO
        );
    }
}
