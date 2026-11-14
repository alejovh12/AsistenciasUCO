package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicyFixture;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.CrearCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto.ProgramaNuevoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.interactor.CrearCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.ProgramaNuevoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DEC-065 (5): el decano registra coordinador + programa en un solo envio, con la facultad del decano autenticado. El caso de uso
 * elige el comando compuesto, mantiene el resto del flujo y devuelve ids y nombre confirmados por la base de datos.
 */
class CrearCoordinadorConProgramaUseCaseImplTest {

    private static final UUID DECANO = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID FACULTAD = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID TIPO_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID TIPO_PROGRAMA = UUID.fromString("33333333-3333-4444-5555-666666666666");
    private static final UUID MODALIDAD = UUID.fromString("44444444-3333-4444-5555-666666666666");
    private static final UUID JORNADA = UUID.fromString("55555555-3333-4444-5555-666666666666");
    private static final UUID COORDINADOR = UUID.fromString("66666666-3333-4444-5555-666666666666");
    private static final UUID PROGRAMA = UUID.fromString("77777777-3333-4444-5555-666666666666");
    private static final UUID USUARIO = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String CORREO = "jorge.henao@uco.edu.co";
    private static final String CLAVE = "Clave12345678!";

    private CoordinadorCommandPort commandPort;
    private InstitutionalScopePort scopePort;
    private UsuarioRepositoryPort usuarios;
    private PasswordEncoderPort encoder;
    private IdentityProviderPort identity;
    private CrearCoordinadorUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(CoordinadorCommandPort.class);
        scopePort = mock(InstitutionalScopePort.class);
        usuarios = mock(UsuarioRepositoryPort.class);
        encoder = mock(PasswordEncoderPort.class);
        identity = mock(IdentityProviderPort.class);
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
        when(encoder.encode(anyString())).thenReturn("HASH");
        when(identity.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        useCase = new CrearCoordinadorUseCaseImpl(commandPort, scopePort, usuarios, encoder, identity);
    }

