package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.FacultadNuevaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.interactor.CrearDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.FacultadNuevaDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DEC-065 (5): el administrador registra decano + facultad en un solo envio. El caso de uso elige el comando compuesto, mantiene el
 * resto del flujo (identidad, hash, cuenta) y devuelve los ids confirmados por la base de datos.
 */
class CrearDecanoConFacultadUseCaseImplTest {

    private static final UUID FACULTAD_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID INSTITUCION_ID = UUID.fromString("44444444-2222-3333-4444-555555555555");
    private static final UUID DECANO_ID = UUID.fromString("77777777-2222-3333-4444-555555555555");
    private static final UUID TIPO_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID USUARIO_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID EJECUTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final String CORREO = "nuevo@uco.edu.co";
    private static final String RAW = "Clave123!";
    private static final String HASH = "HASH_CONTROLADO";

    private FacultadQueryPort facultad;
    private DecanoCommandPort command;
    private UsuarioRepositoryPort usuarios;
    private PasswordEncoderPort encoder;
    private IdentityProviderPort identity;
    private CrearDecanoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        facultad = mock(FacultadQueryPort.class);
        command = mock(DecanoCommandPort.class);
        usuarios = mock(UsuarioRepositoryPort.class);
        encoder = mock(PasswordEncoderPort.class);
        identity = mock(IdentityProviderPort.class);
        when(identity.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        useCase = new CrearDecanoUseCaseImpl(facultad, command, usuarios, encoder,
                new AsegurarCuentaIdentidadService(identity, mock(UsuarioSincronizacionIdentidadPort.class)));
    }

