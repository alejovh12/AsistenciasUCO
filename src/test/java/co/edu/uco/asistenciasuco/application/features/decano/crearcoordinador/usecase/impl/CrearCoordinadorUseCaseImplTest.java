package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", UUID.randomUUID(), "HASH", usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_crea_coordinador_con_facultad_resuelta() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", programa, "HASH", usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.of(facultad));

        useCase.execute(domain);

        final ArgumentCaptor<UUID> facultadCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("123456789"),
                org.mockito.ArgumentMatchers.eq("ANA"), org.mockito.ArgumentMatchers.eq("MARIA"),
                org.mockito.ArgumentMatchers.eq("PEREZ"), org.mockito.ArgumentMatchers.eq("GOMEZ"),
                org.mockito.ArgumentMatchers.eq("ana@uco.edu.co"), org.mockito.ArgumentMatchers.eq(programa),
                facultadCaptor.capture(), org.mockito.ArgumentMatchers.eq("HASH"),
                org.mockito.ArgumentMatchers.eq(usuario));
        assertEquals(facultad, facultadCaptor.getValue());
    }

    @Test
    void execute_con_idPrograma_nulo_y_puertos_iam_codifica_password_y_aprovisiona_rol_coordinador() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        // HU121-HU122 D5 (CONTRACT congelado 2026-10-22): la contrasena ya no tiene valor por defecto, asi que el
        // dominio la trae explicita. Antes este caso enviaba null y dependia de "UcoCoord_<doc>!". Resto sin cambios.
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", "LUIS", "HENAO", "PEREZ", "jorge.henao@uco.edu.co", null, "Clave12345678!", actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(facultad));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH_GENERADO");
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
    }

    @Test
    void execute_con_usuario_preexistente_no_persiste_password_y_reutiliza_identidad() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID preexistenteId = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", "", "HENAO", "", "jorge.henao@uco.edu.co", null, "ClaveExplicita123!", actorDecano);

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
    }

    @Test
    void execute_reintento_por_documento_reutiliza_usuario_canonico_y_no_omite_identity() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        final UUID usuarioId = UUID.randomUUID();
        final UsuarioIdentidadRepositoryProjection usuario = new UsuarioIdentidadRepositoryProjection(
                usuarioId, UUID.randomUUID(), 1023465765, "JORGE", "VALENCIA", "coordinador231@uco.edu.co");
        final UUID programa = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1023465765", "JORGE", null, "VALENCIA", null,
                "coordinador231@uco.edu.co", programa, "Clave12345678!", actorDecano);

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
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("correo@uco.edu.co"))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        UUID.randomUUID(), UUID.randomUUID(), 111111111, "ANA", "PEREZ", "correo@uco.edu.co")));
        when(usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(1023465765))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        UUID.randomUUID(), UUID.randomUUID(), 1023465765, "JORGE", "VALENCIA", "otro@uco.edu.co")));

        assertThrows(ConflictException.class, () -> useCaseConIam.execute(new CrearCoordinadorDomain(
                "1023465765", "JORGE", null, "VALENCIA", null, "correo@uco.edu.co",
                UUID.randomUUID(), "Clave12345678!", actorDecano)));

        org.mockito.Mockito.verifyNoInteractions(commandPort, passwordEncoderPort, identityProviderPort);
    }

    @Test
    void execute_no_declara_aprovisionamiento_completo_si_no_relee_usuario_despues_del_command() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("nuevo@uco.edu.co"))
                .thenReturn(Optional.empty());
        when(usuarioRepositoryPort.consultarUsuarioPorNumeroIdentificacion(1023465765))
                .thenReturn(Optional.empty());

        assertThrows(InternalApplicationException.class, () -> useCaseConIam.execute(new CrearCoordinadorDomain(
                "1023465765", "JORGE", null, "VALENCIA", null, "nuevo@uco.edu.co",
                UUID.randomUUID(), "Clave12345678!", actorDecano)));

        org.mockito.Mockito.verifyNoInteractions(identityProviderPort);
    }

    @Test
    void execute_lanza_internal_application_exception_cuando_identity_provider_falla() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", null, "HENAO", null, "jorge.henao@uco.edu.co", null, "Clave12345678!", actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(facultad));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any()))
                .thenThrow(new IdentityProviderPort.IdentityProviderException("Error Keycloak"));

        assertThrows(co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException.class,
                () -> useCaseConIam.execute(domain));
    }

    // HU121-HU122 T-120R-01 / CA-09 / D5: la contrasena se exige siempre; no existe una por defecto derivada del documento.
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   "})
    void execute_rechaza_una_contrasena_ausente_o_en_blanco_sin_tocar_db_ni_identidad(final String password) {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
        final UUID actorDecano = UUID.randomUUID();
        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(UUID.randomUUID()));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co")).thenReturn(Optional.empty());

        assertThrows(co.edu.uco.asistenciasuco.application.exception.validation.ValidationException.class,
                () -> useCaseConIam.execute(new CrearCoordinadorDomain(
                        "1017555444", "JORGE", null, "HENAO", null, "jorge.henao@uco.edu.co", null, password, actorDecano)));

        org.mockito.Mockito.verifyNoInteractions(commandPort, identityProviderPort, passwordEncoderPort);
    }
}
