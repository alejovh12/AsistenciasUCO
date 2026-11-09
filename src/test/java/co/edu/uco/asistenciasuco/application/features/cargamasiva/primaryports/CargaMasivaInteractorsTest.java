package co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EjecutarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ValidarCargaUsuariosCommand;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.interactor.EjecutarCargaUsuariosInteractor;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.interactor.ValidarCargaUsuariosInteractor;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.EjecutarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.ValidarCargaUsuariosUseCase;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EjecutarCargaUsuariosDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaResultado;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.EstadoFilaValidacion;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ResultadoValidacionCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ValidarCargaUsuariosDomain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 D.5: interactores de la carga masiva (mapeo dominio -> DTO) y records con arreglos de bytes (igualdad por contenido). */
class CargaMasivaInteractorsTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final byte[] CONTENIDO = {1, 2, 3};

    @Test
    void ejecutar_traduce_el_comando_al_dominio_y_el_resultado_al_dto_con_sus_errores() {
        final EjecutarCargaUsuariosUseCase useCase = mock(EjecutarCargaUsuariosUseCase.class);
        final UUID creado = UUID.randomUUID();
        when(useCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(new ResultadoCargaDomain(2, 1, 0, 1, 0, List.of(
                new FilaResultadoCargaDomain(2, "1017", "a@uco.edu.co", EstadoFilaResultado.CREADA, creado, List.of()),
                new FilaResultadoCargaDomain(3, "1018", "b@uco.edu.co", EstadoFilaResultado.ERROR, null,
                        List.of(new ErrorFilaCargaDomain("correo", "CMA_003", "Correo invalido")))), "Carga procesada."));
        final EjecutarCargaUsuariosInteractor interactor = new EjecutarCargaUsuariosInteractor(useCase);

        final ResultadoCargaDTO resultado = interactor.ejecutar(new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, "d.csv", CONTENIDO, "huella"));

        final ArgumentCaptor<EjecutarCargaUsuariosDomain> domain = ArgumentCaptor.forClass(EjecutarCargaUsuariosDomain.class);
        verify(useCase).execute(domain.capture());
        assertEquals(ACTOR, domain.getValue().usuarioEjecutor());
        assertEquals(PerfilDestinoCarga.DOCENTE, domain.getValue().perfilDestino());
        assertArrayEquals(CONTENIDO, domain.getValue().contenidoBytes());
        assertEquals("huella", domain.getValue().huella());
        assertEquals(2, resultado.totalFilas());
        assertEquals("Carga procesada.", resultado.mensaje());
        assertEquals(creado, resultado.filas().getFirst().idUsuario());
        assertEquals("1017", resultado.filas().getFirst().documento());
        assertEquals(1, resultado.filas().get(1).errores().size());
        assertEquals("CMA_003", resultado.filas().get(1).errores().getFirst().codigo());
    }

    @Test
    void validar_traduce_el_comando_al_dominio_y_el_resultado_al_dto_con_sus_errores() {
        final ValidarCargaUsuariosUseCase useCase = mock(ValidarCargaUsuariosUseCase.class);
        when(useCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(new ResultadoValidacionCargaDomain("h", PerfilDestinoCarga.COORDINADOR, 2, 1, 0, 1, List.of(
                new FilaValidacionCargaDomain(2, "1017", "a@uco.edu.co", EstadoFilaValidacion.VALIDA, List.of()),
                new FilaValidacionCargaDomain(3, "1018", "b@uco.edu.co", EstadoFilaValidacion.ERROR,
                        List.of(new ErrorFilaCargaDomain("documento", "CMA_004", "Documento invalido"))))));
        final ValidarCargaUsuariosInteractor interactor = new ValidarCargaUsuariosInteractor(useCase);

        final ResultadoValidacionCargaDTO resultado = interactor.validar(new ValidarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.COORDINADOR, "c.csv", CONTENIDO));

        final ArgumentCaptor<ValidarCargaUsuariosDomain> domain = ArgumentCaptor.forClass(ValidarCargaUsuariosDomain.class);
        verify(useCase).execute(domain.capture());
        assertEquals(ACTOR, domain.getValue().usuarioEjecutor());
        assertEquals(PerfilDestinoCarga.COORDINADOR, domain.getValue().perfilDestino());
        assertArrayEquals(CONTENIDO, domain.getValue().contenidoBytes());
        assertEquals("h", resultado.huella());
        assertEquals(1, resultado.filasValidas());
        assertEquals(1, resultado.filasConError());
        assertEquals(EstadoFilaValidacion.ERROR, resultado.filas().get(1).estado());
        assertEquals("CMA_004", resultado.filas().get(1).errores().getFirst().codigo());
    }

    @Test
    void los_interactores_exigen_su_caso_de_uso_y_su_comando() {
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosInteractor(null));
        assertThrows(NullPointerException.class, () -> new ValidarCargaUsuariosInteractor(null));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosInteractor(mock(EjecutarCargaUsuariosUseCase.class)).ejecutar(null));
        assertThrows(NullPointerException.class, () -> new ValidarCargaUsuariosInteractor(mock(ValidarCargaUsuariosUseCase.class)).validar(null));
    }

    // ---- records con arreglos: igualdad, hash y toString por contenido (java:S6218) ----

    @Test
    void ejecutar_command_compara_por_contenido_copia_defensivamente_y_no_expone_la_huella() {
        final byte[] origen = {1, 2, 3};
        final EjecutarCargaUsuariosCommand a = new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, "d.csv", origen, "huella-secreta");
        final EjecutarCargaUsuariosCommand b = new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, "d.csv", new byte[]{1, 2, 3}, "huella-secreta");
        origen[0] = 9;

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertArrayEquals(new byte[]{1, 2, 3}, a.contenidoBytes());
        a.contenidoBytes()[0] = 7;
        assertArrayEquals(new byte[]{1, 2, 3}, a.contenidoBytes());
        assertNotEquals(a, new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, "d.csv", new byte[]{1, 2, 4}, "huella-secreta"));
        assertNotEquals(a, new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, "d.csv", new byte[]{1, 2, 3}, "otra"));
        assertNotEquals(a, new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.COORDINADOR, "d.csv", new byte[]{1, 2, 3}, "huella-secreta"));
        assertFalse(a.equals("otro tipo")); // Sonar S5845: comparar con otro tipo se expresa con equals directo
        assertTrue(a.equals(a));
        assertTrue(a.toString().contains("3 bytes"));
        assertFalse(a.toString().contains("huella-secreta"));
        assertEquals(0, new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, null, null, "h").contenidoBytes().length);
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosCommand(null, PerfilDestinoCarga.DOCENTE, "d", CONTENIDO, "h"));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosCommand(ACTOR, null, "d", CONTENIDO, "h"));
        assertThrows(NullPointerException.class, () -> new EjecutarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DOCENTE, "d", CONTENIDO, null));
    }

    @Test
    void validar_command_compara_por_contenido_y_copia_defensivamente() {
        final ValidarCargaUsuariosCommand a = new ValidarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DECANO, "d.csv", new byte[]{5, 6});
        final ValidarCargaUsuariosCommand b = new ValidarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DECANO, "d.csv", new byte[]{5, 6});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new ValidarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DECANO, "d.csv", new byte[]{5, 7}));
        assertNotEquals(a, new ValidarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DECANO, "otro.csv", new byte[]{5, 6}));
        assertNotEquals(a, null);
        assertFalse(a.equals("otro tipo")); // Sonar S5845: comparar con otro tipo se expresa con equals directo
        assertTrue(a.toString().contains("2 bytes"));
        a.contenidoBytes()[0] = 0;
        assertArrayEquals(new byte[]{5, 6}, a.contenidoBytes());
        assertEquals(0, new ValidarCargaUsuariosCommand(ACTOR, PerfilDestinoCarga.DECANO, null, null).contenidoBytes().length);
        assertThrows(NullPointerException.class, () -> new ValidarCargaUsuariosCommand(null, PerfilDestinoCarga.DECANO, "d", CONTENIDO));
        assertThrows(NullPointerException.class, () -> new ValidarCargaUsuariosCommand(ACTOR, null, "d", CONTENIDO));
    }

    @Test
    void los_dominios_comparan_por_contenido_del_arreglo() {
        final EjecutarCargaUsuariosDomain a = new EjecutarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 2}, "h");
        final EjecutarCargaUsuariosDomain b = new EjecutarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 2}, "h");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new EjecutarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 3}, "h"));
        assertNotEquals(a, new EjecutarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 2}, "x"));
        assertFalse(a.equals("otro tipo")); // Sonar S5845: comparar con otro tipo se expresa con equals directo
        assertTrue(a.equals(a));
        assertFalse(a.toString().contains("\"h\""));
        assertTrue(a.toString().contains("2 bytes"));
        assertTrue(new EjecutarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, null, "h").toString().contains("0 bytes"));

        final ValidarCargaUsuariosDomain v = new ValidarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 2});
        assertEquals(v, new ValidarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 2}));
        assertEquals(v.hashCode(), new ValidarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{1, 2}).hashCode());
        assertNotEquals(v, new ValidarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, new byte[]{2, 2}));
        assertNotEquals(v, new ValidarCargaUsuariosDomain(UUID.randomUUID(), PerfilDestinoCarga.DOCENTE, new byte[]{1, 2}));
        assertFalse(v.equals("otro tipo")); // Sonar S5845: comparar con otro tipo se expresa con equals directo
        assertTrue(v.equals(v));
        assertTrue(v.toString().contains("2 bytes"));
        assertTrue(new ValidarCargaUsuariosDomain(ACTOR, PerfilDestinoCarga.DOCENTE, null).toString().contains("0 bytes"));
    }
}
