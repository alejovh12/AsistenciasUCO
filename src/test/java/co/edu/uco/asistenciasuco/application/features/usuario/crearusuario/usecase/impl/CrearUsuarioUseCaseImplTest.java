package co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.CrearUsuarioRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.UsuarioIdentidadRepositoryEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearUsuarioUseCaseImplTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void execute_hashea_password_una_sola_vez_y_retorna_resultado_exitoso() {
        final AtomicReference<CrearUsuarioRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final UsuarioRepositoryPort repositoryPort = usuarioRepositoryPort(dto -> {
            dtoCapturado.set(dto);
            return new CrearUsuarioRepositoryEntity("Usuario registrado exitosamente.");
        });

        final CrearUsuarioUseCaseImpl useCase = new CrearUsuarioUseCaseImpl(repositoryPort, passwordEncoderPort);
        final CrearUsuarioResultadoEntity resultado = useCase.execute(domainValido());

        assertTrue(resultado.isExitoso());
        assertEquals("Usuario registrado exitosamente.", resultado.getMensajeUsuario());
        assertEquals("ANA", dtoCapturado.get().getPrimerNombre());
        assertEquals("ana.perez@uco.edu.co", dtoCapturado.get().getCorreo());
        assertEquals(1, passwordEncoderPort.encodeCount.get());
        assertEquals("Clave123!", passwordEncoderPort.rawPassword.get());
        assertEquals("HASH_CONTROLADO", dtoCapturado.get().getPassword());
        assertNotEquals("Clave123!", dtoCapturado.get().getPassword());
    }

    @Test
    void execute_propaga_error_funcional_del_repositorio() {
        final UsuarioRepositoryPort repositoryPort = usuarioRepositoryPort(dto -> {
            throw new ConflictException("La direccion de correo electronico ya se encuentra registrada.");
        });

        final CrearUsuarioUseCaseImpl useCase = new CrearUsuarioUseCaseImpl(repositoryPort, new FakePasswordEncoderPort());

        final ConflictException exception = assertThrows(
                ConflictException.class,
                () -> useCase.execute(domainValido())
        );

        assertEquals("CONFLICT", exception.getCode());
        assertEquals("La direccion de correo electronico ya se encuentra registrada.", exception.getMessage());
    }

    @Test
    void execute_rechaza_usuario_nuevo_sin_password_antes_de_repository_y_encoder() {
        final AtomicReference<CrearUsuarioRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final CrearUsuarioUseCaseImpl useCase = new CrearUsuarioUseCaseImpl(
                usuarioRepositoryPort(dto -> {
                    dtoCapturado.set(dto);
                    return new CrearUsuarioRepositoryEntity("ok");
                }),
                passwordEncoderPort
        );

        final ValidationException exception = assertThrows(
                ValidationException.class,
                () -> useCase.execute(domainConPassword(null))
        );

        assertEquals("ERR_PASSWORD_REQUERIDO", exception.getCode());
        assertEquals(0, passwordEncoderPort.encodeCount.get());
        assertEquals(null, dtoCapturado.get());
    }

    private CrearUsuarioDomain domainValido() {
        return domainConPassword("Clave123!");
    }

    private CrearUsuarioDomain domainConPassword(final String password) {
        return CrearUsuarioDomain.crear(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                "Gomez",
                "Ana",
                "Maria",
                "ANA.PEREZ@UCO.EDU.CO",
                password
        );
    }

    private UsuarioRepositoryPort usuarioRepositoryPort(final CrearUsuarioExecutor executor) {
        return new UsuarioRepositoryPort() {
            @Override
            public CrearUsuarioRepositoryEntity crearUsuario(final CrearUsuarioRepositoryDTO dto) {
                return executor.crearUsuario(dto);
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorCorreo(final String correo) {
                return Optional.empty();
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryEntity> consultarUsuarioPorIdentificacion(
                    final UUID tipoIdentificacionId,
                    final Integer numeroIdentificacion
            ) {
                return Optional.empty();
            }
        };
    }

    @FunctionalInterface
    private interface CrearUsuarioExecutor {

        CrearUsuarioRepositoryEntity crearUsuario(CrearUsuarioRepositoryDTO dto);
    }

    private static final class FakePasswordEncoderPort implements PasswordEncoderPort {

        private final AtomicInteger encodeCount = new AtomicInteger();
        private final AtomicReference<String> rawPassword = new AtomicReference<>();

        @Override
        public String encode(final String rawPassword) {
            encodeCount.incrementAndGet();
            this.rawPassword.set(rawPassword);
            return "HASH_CONTROLADO";
        }

        @Override
        public boolean matches(final String rawPassword, final String encodedPassword) {
            return "Clave123!".equals(rawPassword) && "HASH_CONTROLADO".equals(encodedPassword);
        }
    }
}