    @Test
    void facultad_nueva_usa_el_comando_compuesto_y_devuelve_los_ids_de_la_base() {
        when(encoder.encode(RAW)).thenReturn(HASH);
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789))
                .thenReturn(Optional.empty(), Optional.of(usuario()));
        when(command.crearDecanoConFacultad(any())).thenReturn(
                new DecanoCommandPort.ResultadoDecanoConFacultad(DECANO_ID, FACULTAD_ID, "Facultad de Ingenieria"));

        final var resultado = useCase.execute(dominio(null, new FacultadNuevaDomain("Facultad de Ingenieria", "ING", INSTITUCION_ID)));

        assertEquals(DECANO_ID, resultado.idDecano());
        assertEquals(FACULTAD_ID, resultado.idFacultad());
        assertEquals("Facultad de Ingenieria", resultado.nombreFacultad());
        final ArgumentCaptor<DecanoCommandPort.CrearDecanoConFacultadCommand> db =
                ArgumentCaptor.forClass(DecanoCommandPort.CrearDecanoConFacultadCommand.class);
        final ArgumentCaptor<CrearCuentaIdentidadDTO> idp = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        final InOrder orden = inOrder(command, usuarios, identity);
        orden.verify(command).crearDecanoConFacultad(db.capture());
        orden.verify(usuarios).consultarUsuarioPorIdentificacion(TIPO_ID, 123456789);
        orden.verify(identity).crearCuenta(idp.capture());
        assertNotNull(db.getValue().idDecano());
        assertEquals(TIPO_ID, db.getValue().tipoIdentificacionId());
        assertEquals(123456789, db.getValue().numeroIdentificacion());
        assertEquals("Ana", db.getValue().primerNombre());
        assertEquals("Maria", db.getValue().segundoNombre());
        assertEquals("Perez", db.getValue().primerApellido());
        assertEquals("Gomez", db.getValue().segundoApellido());
        assertEquals(CORREO, db.getValue().correo());
        assertEquals(HASH, db.getValue().password());
        assertEquals("Facultad de Ingenieria", db.getValue().nombreFacultad());
        assertEquals("ING", db.getValue().codigoFacultad());
        assertEquals(INSTITUCION_ID, db.getValue().idInstitucion());
        assertEquals(EJECUTOR, db.getValue().usuarioEjecutor());
        assertEquals(RAW, idp.getValue().passwordInicial());
        assertEquals(InstitutionalRole.DECANO, idp.getValue().rolInstitucional());
        verify(command, never()).crearDecano(any());
        verifyNoInteractions(facultad);
    }

    @Test
    void facultad_nueva_con_persona_preexistente_no_persiste_password() {
        when(usuarios.consultarUsuarioPorCorreo(CORREO)).thenReturn(Optional.of(usuario()));
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789)).thenReturn(Optional.of(usuario()));
        when(usuarios.consultarUsuarioPorId(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(command.crearDecanoConFacultad(any())).thenReturn(
                new DecanoCommandPort.ResultadoDecanoConFacultad(DECANO_ID, FACULTAD_ID, "F"));

        useCase.execute(dominio(null, new FacultadNuevaDomain("Facultad de Ingenieria", null, INSTITUCION_ID)));

        final ArgumentCaptor<DecanoCommandPort.CrearDecanoConFacultadCommand> db =
                ArgumentCaptor.forClass(DecanoCommandPort.CrearDecanoConFacultadCommand.class);
        verify(command).crearDecanoConFacultad(db.capture());
        assertNull(db.getValue().password());
        assertNull(db.getValue().codigoFacultad());
        verifyNoInteractions(encoder);
    }

    @Test
    void facultad_nueva_e_id_facultad_a_la_vez_es_validacion_antes_de_tocar_la_base() {
        final var ambos = dominio(FACULTAD_ID, new FacultadNuevaDomain("Facultad de Ingenieria", null, INSTITUCION_ID));

        assertThrows(ValidationException.class, () -> useCase.execute(ambos));

        verifyNoInteractions(command, usuarios, encoder, identity, facultad);
    }

    @Test
    void rechazo_de_la_base_impide_la_cuenta_de_identidad() {
        when(encoder.encode(RAW)).thenReturn(HASH);
        doThrow(new ConflictException("Ya existe una facultad con ese nombre.")).when(command).crearDecanoConFacultad(any());

        assertThrows(ConflictException.class,
                () -> useCase.execute(dominio(null, new FacultadNuevaDomain("Facultad de Ingenieria", null, INSTITUCION_ID))));

        verifyNoInteractions(identity);
        verify(usuarios, never()).consultarUsuarioPorId(any());
    }

    @Test
    void con_id_facultad_existente_el_resultado_trae_la_facultad_vinculada() {
        when(facultad.consultarFacultadPorId(FACULTAD_ID)).thenReturn(Optional.of(new FacultadProjection(
                FACULTAD_ID, "Facultad existente", null, null, null, null, true, "Activa")));
        when(encoder.encode(RAW)).thenReturn(HASH);
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789))
                .thenReturn(Optional.empty(), Optional.of(usuario()));

        final var resultado = useCase.execute(dominio(FACULTAD_ID, null));

        assertNotNull(resultado.idDecano());
        assertEquals(FACULTAD_ID, resultado.idFacultad());
        assertEquals("Facultad existente", resultado.nombreFacultad());
        verify(command, never()).crearDecanoConFacultad(any());
    }

    @Test
    void sin_facultad_el_resultado_trae_facultad_nula() {
        when(encoder.encode(RAW)).thenReturn(HASH);
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789))
                .thenReturn(Optional.empty(), Optional.of(usuario()));

        final var resultado = useCase.execute(dominio(null, null));

        assertNotNull(resultado.idDecano());
        assertNull(resultado.idFacultad());
        assertNull(resultado.nombreFacultad());
    }

    @Test
    void el_interactor_convierte_dto_a_dominio_y_resultado_a_dto() {
        when(encoder.encode(RAW)).thenReturn(HASH);
        when(usuarios.consultarUsuarioPorIdentificacion(TIPO_ID, 123456789))
                .thenReturn(Optional.empty(), Optional.of(usuario()));
        when(command.crearDecanoConFacultad(any())).thenReturn(
                new DecanoCommandPort.ResultadoDecanoConFacultad(DECANO_ID, FACULTAD_ID, "Facultad de Ingenieria"));

        final var resultado = new CrearDecanoInteractor(useCase).execute(new CrearDecanoDTO(
                TIPO_ID, 123456789, "Ana", "Maria", "Perez", "Gomez", CORREO, RAW, null, EJECUTOR,
                new FacultadNuevaDTO("Facultad de Ingenieria", "ING", INSTITUCION_ID)));

        assertEquals(DECANO_ID, resultado.idDecano());
        assertEquals(FACULTAD_ID, resultado.idFacultad());
        assertEquals("Facultad de Ingenieria", resultado.nombreFacultad());
    }

    private static CrearDecanoDomain dominio(final UUID idFacultad, final FacultadNuevaDomain facultadNueva) {
        return new CrearDecanoDomain(TIPO_ID, 123456789, "Ana", "Maria", "Perez", "Gomez", CORREO, RAW, idFacultad, EJECUTOR,
                facultadNueva);
    }

    private static UsuarioIdentidadRepositoryProjection usuario() {
        return new UsuarioIdentidadRepositoryProjection(USUARIO_ID, TIPO_ID, 123456789, "ANA", "PEREZ", CORREO);
    }
}
