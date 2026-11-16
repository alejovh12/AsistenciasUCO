package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.ActivacionCuentaService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCoordinadorUseCaseImplTest {

    private final CoordinadorCommandPort commandPort = mock(CoordinadorCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CrearCoordinadorUseCaseImpl useCase = new CrearCoordinadorUseCaseImpl(commandPort, scopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_facultad_del_decano() {
        final UUID usuario = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", UUID.randomUUID(), usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_crea_coordinador_con_facultad_resuelta() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", programa, usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.of(facultad));

        useCase.execute(domain);

        final ArgumentCaptor<UUID> facultadCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("123456789"),
                org.mockito.ArgumentMatchers.eq("ANA"), org.mockito.ArgumentMatchers.eq("MARIA"),
                org.mockito.ArgumentMatchers.eq("PEREZ"), org.mockito.ArgumentMatchers.eq("GOMEZ"),
                org.mockito.ArgumentMatchers.eq("ana@uco.edu.co"), org.mockito.ArgumentMatchers.eq(programa),
                facultadCaptor.capture(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(usuario));
        assertEquals(facultad, facultadCaptor.getValue());
    }

    @Test
    void execute_con_idPrograma_nulo_sin_contrasena_cuenta_pendiente_y_aviso_de_activacion() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        // IDENT-002: el alta ya no recibe contrasena; la BD guarda el secreto inutilizable y la cuenta nace pendiente.
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", "LUIS", "HENAO", "PEREZ", "jorge.henao@uco.edu.co", null, actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(facultad));
        when(activacion.credencialLegadaInutilizable()).thenReturn("HASH_GENERADO");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-coord-1", true));

        useCaseConIam.execute(domain);

        final ArgumentCaptor<UUID> programaCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("1017555444"),
                org.mockito.ArgumentMatchers.eq("JORGE"), org.mockito.ArgumentMatchers.eq("LUIS"),
                org.mockito.ArgumentMatchers.eq("HENAO"), org.mockito.ArgumentMatchers.eq("PEREZ"),
                org.mockito.ArgumentMatchers.eq("jorge.henao@uco.edu.co"), programaCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(facultad), passwordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(actorDecano));
        assertNull(programaCaptor.getValue());
        assertEquals("HASH_GENERADO", passwordCaptor.getValue());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuentaCaptor = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuentaCaptor.capture());
        assertEquals(usuarioCreadoId, cuentaCaptor.getValue().idUsuario());
        assertEquals("1017555444", cuentaCaptor.getValue().username());
        assertEquals("jorge.henao@uco.edu.co", cuentaCaptor.getValue().correo());
        assertEquals(InstitutionalRole.COORDINADOR, cuentaCaptor.getValue().rolInstitucional());
        assertNull(cuentaCaptor.getValue().passwordInicial());
        assertTrue(cuentaCaptor.getValue().requiereVerificacionCorreo());
        verify(activacion).avisar(usuarioCreadoId, "JORGE", InstitutionalRole.COORDINADOR);
    }

    @Test
    void execute_con_usuario_preexistente_no_persiste_password_y_reutiliza_identidad() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID preexistenteId = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", "", "HENAO", "", "jorge.henao@uco.edu.co", null, actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(facultad));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co"))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        preexistenteId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(1017555444))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        preexistenteId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(usuarioRepositoryPort.consultarUsuarioPorId(preexistenteId)).thenReturn(Optional.of(
                new UsuarioIdentidadRepositoryProjection(
                        preexistenteId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-coord-1", false));

        useCaseConIam.execute(domain);

        final ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("1017555444"),
                org.mockito.ArgumentMatchers.eq("JORGE"), org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq("HENAO"), org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq("jorge.henao@uco.edu.co"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(facultad), passwordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(actorDecano));
        assertNull(passwordCaptor.getValue());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuentaCaptor = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuentaCaptor.capture());
        assertEquals(preexistenteId, cuentaCaptor.getValue().idUsuario());
        // La persona ya tenia cuenta en el proveedor: no recibe aviso de activacion.
        verify(activacion, org.mockito.Mockito.never()).avisar(any(), any(), any());
    }

    @Test
    void execute_reintento_por_documento_reutiliza_usuario_canonico_y_no_omite_identity() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        final UUID usuarioId = UUID.randomUUID();
        final UsuarioIdentidadRepositoryProjection usuario = new UsuarioIdentidadRepositoryProjection(
                usuarioId, UUID.randomUUID(), 1023465765, "JORGE", "VALENCIA", "coordinador231@uco.edu.co");
        final UUID programa = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1023465765", "JORGE", null, "VALENCIA", null,
                "coordinador231@uco.edu.co", programa, actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("coordinador231@uco.edu.co"))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(1023465765))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepositoryPort.consultarUsuarioPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-jorge", false));

        useCaseConIam.execute(domain);

        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("1023465765"),
                org.mockito.ArgumentMatchers.eq("JORGE"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("VALENCIA"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("coordinador231@uco.edu.co"),
                org.mockito.ArgumentMatchers.eq(programa), any(UUID.class),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(actorDecano));
        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuentaCaptor = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuentaCaptor.capture());
        assertEquals(usuarioId, cuentaCaptor.getValue().idUsuario());
        assertEquals("coordinador231@uco.edu.co", cuentaCaptor.getValue().correo());
    }

    @Test
    void execute_rechaza_correo_y_documento_de_personas_distintas_sin_command() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("correo@uco.edu.co"))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        UUID.randomUUID(), UUID.randomUUID(), 111111111, "ANA", "PEREZ", "correo@uco.edu.co")));
        when(usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(1023465765))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        UUID.randomUUID(), UUID.randomUUID(), 1023465765, "JORGE", "VALENCIA", "otro@uco.edu.co")));

        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1023465765", "JORGE", null, "VALENCIA", null, "correo@uco.edu.co",
                UUID.randomUUID(), actorDecano);
        assertThrows(ConflictException.class, () -> useCaseConIam.execute(domain));

        org.mockito.Mockito.verifyNoInteractions(commandPort, activacion, identityProviderPort);
    }

    @Test
    void execute_no_declara_aprovisionamiento_completo_si_no_relee_usuario_despues_del_command() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(activacion.credencialLegadaInutilizable()).thenReturn("HASH");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("nuevo@uco.edu.co"))
                .thenReturn(Optional.empty());
        when(usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(1023465765))
                .thenReturn(Optional.empty());

        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1023465765", "JORGE", null, "VALENCIA", null, "nuevo@uco.edu.co",
                UUID.randomUUID(), actorDecano);
        assertThrows(InternalApplicationException.class, () -> useCaseConIam.execute(domain));

        org.mockito.Mockito.verifyNoInteractions(identityProviderPort);
    }

    @Test
    void execute_lanza_internal_application_exception_cuando_identity_provider_falla() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", null, "HENAO", null, "jorge.henao@uco.edu.co", null, actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(facultad));
        when(activacion.credencialLegadaInutilizable()).thenReturn("HASH");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any()))
                .thenThrow(new IdentityProviderPort.IdentityProviderException("Error Keycloak"));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException.class,
                () -> useCaseConIam.execute(domain));
    }

    // IDENT-002 (antes HU121-HU122 D5): el alta ya no exige contrasena; sin clave se registra y se avisa la activacion.
    @Test
    void execute_alta_nueva_sin_contrasena_no_falla_y_no_usa_credencial_recibida() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, activacion, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(activacion.credencialLegadaInutilizable()).thenReturn("SECRETO_INUTILIZABLE");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-coord-9", true));

        useCaseConIam.execute(new CrearCoordinadorDomain(
                "1017555444", "JORGE", null, "HENAO", null, "jorge.henao@uco.edu.co", null, actorDecano));

        verify(commandPort).crearCoordinador(any(UUID.class), anyString(), anyString(), any(), anyString(), any(),
                anyString(), any(), any(UUID.class), org.mockito.ArgumentMatchers.eq("SECRETO_INUTILIZABLE"), any(UUID.class));
        verify(activacion).avisar(usuarioCreadoId, "JORGE", InstitutionalRole.COORDINADOR);
    }

    private static final String CORREO = "jorge.henao@uco.edu.co";

    private static UsuarioIdentidadRepositoryProjection usuario(final UUID id, final String correo) {
        return new UsuarioIdentidadRepositoryProjection(id, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", correo);
    }

    private static CrearCoordinadorDomain dominio(final String documento, final String correo, final UUID actorDecano) {
        return new CrearCoordinadorDomain(documento, "JORGE", null, "HENAO", null, correo, null, actorDecano);
    }

    private UUID decanoConFacultad() {
        final UUID actorDecano = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        return actorDecano;
    }

    @Test
    void execute_rechaza_un_correo_ya_registrado_con_otro_documento_sin_command() {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConUsuarios = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, mock(ActivacionCuentaService.class), mock(IdentityProviderPort.class));
        final UUID actorDecano = decanoConFacultad();
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(usuario(UUID.randomUUID(), CORREO)));
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty());
        final CrearCoordinadorDomain domain = dominio("1017555444", CORREO, actorDecano);

        assertThrows(ConflictException.class, () -> useCaseConUsuarios.execute(domain));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void execute_rechaza_un_documento_ya_registrado_con_otro_correo_sin_command() {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConUsuarios = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, mock(ActivacionCuentaService.class), mock(IdentityProviderPort.class));
        final UUID actorDecano = decanoConFacultad();
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444))
                .thenReturn(Optional.of(usuario(UUID.randomUUID(), "otro@uco.edu.co")));
        final CrearCoordinadorDomain domain = dominio("1017555444", CORREO, actorDecano);

        assertThrows(ConflictException.class, () -> useCaseConUsuarios.execute(domain));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void execute_sin_correo_en_el_dominio_solo_consulta_por_documento() {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConUsuarios = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, mock(ActivacionCuentaService.class), mock(IdentityProviderPort.class));
        final UUID actorDecano = decanoConFacultad();
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444))
                .thenReturn(Optional.of(usuario(UUID.randomUUID(), CORREO)));
        final CrearCoordinadorDomain domain = dominio("1017555444", null, actorDecano);

        assertThrows(ConflictException.class, () -> useCaseConUsuarios.execute(domain));

        verify(usuarios, org.mockito.Mockito.never()).consultarUsuarioPorCorreo(anyString());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "ABC123"})
    void execute_con_documento_vacio_o_no_numerico_no_consulta_por_documento_y_resuelve_por_correo(
            final String documento) {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, activacion, identityProviderPort);
        final UUID actorDecano = decanoConFacultad();
        final UUID usuarioCreadoId = UUID.randomUUID();
        when(activacion.credencialLegadaInutilizable()).thenReturn("HASH");
        when(usuarios.consultarUsuarioPorCorreo(CORREO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(usuario(usuarioCreadoId, CORREO)));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-coord-2", true));

        useCaseConIam.execute(dominio(documento, CORREO, actorDecano));

        verify(usuarios, org.mockito.Mockito.never()).consultarUsuarioPorNumeroIdentificacion(any());
        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuenta = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuenta.capture());
        assertEquals(usuarioCreadoId, cuenta.getValue().idUsuario());
    }

    static java.util.stream.Stream<UsuarioIdentidadRepositoryProjection> proyeccionesNoCanonicas() {
        final UUID id = UUID.randomUUID();
        final UUID tipo = UUID.randomUUID();
        return java.util.stream.Stream.of(
                new UsuarioIdentidadRepositoryProjection(null, tipo, 1017555444, "JORGE", "HENAO", CORREO),
                new UsuarioIdentidadRepositoryProjection(id, tipo, null, "JORGE", "HENAO", CORREO),
                new UsuarioIdentidadRepositoryProjection(id, tipo, 1017555444, " ", "HENAO", CORREO),
                new UsuarioIdentidadRepositoryProjection(id, tipo, 1017555444, "JORGE", null, CORREO),
                new UsuarioIdentidadRepositoryProjection(id, tipo, 1017555444, "JORGE", "HENAO", ""));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("proyeccionesNoCanonicas")
    void execute_rechaza_una_identidad_existente_que_no_es_canonica_sin_command(
            final UsuarioIdentidadRepositoryProjection incompleta) {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConUsuarios = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, mock(ActivacionCuentaService.class), mock(IdentityProviderPort.class));
        final UUID actorDecano = decanoConFacultad();
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(incompleta));
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.of(incompleta));
        final CrearCoordinadorDomain domain = dominio("1017555444", CORREO, actorDecano);

        assertThrows(InternalApplicationException.class, () -> useCaseConUsuarios.execute(domain));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void execute_sin_proveedor_de_identidad_persiste_el_coordinador_sin_aprovisionar() {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
        final CrearCoordinadorUseCaseImpl useCaseSinIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, activacion, null);
        final UUID actorDecano = decanoConFacultad();
        when(activacion.credencialLegadaInutilizable()).thenReturn("HASH");
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty());

        useCaseSinIam.execute(dominio("1017555444", CORREO, actorDecano));

        verify(commandPort).crearCoordinador(any(UUID.class), anyString(), anyString(), any(), anyString(), any(),
                anyString(), any(), any(UUID.class), org.mockito.ArgumentMatchers.eq("HASH"), any(UUID.class));
    }

    @Test
    void execute_sin_servicio_de_activacion_persiste_credencial_nula() {
        final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
        final CrearCoordinadorUseCaseImpl useCaseSinCodificador = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarios, null, null);
        final UUID actorDecano = decanoConFacultad();
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty());

        useCaseSinCodificador.execute(dominio("1017555444", CORREO, actorDecano));

        verify(commandPort).crearCoordinador(any(UUID.class), anyString(), anyString(), any(), anyString(), any(),
                anyString(), any(), any(UUID.class), org.mockito.ArgumentMatchers.isNull(), any(UUID.class));
    }

    @Test
    void puerto_de_usuarios_no_consulta_por_documento_solo_por_defecto() {
        final UsuarioRepositoryPort puerto = mock(UsuarioRepositoryPort.class, org.mockito.Mockito.CALLS_REAL_METHODS);

        assertEquals(Optional.empty(), puerto.consultarUsuarioPorNumeroIdentificacion(1017555444));
    }
}