    @Test
    void programa_nuevo_usa_el_comando_compuesto_con_la_facultad_del_decano_y_devuelve_los_ids() {
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty(), Optional.of(usuario()));
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.empty());
        when(commandPort.crearCoordinadorConPrograma(any())).thenReturn(
                new CoordinadorCommandPort.ResultadoCoordinadorConPrograma(COORDINADOR, PROGRAMA, "Ingenieria de Sistemas"));

        final var resultado = useCase.execute(dominio(null, new ProgramaNuevoDomain("Ingenieria de Sistemas", TIPO_PROGRAMA, MODALIDAD, JORNADA)));

        assertEquals(COORDINADOR, resultado.idCoordinador());
        assertEquals(PROGRAMA, resultado.idPrograma());
        assertEquals("Ingenieria de Sistemas", resultado.nombrePrograma());
        final ArgumentCaptor<CoordinadorCommandPort.CrearCoordinadorConProgramaCommand> db =
                ArgumentCaptor.forClass(CoordinadorCommandPort.CrearCoordinadorConProgramaCommand.class);
        verify(commandPort).crearCoordinadorConPrograma(db.capture());
        assertNotNull(db.getValue().idCoordinador());
        assertEquals(TIPO_ID, db.getValue().tipoIdentificacionId());
        assertEquals("1017555444", db.getValue().numeroIdentificacion());
        assertEquals("JORGE", db.getValue().primerNombre());
        assertEquals("LUIS", db.getValue().segundoNombre());
        assertEquals("HENAO", db.getValue().primerApellido());
        assertEquals("PEREZ", db.getValue().segundoApellido());
        assertEquals(CORREO, db.getValue().correo());
        assertEquals("HASH", db.getValue().password());
        assertEquals("Ingenieria de Sistemas", db.getValue().nombrePrograma());
        assertEquals(TIPO_PROGRAMA, db.getValue().idTipoPrograma());
        assertEquals(MODALIDAD, db.getValue().idModalidad());
        assertEquals(JORNADA, db.getValue().idJornada());
        assertEquals(FACULTAD, db.getValue().idFacultad());
        assertEquals(DECANO, db.getValue().usuarioEjecutor());
        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuenta = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identity).crearCuenta(cuenta.capture());
        assertEquals(USUARIO, cuenta.getValue().idUsuario());
        assertEquals(InstitutionalRole.COORDINADOR, cuenta.getValue().rolInstitucional());
        verify(commandPort, never()).crearCoordinador(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(commandPort, never()).crearCoordinador(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void programa_nuevo_sin_modalidad_ni_jornada_los_envia_nulos() {
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty(), Optional.of(usuario()));
        when(commandPort.crearCoordinadorConPrograma(any())).thenReturn(
                new CoordinadorCommandPort.ResultadoCoordinadorConPrograma(COORDINADOR, PROGRAMA, "P"));

        useCase.execute(dominio(null, new ProgramaNuevoDomain("P", TIPO_PROGRAMA, null, null)));

        final ArgumentCaptor<CoordinadorCommandPort.CrearCoordinadorConProgramaCommand> db =
                ArgumentCaptor.forClass(CoordinadorCommandPort.CrearCoordinadorConProgramaCommand.class);
        verify(commandPort).crearCoordinadorConPrograma(db.capture());
        assertNull(db.getValue().idModalidad());
        assertNull(db.getValue().idJornada());
    }

    @Test
    void programa_nuevo_e_id_programa_a_la_vez_es_validacion_antes_de_tocar_la_base() {
        final var ambos = dominio(PROGRAMA, new ProgramaNuevoDomain("P", TIPO_PROGRAMA, null, null));

        assertThrows(ValidationException.class, () -> useCase.execute(ambos));

        verifyNoInteractions(commandPort, usuarios, encoder, identity, scopePort);
    }

    @Test
    void programa_nuevo_sin_tipo_es_validacion_antes_de_tocar_la_base() {
        final var sinTipo = dominio(null, new ProgramaNuevoDomain("P", null, null, null));

        assertThrows(ValidationException.class, () -> useCase.execute(sinTipo));

        verifyNoInteractions(commandPort, usuarios, encoder, identity, scopePort);
    }

    @Test
    void programa_nuevo_sin_facultad_del_decano_es_prohibido() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(dominio(null, new ProgramaNuevoDomain("P", TIPO_PROGRAMA, null, null))));

        verifyNoInteractions(commandPort, identity);
    }

    @Test
    void rechazo_de_la_base_impide_la_cuenta_de_identidad() {
        doThrow(new ConflictException("Ya existe un programa con ese nombre.")).when(commandPort).crearCoordinadorConPrograma(any());

        assertThrows(ConflictException.class,
                () -> useCase.execute(dominio(null, new ProgramaNuevoDomain("P", TIPO_PROGRAMA, null, null))));

        verifyNoInteractions(identity);
    }

    @Test
    void con_id_programa_existente_el_resultado_trae_el_programa_vinculado_y_nombre_nulo() {
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty(), Optional.of(usuario()));

        final var resultado = useCase.execute(dominio(PROGRAMA, null));

        assertNotNull(resultado.idCoordinador());
        assertEquals(PROGRAMA, resultado.idPrograma());
        assertNull(resultado.nombrePrograma());
        verify(commandPort, never()).crearCoordinadorConPrograma(any());
    }

    @Test
    void sin_tipo_de_identificacion_ni_programa_usa_el_comando_clasico_y_resultado_sin_programa() {
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty(), Optional.of(usuario()));
        final var domain = new CrearCoordinadorDomain(null, "1017555444", "JORGE", "LUIS", "HENAO", "PEREZ", CORREO, null, CLAVE, DECANO);

        final var resultado = useCase.execute(domain);

        assertNotNull(resultado.idCoordinador());
        assertNull(resultado.idPrograma());
        assertNull(resultado.nombrePrograma());
        verify(commandPort).crearCoordinador(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any());
    }

    @Test
    void el_interactor_convierte_dto_a_dominio_y_resultado_a_dto() {
        when(usuarios.consultarUsuarioPorNumeroIdentificacion(1017555444)).thenReturn(Optional.empty(), Optional.of(usuario()));
        when(commandPort.crearCoordinadorConPrograma(any())).thenReturn(
                new CoordinadorCommandPort.ResultadoCoordinadorConPrograma(COORDINADOR, PROGRAMA, "Ingenieria de Sistemas"));

        final var resultado = new CrearCoordinadorInteractor(useCase, CorreoInstitucionalPolicyFixture.institucional()).execute(new CrearCoordinadorDTO(
                TIPO_ID, "1017555444", "JORGE", "LUIS", "HENAO", "PEREZ", CORREO, null, CLAVE, DECANO,
                new ProgramaNuevoDTO("Ingenieria de Sistemas", TIPO_PROGRAMA, MODALIDAD, JORNADA)));

        assertEquals(COORDINADOR, resultado.idCoordinador());
        assertEquals(PROGRAMA, resultado.idPrograma());
        assertEquals("Ingenieria de Sistemas", resultado.nombrePrograma());
    }

    private static CrearCoordinadorDomain dominio(final UUID idPrograma, final ProgramaNuevoDomain programaNuevo) {
        return new CrearCoordinadorDomain(TIPO_ID, "1017555444", "JORGE", "LUIS", "HENAO", "PEREZ", CORREO, idPrograma, CLAVE, DECANO,
                programaNuevo);
    }

    private static UsuarioIdentidadRepositoryProjection usuario() {
        return new UsuarioIdentidadRepositoryProjection(USUARIO, TIPO_ID, 1017555444, "JORGE", "HENAO", CORREO);
    }
}
