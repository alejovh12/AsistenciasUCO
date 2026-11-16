package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmisionEstudianteResultadoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmitirEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.EstadoCuentaAprovisionada;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.ResultadoCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdmisionEstudianteCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteResultadoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.ActivacionCuentaService;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CULM-001 D.4 (CONTRACT §5.3, HU156): todas las ramas de la admision de estudiantes. */
class AdmitirEstudianteUseCaseImplTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String CORREO = "ana.perez@uco.edu.co";
    private static final String HASH = "{hash}secreto-inutilizable";

    private final AdmisionEstudianteCommandPort comando = mock(AdmisionEstudianteCommandPort.class);
    private final UsuarioRepositoryPort usuarios = mock(UsuarioRepositoryPort.class);
    private final ActivacionCuentaService activacion = mock(ActivacionCuentaService.class);
    private final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
    private final MessageCatalogPort mensajes = mock(MessageCatalogPort.class);
    private final AsegurarCuentaIdentidadService cuentas = mock(AsegurarCuentaIdentidadService.class);
    private final InstitutionalScopePort ambito = mock(InstitutionalScopePort.class);
    private final AdmitirEstudianteUseCaseImpl useCase;

    AdmitirEstudianteUseCaseImplTest() {
        when(parametros.getRequiredParameter("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE")).thenReturn("@uco.edu.co");
        when(mensajes.getUserMessage(any(), any())).thenReturn("El correo debe ser institucional (@uco.edu.co).");
        when(activacion.credencialLegadaInutilizable()).thenReturn(HASH);
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.empty());
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.empty());
        useCase = new AdmitirEstudianteUseCaseImpl(comando, usuarios,
                new CorreoInstitucionalPolicy(parametros, mensajes), cuentas, ambito, activacion);
    }

    private static AdmitirEstudianteDomain domain(final String correo) {
        return new AdmitirEstudianteDomain(TIPO, 1017123456, "Ana", null, "Perez", null, correo, PROGRAMA, ADMIN);
    }

    private static UsuarioIdentidadRepositoryProjection canonico() {
        return new UsuarioIdentidadRepositoryProjection(USUARIO, TIPO, 1017123456, "Ana", "Perez", CORREO);
    }

    private void comandoExitoso(final boolean usuarioNuevo, final boolean estudianteNuevo) {
        when(comando.admitir(any())).thenReturn(new AdmisionEstudianteResultadoProjection(USUARIO, ESTUDIANTE, usuarioNuevo, estudianteNuevo));
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(Optional.of(canonico()));
    }

    @Test
    void alta_nueva_sin_contrasena_admite_aprovisiona_pendiente_y_avisa_la_activacion() {
        comandoExitoso(true, true);
        when(cuentas.asegurar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION, false));

        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(domain(CORREO));

        assertEquals(ESTUDIANTE, resultado.idEstudiante());
        assertEquals(USUARIO, resultado.idUsuario());
        assertTrue(resultado.esUsuarioNuevo());
        assertEquals(EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION, resultado.estadoCuenta());
        assertFalse(resultado.cuentaReparada());

        final ArgumentCaptor<AdmisionEstudianteCommand> orden = ArgumentCaptor.forClass(AdmisionEstudianteCommand.class);
        verify(comando).admitir(orden.capture());
        assertEquals(HASH, orden.getValue().passwordHash());
        assertEquals(PROGRAMA, orden.getValue().idPrograma());
        assertEquals(ADMIN, orden.getValue().usuarioEjecutor());
        assertEquals(CORREO, orden.getValue().correo());

        final ArgumentCaptor<SolicitudCuentaIdentidad> solicitud = ArgumentCaptor.forClass(SolicitudCuentaIdentidad.class);
        verify(cuentas).asegurar(solicitud.capture());
        assertTrue(solicitud.getValue().requiereVerificacionCorreo());
        assertNull(solicitud.getValue().passwordInicial());
        assertEquals(InstitutionalRole.ESTUDIANTE, solicitud.getValue().rol());
        assertEquals(USUARIO, solicitud.getValue().idUsuario());
        assertEquals("1017123456", solicitud.getValue().username());

        // IDENT-002: el aviso es el de activacion de cuenta (el estudiante define su contrasena con el enlace).
        verify(activacion).avisar(USUARIO, "Ana", InstitutionalRole.ESTUDIANTE);
    }

    @Test
    void cuenta_que_ya_existia_en_el_proveedor_no_recibe_aviso_de_activacion() {
        comandoExitoso(true, true);
        when(cuentas.asegurar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, false));

        assertEquals(EstadoCuentaAprovisionada.ACTIVA, useCase.execute(domain(CORREO)).estadoCuenta());
        verify(activacion, never()).avisar(any(), any(), any());
    }

    @Test
    void alta_nueva_con_correo_no_institucional_es_validacion_VCE_001_y_no_toca_la_base() {
        assertThrows(ValidationException.class, () -> useCase.execute(domain("ana@gmail.com")));
        verifyNoInteractions(comando, cuentas);
    }

    @Test
    void persona_existente_no_revalida_correo_no_genera_credencial_y_no_avisa_si_su_cuenta_esta_activa() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        comandoExitoso(false, true);
        when(cuentas.asegurar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, false));

        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(domain("ana@externo.com"));

        assertFalse(resultado.esUsuarioNuevo());
        final ArgumentCaptor<AdmisionEstudianteCommand> orden = ArgumentCaptor.forClass(AdmisionEstudianteCommand.class);
        verify(comando).admitir(orden.capture());
        assertEquals(null, orden.getValue().passwordHash());
        verify(activacion, never()).credencialLegadaInutilizable();
        final ArgumentCaptor<SolicitudCuentaIdentidad> solicitud = ArgumentCaptor.forClass(SolicitudCuentaIdentidad.class);
        verify(cuentas).asegurar(solicitud.capture());
        // IDENT-002: sin contrasena, si faltara la cuenta nace pendiente; una cuenta existente no cambia.
        assertTrue(solicitud.getValue().requiereVerificacionCorreo());
        verify(activacion, never()).avisar(any(), any(), any());
    }

    @Test
    void una_sola_clave_existente_deja_la_decision_al_sp_ADS_004() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_004));

        final ConflictException error = assertThrows(ConflictException.class, () -> useCase.execute(domain(CORREO)));

        assertEquals(AdmisionEstudianteErrorCode.ADS_004.code(), error.getCode());
        verifyNoInteractions(cuentas);
    }

    @Test
    void ya_admitido_ADS_002_repara_la_cuenta_faltante_y_responde_con_la_persona_existente() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(cuentas.repararOFallar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.ACTIVA, true));

        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(domain(CORREO));

        assertEquals(ESTUDIANTE, resultado.idEstudiante());
        assertEquals(USUARIO, resultado.idUsuario());
        assertFalse(resultado.esUsuarioNuevo());
        assertTrue(resultado.cuentaReparada());
        assertEquals(EstadoCuentaAprovisionada.ACTIVA, resultado.estadoCuenta());
        verify(cuentas, never()).asegurar(any());
        verify(activacion, never()).avisar(any(), any(), any());
    }

    @Test
    void IDENT002_ya_admitido_con_cuenta_reparada_pendiente_recibe_el_aviso_de_activacion() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(cuentas.repararOFallar(any())).thenReturn(new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION, true));

        useCase.execute(domain(CORREO));

        final ArgumentCaptor<SolicitudCuentaIdentidad> solicitud = ArgumentCaptor.forClass(SolicitudCuentaIdentidad.class);
        verify(cuentas).repararOFallar(solicitud.capture());
        assertNull(solicitud.getValue().passwordInicial());
        assertTrue(solicitud.getValue().requiereVerificacionCorreo());
        verify(activacion).avisar(USUARIO, "Ana", InstitutionalRole.ESTUDIANTE);
    }

    @Test
    void ya_admitido_con_la_cuenta_existente_propaga_el_conflicto_del_servicio() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        final ConflictException conflicto = new ConflictException(AdmisionEstudianteErrorCode.ADS_002);
        when(cuentas.repararOFallar(any())).thenThrow(conflicto);

        assertSame(conflicto, assertThrows(ConflictException.class, () -> useCase.execute(domain(CORREO))));
    }

    @Test
    void ya_admitido_sin_poder_resolver_la_persona_es_un_error_interno() {
        when(usuarios.consultarUsuarioPorCorreo(any())).thenReturn(Optional.of(canonico()));
        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()), Optional.empty());
        when(comando.admitir(any())).thenThrow(new ConflictException(AdmisionEstudianteErrorCode.ADS_002));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(CORREO)));

        when(usuarios.consultarUsuarioPorIdentificacion(any(), any())).thenReturn(Optional.of(canonico()));
        when(ambito.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.empty());
        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(CORREO)));
    }

    @Test
    void un_conflicto_distinto_de_ADS_002_se_propaga_sin_reparar() {
        when(comando.admitir(any())).thenThrow(new ConflictException(UsuarioErrorCode.ERR_UNICIDAD_DOCUMENTO));

        assertThrows(ConflictException.class, () -> useCase.execute(domain(CORREO)));
        verifyNoInteractions(cuentas);
    }

    @Test
    void sin_la_identidad_canonica_despues_del_comando_es_un_error_interno() {
        when(comando.admitir(any())).thenReturn(new AdmisionEstudianteResultadoProjection(USUARIO, ESTUDIANTE, true, true));
        when(usuarios.consultarUsuarioPorId(USUARIO)).thenReturn(Optional.empty());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(domain(CORREO)));
        verifyNoInteractions(cuentas);
    }

    @Test
    void el_dominio_nulo_y_las_dependencias_nulas_se_rechazan() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        final CorreoInstitucionalPolicy politica = new CorreoInstitucionalPolicy(parametros, mensajes);
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(null, usuarios, politica, cuentas, ambito, activacion));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, null, politica, cuentas, ambito, activacion));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, null, cuentas, ambito, activacion));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, politica, null, ambito, activacion));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, politica, cuentas, null, activacion));
        assertThrows(NullPointerException.class, () -> new AdmitirEstudianteUseCaseImpl(comando, usuarios, politica, cuentas, ambito, null));
    }

    /** OBS-12: la credencial nunca aparece en la representacion de texto de la orden (logs, metadata). */
    @Test
    void la_credencial_no_aparece_en_to_string() {
        assertFalse(domain(CORREO).toString().contains(CORREO));
        assertFalse(new AdmisionEstudianteCommand(TIPO, 1, "Ana", null, "Perez", null, CORREO, HASH, PROGRAMA, ADMIN).toString().contains(HASH));
        assertNotNull(domain(CORREO).toString());
    }
}
