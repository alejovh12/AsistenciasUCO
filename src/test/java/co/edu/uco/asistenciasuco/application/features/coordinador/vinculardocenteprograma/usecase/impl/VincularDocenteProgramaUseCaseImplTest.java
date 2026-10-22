package co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.domain.VincularDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.VincularDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU091: vinculacion de docente al programa con aprovisionamiento IAM posterior al commit en DB. */
class VincularDocenteProgramaUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID TIPO_VINCULACION = UUID.randomUUID();
    private static final UUID TIPO_ID = UUID.randomUUID();
    private static final UUID ID_USUARIO = UUID.randomUUID();
    private static final UUID ID_DOCENTE = UUID.randomUUID();
    private static final String CORREO = "jorge.henao@uco.edu.co";

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DocenteProgramaCommandPort commandPort = mock(DocenteProgramaCommandPort.class);
    private final DocenteProgramaQueryPort queryPort = mock(DocenteProgramaQueryPort.class);
    private final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
    private final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
    private final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
    private VincularDocenteProgramaUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        useCase = new VincularDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort,
                usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH_GENERADO");
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaUseCaseImpl(null, commandPort,
                queryPort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort));
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaUseCaseImpl(scopePort, null,
                queryPort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort));
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaUseCaseImpl(scopePort, commandPort,
                null, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort));
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaUseCaseImpl(scopePort, commandPort,
                queryPort, null, passwordEncoderPort, identityProviderPort));
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaUseCaseImpl(scopePort, commandPort,
                queryPort, usuarioRepositoryPort, null, identityProviderPort));
        assertThrows(NullPointerException.class, () -> new VincularDocenteProgramaUseCaseImpl(scopePort, commandPort,
                queryPort, usuarioRepositoryPort, passwordEncoderPort, null));
    }

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void T_091_08_sin_programa_del_coordinador_lanza_forbidden_y_no_toca_db_ni_iam() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(otro)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(dominioNuevo(otro, "Clave1234!")));

        verifyNoInteractions(commandPort, identityProviderPort, passwordEncoderPort);
    }

    @Test
    void T_091_01_modo_nuevo_codifica_password_persiste_hash_y_aprovisiona_rol_docente_con_password_en_claro() {
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo(CORREO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(usuario()));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(pagina(fila()));

        final DocenteProgramaDomain resultado = useCase.execute(dominioNuevo(ACTOR, "Clave1234!"));

        final ArgumentCaptor<VincularDocenteProgramaCommand> comando = ArgumentCaptor.forClass(VincularDocenteProgramaCommand.class);
        verify(commandPort).vincular(comando.capture());
        assertEquals("HASH_GENERADO", comando.getValue().passwordHash());
        assertNull(comando.getValue().idDocente());
        assertEquals(PROGRAMA, comando.getValue().idPrograma());
        assertEquals(TIPO_VINCULACION, comando.getValue().idTipoVinculacion());
        assertEquals(1017555444, comando.getValue().numeroIdentificacion());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuenta = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuenta.capture());
        assertEquals(ID_USUARIO, cuenta.getValue().idUsuario());
        assertEquals("1017555444", cuenta.getValue().username());
        assertEquals(CORREO, cuenta.getValue().correo());
        assertEquals("Clave1234!", cuenta.getValue().passwordInicial());
        assertEquals(InstitutionalRole.DOCENTE, cuenta.getValue().rolInstitucional());
        assertEquals(ID_DOCENTE, resultado.idDocente());
    }

    @Test
    void T_091_20_modo_nuevo_sin_password_no_deriva_una_por_defecto_y_falla_antes_de_la_db() {
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> useCase.execute(dominioNuevo(ACTOR, null)));
        assertThrows(ValidationException.class, () -> useCase.execute(dominioNuevo(ACTOR, "débil")));

        verifyNoInteractions(commandPort, identityProviderPort);
    }

    @Test
    void T_091_02_modo_docente_existente_no_envia_hash_y_aprovisiona_sin_password() {
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(pagina(fila()));

        final DocenteProgramaDomain resultado = useCase.execute(dominioDocente(ID_DOCENTE));

        final ArgumentCaptor<VincularDocenteProgramaCommand> comando = ArgumentCaptor.forClass(VincularDocenteProgramaCommand.class);
        verify(commandPort).vincular(comando.capture());
        assertEquals(ID_DOCENTE, comando.getValue().idDocente());
        assertNull(comando.getValue().passwordHash());
        verify(passwordEncoderPort, never()).encode(anyString());

        final ArgumentCaptor<DocentesProgramaFiltro> filtro = ArgumentCaptor.forClass(DocentesProgramaFiltro.class);
        verify(queryPort).consultar(eq(ACTOR), filtro.capture());
        assertEquals(PROGRAMA, filtro.getValue().idPrograma());
        assertEquals(ID_DOCENTE, filtro.getValue().idDocente());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuenta = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuenta.capture());
        assertNull(cuenta.getValue().passwordInicial());
        assertEquals(InstitutionalRole.DOCENTE, cuenta.getValue().rolInstitucional());
        assertEquals(ID_DOCENTE, resultado.idDocente());
    }

    @Test
    void T_091_03_modo_usuario_existente_relee_por_id_de_usuario() {
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(pagina(fila()));

        useCase.execute(new VincularDocenteProgramaDomain(ACTOR, null, ID_USUARIO, null, null, null, null, null, null,
                null, null, TIPO_VINCULACION));

        final ArgumentCaptor<VincularDocenteProgramaCommand> comando = ArgumentCaptor.forClass(VincularDocenteProgramaCommand.class);
        verify(commandPort).vincular(comando.capture());
        assertEquals(ID_USUARIO, comando.getValue().idUsuario());
        final ArgumentCaptor<DocentesProgramaFiltro> filtro = ArgumentCaptor.forClass(DocentesProgramaFiltro.class);
        verify(queryPort).consultar(eq(ACTOR), filtro.capture());
        assertEquals(ID_USUARIO, filtro.getValue().idUsuario());
        verify(identityProviderPort).crearCuenta(any());
    }

    @Test
    void T_091_16_falla_de_keycloak_tras_el_commit_lanza_error_interno_controlado_sin_compensar_db() {
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(pagina(fila()));
        when(identityProviderPort.crearCuenta(any())).thenThrow(new IdentityProviderPort.IdentityProviderException("kc caido"));

        final InternalApplicationException error = assertThrows(InternalApplicationException.class,
                () -> useCase.execute(dominioDocente(ID_DOCENTE)));

        verify(commandPort, times(1)).vincular(any());
        assertTrue(error.getCause() instanceof IdentityProviderPort.IdentityProviderException);
    }

    @Test
    void T_091_17_reintento_tras_fallo_iam_con_misma_persona_ya_vinculada_solo_reintenta_el_aprovisionamiento() {
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(pagina(fila()));

        final DocenteProgramaDomain resultado = useCase.execute(dominioNuevo(ACTOR, "Clave1234!"));

        verify(commandPort, never()).vincular(any());
        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuenta = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuenta.capture());
        assertEquals("Clave1234!", cuenta.getValue().passwordInicial());
        assertEquals(ID_DOCENTE, resultado.idDocente());
    }

    @Test
    void persona_existente_con_el_mismo_correo_pero_sin_vinculo_en_el_programa_delega_en_la_db_que_responde_DOC_003() {
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(), 0))
                .thenReturn(pagina(fila()));

        useCase.execute(dominioNuevo(ACTOR, "Clave1234!"));

        verify(commandPort).vincular(any());
    }

    @Test
    void fila_no_encontrada_despues_del_comando_es_error_interno() {
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(), 0));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominioDocente(ID_DOCENTE)));

        verifyNoInteractions(identityProviderPort);
    }

    private static VincularDocenteProgramaDomain dominioNuevo(final UUID actor, final String password) {
        return new VincularDocenteProgramaDomain(actor, null, null, TIPO_ID, 1017555444, "Jorge", null, "Henao", null,
                CORREO, password, TIPO_VINCULACION);
    }

    private static VincularDocenteProgramaDomain dominioDocente(final UUID idDocente) {
        return new VincularDocenteProgramaDomain(ACTOR, idDocente, null, null, null, null, null, null, null,
                null, null, TIPO_VINCULACION);
    }

    private static UsuarioIdentidadRepositoryProjection usuario() {
        return new UsuarioIdentidadRepositoryProjection(ID_USUARIO, TIPO_ID, 1017555444, "Jorge", "Henao", CORREO);
    }

    private static DocenteProgramaProjection fila() {
        return new DocenteProgramaProjection(UUID.randomUUID(), ID_DOCENTE, ID_USUARIO, "CC", 1017555444, "Jorge", null,
                "Henao", null, "Jorge Henao", CORREO, TIPO_VINCULACION, "CAT", "Cátedra",
                Instant.parse("2026-10-22T10:00:00Z"), 0, true);
    }

    private static DocentesProgramaPagina pagina(final DocenteProgramaProjection fila) {
        return new DocentesProgramaPagina(List.of(fila), 1);
    }
}
