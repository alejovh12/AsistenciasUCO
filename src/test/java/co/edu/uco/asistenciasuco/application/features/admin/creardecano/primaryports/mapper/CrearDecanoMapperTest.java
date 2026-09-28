package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrearDecanoMapperTest {

    private static final UUID TIPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static CrearDecanoDTO dto(final UUID tipo, final UUID ejecutor) {
        return new CrearDecanoDTO(tipo, 123456, "Ana", "Maria", "Perez", "Gomez",
                "ana.perez@uco.edu.co", "Secreta#123", FACULTAD, ejecutor);
    }

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> CrearDecanoMapper.toDomain(null));
    }

    @Test
    void toDomain_sin_tipo_de_identificacion_lanza_validacion_especifica() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> CrearDecanoMapper.toDomain(dto(null, EJECUTOR)));

        assertEquals(TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_REQUERIDA.code(), ex.getCode());
    }

    @Test
    void toDomain_sin_usuario_ejecutor_lanza_validacion_especifica() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> CrearDecanoMapper.toDomain(dto(TIPO, null)));

        assertEquals(UsuarioErrorCode.ERR_USUARIO_REQUERIDO.code(), ex.getCode());
    }

    @Test
    void toDomain_con_dto_valido_conserva_todos_los_campos() {
        final CrearDecanoDomain domain = CrearDecanoMapper.toDomain(dto(TIPO, EJECUTOR));

        assertEquals(TIPO, domain.getTipoIdentificacionId());
        assertEquals(123456, domain.getNumeroIdentificacion());
        assertEquals("Ana", domain.getPrimerNombre());
        assertEquals("Maria", domain.getSegundoNombre());
        assertEquals("Perez", domain.getPrimerApellido());
        assertEquals("Gomez", domain.getSegundoApellido());
        assertEquals("ana.perez@uco.edu.co", domain.getCorreo());
        assertEquals(FACULTAD, domain.getIdFacultad());
        assertEquals(EJECUTOR, domain.getUsuarioEjecutor());
    }
}
