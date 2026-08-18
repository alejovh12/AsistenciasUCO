package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.dto.CrearUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearUsuarioInteractorTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void execute_mapea_request_y_response_sin_password_ni_mensaje_tecnico() {
        final AtomicReference<CrearUsuarioDomain> domainCapturado = new AtomicReference<>();
        final CrearUsuarioUseCase useCase = domain -> {
            domainCapturado.set(domain);
            return new CrearUsuarioResultadoEntity(
                    true,
                    "Usuario registrado exitosamente."
            );
        };

        final CrearUsuarioInteractor interactor = new CrearUsuarioInteractor(useCase);
        final CrearUsuarioResultadoDTO resultado = interactor.execute(dtoValido());

        assertEquals("ANA", domainCapturado.get().getPrimerNombre());
        assertEquals("ana.perez@uco.edu.co", domainCapturado.get().getCorreo());
        assertTrue(resultado.isExitoso());
        assertEquals("Usuario registrado exitosamente.", resultado.getMensajeUsuario());
        assertFalse(tieneCampo(CrearUsuarioResultadoDTO.class, "password"));
        assertFalse(tieneCampo(CrearUsuarioResultadoDTO.class, "mensajeTecnicoResultado"));
    }

    private CrearUsuarioDTO dtoValido() {
        return new CrearUsuarioDTO(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                "Gomez",
                "Ana",
                "Maria",
                "ANA.PEREZ@UCO.EDU.CO",
                "Clave123!"
        );
    }

    private boolean tieneCampo(final Class<?> type, final String fieldName) {
        for (final Field field : type.getDeclaredFields()) {
            if (fieldName.equals(field.getName())) {
                return true;
            }
        }
        return false;
    }
}
