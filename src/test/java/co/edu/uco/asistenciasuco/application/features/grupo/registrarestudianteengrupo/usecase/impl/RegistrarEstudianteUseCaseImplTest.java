package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrarEstudianteUseCaseImplTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void execute_usuario_no_existente_hashea_password_una_sola_vez_sin_interpretar_resultado_sql() {
        final AtomicReference<RegistrarEstudianteRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    dtoCapturado.set(dto);
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPortSinUsuarios(),
                passwordEncoderPort
        );

        final RegistrarEstudianteResultadoEntity resultado = useCase.execute(domainValido());

        assertTrue(resultado.isExitoso());
        assertEquals("Estudiante registrado.", resultado.getMensajeUsuario());
        assertEquals(TIPO_IDENTIFICACION, dtoCapturado.get().getTipoIdentificacionId());
        assertEquals(GRUPO, dtoCapturado.get().getGrupoId());
        assertEquals("ana.perez@uco.edu.co", dtoCapturado.get().getCorreo());
        assertEquals(1, passwordEncoderPort.encodeCount.get());
        assertEquals("Clave123!", passwordEncoderPort.rawPassword.get());
        assertEquals("HASH_CONTROLADO", dtoCapturado.get().getPassword());
        assertNotEquals("Clave123!", dtoCapturado.get().getPassword());
    }

    @Test
    void execute_propaga_applicationException_del_puerto_secundario() {
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    throw new ConflictException("Matricula duplicada.");
                }),
                usuarioRepositoryPortSinUsuarios(),
                new FakePasswordEncoderPort()
        );

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.execute(domainValido()));

        assertEquals("Matricula duplicada.", exception.getMessage());
    }

    @Test
    void execute_detecta_conflicto_identidad_usuario_antes_de_ejecutar_sp_y_encoder() {
        final AtomicBoolean spEjecutado = new AtomicBoolean(false);
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final UUID usuarioCorreo = UUID.fromString("aaaaaaaa-e3cd-485c-b275-47e7b731e18c");
        final UUID usuarioDocumento = UUID.fromString("bbbbbbbb-e3cd-485c-b275-47e7b731e18c");
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    spEjecutado.set(true);
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(usuarioCorreo, usuarioDocumento),
                passwordEncoderPort
        );

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.execute(domainValido()));

        assertEquals("ERR_IDENTIDAD_USUARIO_CONFLICTO", exception.getCode());
        assertTrue(!spEjecutado.get());
        assertEquals(0, passwordEncoderPort.encodeCount.get());
    }

    @Test
    void execute_usuario_existente_no_invoca_encoder_ni_reemplaza_password() {
        final AtomicReference<RegistrarEstudianteRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final UUID usuario = UUID.fromString("aaaaaaaa-e3cd-485c-b275-47e7b731e18c");
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    dtoCapturado.set(dto);
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(usuario, usuario),
                passwordEncoderPort
        );

        useCase.execute(domainValido());

        assertEquals(0, passwordEncoderPort.encodeCount.get());
        assertEquals(null, dtoCapturado.get().getPassword());
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

    private GrupoRepositoryPort grupoRepositoryPort(final RegistrarEstudianteExecutor executor) {
        return new GrupoRepositoryPort() {
            @Override
            public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
                return executor.registrarEstudiante(dto);
            }

            @Override
            public List<GrupoRepositoryProjection> consultarGrupos() {
                return List.of();
            }
        };
    }

    private UsuarioRepositoryPort usuarioRepositoryPortSinUsuarios() {
        return usuarioRepositoryPort(null, null);
    }

    private UsuarioRepositoryPort usuarioRepositoryPort(final UUID usuarioPorCorreo, final UUID usuarioPorDocumento) {
        return new UsuarioRepositoryPort() {
            @Override
            public CrearUsuarioRepositoryProjection crearUsuario(final CrearUsuarioRepositoryDTO dto) {
                return new CrearUsuarioRepositoryProjection("ok");
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
                return Optional.ofNullable(usuarioPorCorreo).map(UsuarioIdentidadRepositoryProjection::new);
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
                    final UUID tipoIdentificacionId,
                    final Integer numeroIdentificacion
            ) {
                return Optional.ofNullable(usuarioPorDocumento).map(UsuarioIdentidadRepositoryProjection::new);
            }
        };
    }

    @FunctionalInterface
    private interface RegistrarEstudianteExecutor {

        RegistrarEstudianteRepositoryProjection registrarEstudiante(RegistrarEstudianteRepositoryDTO dto);
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
