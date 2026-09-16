package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cubre el flujo DB-first de registrar estudiante en grupo: el command contra
 * {@link GrupoRepositoryPort} debe terminar exitosamente antes de invocar
 * {@link IdentityProviderPort}, el UUID institucional se resuelve DESPUES del command via
 * {@code consultarUsuarioPorIdentificacion}, el rol lo determina el servidor
 * ({@link InstitutionalRole#ESTUDIANTE}), y un fallo de Identity posterior a un DB success nunca
 * intenta deshacer la operacion de base de datos.
 */
class RegistrarEstudianteUseCaseImplTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO_NUEVO_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    // -------------------------------------------------------------------------
    // Caso A / F: DB success + Identity success, usuario nuevo — hash a DB, raw a Identity
    // -------------------------------------------------------------------------

    @Test
    void caso_A_F_usuario_nuevo_db_e_identity_exitosos_hash_a_db_raw_a_identity() {
        final AtomicReference<RegistrarEstudianteRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final AtomicInteger spLlamadas = new AtomicInteger();
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true);
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    spLlamadas.incrementAndGet();
                    dtoCapturado.set(dto);
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(null, Arrays.asList(null, USUARIO_NUEVO_ID)),
                passwordEncoderPort,
                identityProviderPort
        );

        final RegistrarEstudianteResultadoEntity resultado = useCase.execute(domainValido());

        assertTrue(resultado.isExitoso());
        assertEquals("Estudiante registrado.", resultado.getMensajeUsuario());
        assertEquals(1, spLlamadas.get());
        assertEquals(TIPO_IDENTIFICACION, dtoCapturado.get().getTipoIdentificacionId());
        assertEquals(GRUPO, dtoCapturado.get().getGrupoId());
        assertEquals("ana.perez@uco.edu.co", dtoCapturado.get().getCorreo());

        // DB recibe hash
        assertEquals(1, passwordEncoderPort.encodeCount.get());
        assertEquals("Clave123!", passwordEncoderPort.rawPassword.get());
        assertEquals("HASH_CONTROLADO", dtoCapturado.get().getPassword());
        assertNotEquals("Clave123!", dtoCapturado.get().getPassword());

        // Identity llamado exactamente una vez, con el raw password — nunca el hash
        assertEquals(1, identityProviderPort.crearCuentaCount.get());
        final CrearCuentaIdentidadDTO identidadEnviada = identityProviderPort.dtoCapturado.get();
        assertEquals("Clave123!", identidadEnviada.passwordInicial());
        assertNotEquals("HASH_CONTROLADO", identidadEnviada.passwordInicial());
        assertEquals("ana.perez@uco.edu.co", identidadEnviada.correo());
    }

    // -------------------------------------------------------------------------
    // Caso D / E: UUID post-command exacto + rol ESTUDIANTE tipado
    // -------------------------------------------------------------------------

    @Test
    void caso_D_E_dto_identity_recibe_exactamente_el_uuid_resuelto_post_command_y_rol_estudiante_tipado() {
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true);
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> new RegistrarEstudianteRepositoryProjection("Estudiante registrado.")),
                usuarioRepositoryPort(null, Arrays.asList(null, USUARIO_NUEVO_ID)),
                new FakePasswordEncoderPort(),
                identityProviderPort
        );

        useCase.execute(domainValido());

        final CrearCuentaIdentidadDTO dto = identityProviderPort.dtoCapturado.get();
        assertEquals(USUARIO_NUEVO_ID, dto.idUsuario());
        assertEquals(InstitutionalRole.ESTUDIANTE, dto.rolInstitucional());
        assertEquals("123456789", dto.username());
    }

    // -------------------------------------------------------------------------
    // Caso B: DB failure -> Identity nunca se invoca
    // -------------------------------------------------------------------------

    @Test
    void caso_B_fallo_db_impide_llamar_identity() {
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true);
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    throw new ConflictException("Matricula duplicada.");
                }),
                usuarioRepositoryPort(null, Arrays.asList((UUID) null)),
                new FakePasswordEncoderPort(),
                identityProviderPort
        );

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.execute(domainValido()));

        assertEquals("Matricula duplicada.", exception.getMessage());
        assertEquals(0, identityProviderPort.crearCuentaCount.get());
    }

    // -------------------------------------------------------------------------
    // Caso C: conflicto de identidad pre-command -> SP no llamado, Identity no llamado
    // -------------------------------------------------------------------------

    @Test
    void caso_C_conflicto_identidad_pre_command_no_llama_sp_ni_encoder_ni_identity() {
        final AtomicBoolean spEjecutado = new AtomicBoolean(false);
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true);
        final UUID usuarioCorreo = UUID.fromString("aaaaaaaa-e3cd-485c-b275-47e7b731e18c");
        final UUID usuarioDocumento = UUID.fromString("bbbbbbbb-e3cd-485c-b275-47e7b731e18c");
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    spEjecutado.set(true);
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(usuarioCorreo, List.of(usuarioDocumento)),
                passwordEncoderPort,
                identityProviderPort
        );

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.execute(domainValido()));

        assertEquals("ERR_IDENTIDAD_USUARIO_CONFLICTO", exception.getCode());
        assertFalse(spEjecutado.get());
        assertEquals(0, passwordEncoderPort.encodeCount.get());
        assertEquals(0, identityProviderPort.crearCuentaCount.get());
    }

    // -------------------------------------------------------------------------
    // Caso G: usuario DB existente — encoder no invocado, password DB null,
    // Identity recibe el raw password disponible (no null-forzado, no hash)
    // -------------------------------------------------------------------------

    @Test
    void caso_G_usuario_existente_no_invoca_encoder_password_db_null_identity_recibe_raw_disponible() {
        final AtomicReference<RegistrarEstudianteRepositoryDTO> dtoCapturado = new AtomicReference<>();
        final FakePasswordEncoderPort passwordEncoderPort = new FakePasswordEncoderPort();
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, false);
        final UUID usuarioExistente = UUID.fromString("aaaaaaaa-e3cd-485c-b275-47e7b731e18c");
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    dtoCapturado.set(dto);
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(usuarioExistente, List.of(usuarioExistente, usuarioExistente)),
                passwordEncoderPort,
                identityProviderPort
        );

        useCase.execute(domainValido());

        assertEquals(0, passwordEncoderPort.encodeCount.get());
        assertNull(dtoCapturado.get().getPassword());
        // Identity no depende del password persistido en DB; recibe el raw enviado en el dominio.
        assertEquals("Clave123!", identityProviderPort.dtoCapturado.get().passwordInicial());
        assertEquals(1, identityProviderPort.crearCuentaCount.get());
    }

    @Test
    void correo_identity_proviene_de_proyeccion_post_command_aunque_request_sea_distinto() {
        final FakeIdentityProviderPort identity = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, false);
        final UUID existente = UUID.fromString("aaaaaaaa-e3cd-485c-b275-47e7b731e18c");
        final RegistrarEstudianteDomain request = domainConCorreo("nuevo@uco.edu.co");
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> new RegistrarEstudianteRepositoryProjection("Estudiante registrado.")),
                usuarioRepositoryPort(null, List.of(existente, existente), "viejo@uco.edu.co"),
                new FakePasswordEncoderPort(), identity
        );

        useCase.execute(request);

        assertEquals("nuevo@uco.edu.co", request.getCorreo());
        assertEquals(existente, identity.dtoCapturado.get().idUsuario());
        assertEquals("viejo@uco.edu.co", identity.dtoCapturado.get().correo());
        assertNotEquals(request.getCorreo(), identity.dtoCapturado.get().correo());
    }

    @Test
    void correo_identity_usuario_nuevo_tambien_proviene_de_proyeccion_post_command() {
        final FakeIdentityProviderPort identity = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true);
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> new RegistrarEstudianteRepositoryProjection("Estudiante registrado.")),
                usuarioRepositoryPort(null, Arrays.asList(null, USUARIO_NUEVO_ID), "ana.perez@uco.edu.co"),
                new FakePasswordEncoderPort(), identity
        );

        useCase.execute(domainValido());

        assertEquals("ana.perez@uco.edu.co", identity.dtoCapturado.get().correo());
    }

    // -------------------------------------------------------------------------
    // Caso H: Identity falla despues de DB success -> error controlado, sin rollback ni eliminarCuenta
    // -------------------------------------------------------------------------

    @Test
    void caso_H_fallo_identity_posterior_a_db_success_no_intenta_rollback_ni_eliminar_cuenta() {
        final AtomicInteger spLlamadas = new AtomicInteger();
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.fallido();
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    spLlamadas.incrementAndGet();
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(null, Arrays.asList(null, USUARIO_NUEVO_ID)),
                new FakePasswordEncoderPort(),
                identityProviderPort
        );

        final InternalApplicationException exception = assertThrows(
                InternalApplicationException.class, () -> useCase.execute(domainValido())
        );

        assertEquals(1, spLlamadas.get());
        assertEquals(1, identityProviderPort.crearCuentaCount.get());
        assertEquals(0, identityProviderPort.eliminarCuentaCount.get());
        assertNotEquals(null, exception.getCause());
    }

    // -------------------------------------------------------------------------
    // Caso I: DB success pero UUID no resuelto post-command -> InternalApplicationException, Identity no llamado
    // -------------------------------------------------------------------------

    @Test
    void caso_I_uuid_no_encontrado_post_command_no_llama_identity() {
        final AtomicInteger spLlamadas = new AtomicInteger();
        final FakeIdentityProviderPort identityProviderPort = FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true);
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> {
                    spLlamadas.incrementAndGet();
                    return new RegistrarEstudianteRepositoryProjection("Estudiante registrado.");
                }),
                usuarioRepositoryPort(null, Arrays.asList(null, null)),
                new FakePasswordEncoderPort(),
                identityProviderPort
        );

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domainValido()));

        assertEquals(1, spLlamadas.get());
        assertEquals(0, identityProviderPort.crearCuentaCount.get());
    }

    // -------------------------------------------------------------------------
    // Caso J: el mensaje funcional no contiene semantica del proveedor de identidad
    // -------------------------------------------------------------------------

    @Test
    void caso_J_mensaje_funcional_no_menciona_al_proveedor_de_identidad() {
        final RegistrarEstudianteUseCaseImpl useCase = new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort(dto -> new RegistrarEstudianteRepositoryProjection("Estudiante registrado.")),
                usuarioRepositoryPort(null, Arrays.asList(null, USUARIO_NUEVO_ID)),
                new FakePasswordEncoderPort(),
                FakeIdentityProviderPort.exitoso(USUARIO_NUEVO_ID, true)
        );

        final RegistrarEstudianteResultadoEntity resultado = useCase.execute(domainValido());

        final String mensaje = resultado.getMensajeUsuario().toLowerCase(Locale.ROOT);
        assertFalse(mensaje.contains("idp"));
        assertFalse(mensaje.contains("keycloak"));
        assertFalse(mensaje.contains("identity provider"));
    }

    private RegistrarEstudianteDomain domainValido() {
        return domainConCorreo("ana.perez@uco.edu.co");
    }

    private RegistrarEstudianteDomain domainConCorreo(final String correo) {
        return new RegistrarEstudianteDomain(
                TIPO_IDENTIFICACION,
                123456789,
                "Perez",
                "Gomez",
                "Ana",
                "Maria",
                correo,
                "Clave123!",
                GRUPO
        );
    }

    private GrupoRepositoryPort grupoRepositoryPort(final RegistrarEstudianteExecutor executor) {
        return new GrupoRepositoryPort() {
            @Override
            public GrupoCommandRepositoryProjection crearGrupo(final CrearGrupoRepositoryDTO dto) {
                throw new UnsupportedOperationException("No usado por este test.");
            }

            @Override
            public GrupoCommandRepositoryProjection actualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
                throw new UnsupportedOperationException("No usado por este test.");
            }

            @Override
            public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
                return executor.registrarEstudiante(dto);
            }

            @Override
            public List<GrupoRepositoryProjection> consultarGrupos() {
                return List.of();
            }

            @Override
            public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId) {
                throw new UnsupportedOperationException("No usado por este test.");
            }
        };
    }

    /**
     * @param usuarioPorCorreo             resultado fijo de {@code consultarUsuarioPorCorreo} (pre-command).
     * @param secuenciaPorIdentificacion    resultados sucesivos de {@code consultarUsuarioPorIdentificacion},
     *                                      consumidos en orden: la 1a invocacion es la verificacion de
     *                                      conflicto PRE-command, la 2a (cuando el flujo llega ahi) es la
     *                                      resolucion del UUID institucional POST-command.
     */
    private UsuarioRepositoryPort usuarioRepositoryPort(
            final UUID usuarioPorCorreo,
            final List<UUID> secuenciaPorIdentificacion
    ) {
        return usuarioRepositoryPort(usuarioPorCorreo, secuenciaPorIdentificacion, "ana.perez@uco.edu.co");
    }

    private UsuarioRepositoryPort usuarioRepositoryPort(
            final UUID usuarioPorCorreo,
            final List<UUID> secuenciaPorIdentificacion,
            final String correoCanonico
    ) {
        final Iterator<UUID> secuencia = secuenciaPorIdentificacion.iterator();
        return new UsuarioRepositoryPort() {
            @Override
            public CrearUsuarioRepositoryProjection crearUsuario(final CrearUsuarioRepositoryDTO dto) {
                throw new UnsupportedOperationException("No usado por este test.");
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorCorreo(final String correo) {
                return Optional.ofNullable(usuarioPorCorreo)
                        .map(id -> new UsuarioIdentidadRepositoryProjection(
                                id, TIPO_IDENTIFICACION, 123456789, "ANA", "PEREZ", correoCanonico));
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorId(final UUID idUsuario) {
                return Optional.empty();
            }

            @Override
            public Optional<UsuarioIdentidadRepositoryProjection> consultarUsuarioPorIdentificacion(
                    final UUID tipoIdentificacionId,
                    final Integer numeroIdentificacion
            ) {
                if (!secuencia.hasNext()) {
                    throw new IllegalStateException(
                            "El test no configuro suficientes resultados para consultarUsuarioPorIdentificacion."
                    );
                }
                return Optional.ofNullable(secuencia.next())
                        .map(id -> new UsuarioIdentidadRepositoryProjection(
                                id, TIPO_IDENTIFICACION, 123456789, "ANA", "PEREZ", correoCanonico));
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

    private static final class FakeIdentityProviderPort implements IdentityProviderPort {

        private final AtomicInteger crearCuentaCount = new AtomicInteger();
        private final AtomicInteger eliminarCuentaCount = new AtomicInteger();
        private final AtomicReference<CrearCuentaIdentidadDTO> dtoCapturado = new AtomicReference<>();
        private final Supplier<CuentaIdentidadDTO> resultado;

        private FakeIdentityProviderPort(final Supplier<CuentaIdentidadDTO> resultado) {
            this.resultado = resultado;
        }

        static FakeIdentityProviderPort exitoso(final UUID idExterno, final boolean newlyCreated) {
            return new FakeIdentityProviderPort(() -> new CuentaIdentidadDTO(idExterno.toString(), newlyCreated));
        }

        static FakeIdentityProviderPort fallido() {
            return new FakeIdentityProviderPort(() -> {
                throw new IdentityProviderException("Fallo simulado del proveedor de identidad.");
            });
        }

        @Override
        public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
            crearCuentaCount.incrementAndGet();
            dtoCapturado.set(dto);
            return resultado.get();
        }

        @Override
        public void eliminarCuenta(final String idExterno) {
            eliminarCuentaCount.incrementAndGet();
        }

        @Override
        public void asignarRol(final String idExterno, final InstitutionalRole rol) {
            throw new UnsupportedOperationException("No usado por este test.");
        }
    }
}
