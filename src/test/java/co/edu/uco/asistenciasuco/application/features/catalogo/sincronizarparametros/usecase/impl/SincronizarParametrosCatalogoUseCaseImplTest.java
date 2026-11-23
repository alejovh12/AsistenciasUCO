package co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterValueRetentionPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort.AvisoCambioParametro;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.primaryports.SincronizarParametrosCatalogoInputPort.ResumenSincronizacion;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort.ParametroExterno;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort.Operacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort.ResultadoSincronizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SincronizarParametrosCatalogoUseCaseImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-22T12:00:00Z");
    private static final Instant EVENTO = Instant.parse("2026-11-22T11:59:00Z");

    private ParameterCatalogSourcePort fuente;
    private ParametroCatalogoRepositoryPort repositorio;
    private CatalogInvalidationPort invalidacion;
    private SincronizarParametrosCatalogoUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        fuente = mock(ParameterCatalogSourcePort.class);
        repositorio = mock(ParametroCatalogoRepositoryPort.class);
        invalidacion = mock(CatalogInvalidationPort.class);
        useCase = nuevo(null, null);
    }

    private SincronizarParametrosCatalogoUseCaseImpl nuevo(final String app, final String ambiente) {
        return new SincronizarParametrosCatalogoUseCaseImpl(fuente, repositorio, invalidacion, ".", app, ambiente,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static AvisoCambioParametro aviso(final String id, final String tipo, final String nombre) {
        return new AvisoCambioParametro(id, tipo, nombre, null, null, EVENTO);
    }

    private void repositorioResponde(final ResultadoSincronizacion resultado) {
        when(repositorio.sincronizarParametroExterno(anyString(), anyString(), any(), any(), any(), any())).thenReturn(resultado);
    }

    @Test
    void actualizado_relee_por_m2m_invalida_y_hace_upsert_con_el_id_y_la_hora_del_evento() {
        when(fuente.obtener("REGISTRO.DOMINIO")).thenReturn(Optional.of(new ParametroExterno("REGISTRO", "DOMINIO", "uco.edu.co")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        final ResumenSincronizacion resumen = useCase.procesarAviso(aviso("e1", "ParameterUpdated", "REGISTRO.DOMINIO"));

        assertEquals(new ResumenSincronizacion(1, 1, 0, 0, 0, 0, 0), resumen);
        verify(invalidacion).invalidateAll();
        verify(repositorio).sincronizarParametroExterno("REGISTRO", "DOMINIO", "uco.edu.co", Operacion.UPSERT, "e1", EVENTO);
    }

    @Test
    void creado_se_trata_como_actualizado() {
        when(fuente.obtener("G.K")).thenReturn(Optional.of(new ParametroExterno("G", "K", "v")));
        repositorioResponde(ResultadoSincronizacion.SIN_CAMBIOS);

        assertEquals(new ResumenSincronizacion(1, 0, 1, 0, 0, 0, 0), useCase.procesarAviso(aviso("e2", "ParameterCreated", "G.K")));
    }

    @Test
    void borrado_sin_valor_externo_hace_delete_partiendo_el_nombre_por_el_separador() {
        when(fuente.obtener("G.K.SUB")).thenReturn(Optional.empty());
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        final ResumenSincronizacion resumen = useCase.procesarAviso(aviso("e3", "ParameterDeleted", "G.K.SUB"));

        assertEquals(1, resumen.aplicados());
        verify(repositorio).sincronizarParametroExterno("G", "K.SUB", null, Operacion.DELETE, "e3", EVENTO);
    }

    @Test
    void borrado_con_valor_vigente_hace_upsert_porque_el_catalogo_manda() {
        when(fuente.obtener("G.K")).thenReturn(Optional.of(new ParametroExterno("G", "K", "otra-vez")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        useCase.procesarAviso(aviso("e4", "ParameterDeleted", "G.K"));

        verify(repositorio).sincronizarParametroExterno("G", "K", "otra-vez", Operacion.UPSERT, "e4", EVENTO);
    }

    @Test
    void actualizado_que_el_catalogo_ya_no_devuelve_no_borra_solo_invalida() {
        when(fuente.obtener("G.K")).thenReturn(Optional.empty());

        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e5", "ParameterUpdated", "G.K")));

        verify(invalidacion).invalidateAll();
        verifyNoInteractions(repositorio);
    }

    @Test
    void nombre_sin_separador_se_ignora_sin_consultar() {
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e6", "ParameterUpdated", "SINSEPARADOR")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e7", "ParameterUpdated", ".CLAVE")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e8", "ParameterUpdated", "GRUPO.")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e9", "ParameterUpdated", " ")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e10", "ParameterUpdated", null)));
        verifyNoInteractions(fuente, repositorio);
    }

    @Test
    void metadato_actualizado_relee_por_nombre_y_hace_upsert_como_el_evento_real_del_catalogo() {
        when(fuente.obtener("RECLAMO.ESTADO_RETIRADO")).thenReturn(Optional.of(new ParametroExterno("RECLAMO", "ESTADO_RETIRADO", "X-int004")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        final ResumenSincronizacion resumen = useCase.procesarAviso(
                aviso("355f30cf", "MetadataUpdated", "RECLAMO.ESTADO_RETIRADO"));

        assertEquals(1, resumen.aplicados());
        verify(invalidacion).invalidateAll();
        verify(fuente, never()).listar();
        verify(repositorio).sincronizarParametroExterno("RECLAMO", "ESTADO_RETIRADO", "X-int004", Operacion.UPSERT, "355f30cf", EVENTO);
    }

    @Test
    void todos_los_tipos_de_metadato_y_metadato_por_ambiente_relee_el_valor() {
        when(fuente.obtener("G.K")).thenReturn(Optional.of(new ParametroExterno("G", "K", "v")));
        repositorioResponde(ResultadoSincronizacion.SIN_CAMBIOS);
        int n = 0;
        for (final String tipo : List.of("MetadataCreated", "MetadataUpdated", "MetadataDeleted", "MetadataEnvironmentCreated",
                "MetadataEnvironmentUpdated", "MetadataEnvironmentDeleted")) {
            assertEquals(1, useCase.procesarAviso(aviso("m" + n++, tipo, "G.K")).sinCambios(), tipo);
        }
    }

    @Test
    void metadato_borrado_cuyo_valor_desaparece_conserva_el_valor_local() {
        when(fuente.obtener("G.K")).thenReturn(Optional.empty());

        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("md", "MetadataDeleted", "G.K")));

        verify(invalidacion).invalidateAll();
        verifyNoInteractions(repositorio);
    }

    @Test
    void metadato_sin_nombre_usable_reconcilia_todo_y_reintenta_si_la_bd_falla() {
        when(fuente.listar()).thenReturn(List.of(new ParametroExterno("A", "B", "1"), new ParametroExterno("C", "D", "2")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        assertEquals(new ResumenSincronizacion(2, 2, 0, 0, 0, 0, 0), useCase.procesarAviso(aviso("e11", "MetadataEnvironmentUpdated", null)));

        when(repositorio.sincronizarParametroExterno(anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("bd caida"));
        assertThrows(IllegalStateException.class, () -> useCase.procesarAviso(aviso("e12", "MetadataCreated", " ")));
        assertThrows(IllegalStateException.class, () -> useCase.procesarAviso(aviso("e12", "MetadataCreated", " ")));
        verify(fuente, times(3)).listar();
    }

    @Test
    void tipo_desconocido_o_vacio_se_ignora() {
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e13", "AlgoRaro", "G.K")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e13b", "ParameterRenamed", "G.K")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e13c", "MetadataEnvironmentBorrado", "G.K")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("e14", " ", "G.K")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(null));
        verifyNoInteractions(fuente, repositorio);
    }

    @Test
    void evento_repetido_se_ignora_solo_si_el_primero_se_proceso() {
        when(fuente.obtener("G.K")).thenReturn(Optional.of(new ParametroExterno("G", "K", "v")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        useCase.procesarAviso(aviso("rep", "ParameterUpdated", "G.K"));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(aviso("rep", "ParameterUpdated", "G.K")));

        verify(repositorio, times(1)).sincronizarParametroExterno(anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    void falla_de_la_fuente_o_de_la_bd_propaga_y_no_marca_procesado() {
        when(fuente.obtener("G.K")).thenThrow(new ParameterCatalogException("caido"))
                .thenReturn(Optional.of(new ParametroExterno("G", "K", "v")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        assertThrows(ParameterCatalogException.class, () -> useCase.procesarAviso(aviso("r1", "ParameterUpdated", "G.K")));
        assertEquals(1, useCase.procesarAviso(aviso("r1", "ParameterUpdated", "G.K")).aplicados());
    }

    @Test
    void filtra_por_aplicacion_y_ambiente_cuando_estan_configurados() {
        final var filtrado = nuevo("app-1", "amb-1");
        when(fuente.obtener("G.K")).thenReturn(Optional.of(new ParametroExterno("G", "K", "v")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        assertEquals(ResumenSincronizacion.vacio(), filtrado.procesarAviso(
                new AvisoCambioParametro("f1", "ParameterUpdated", "G.K", "otra-app", "amb-1", EVENTO)));
        assertEquals(ResumenSincronizacion.vacio(), filtrado.procesarAviso(
                new AvisoCambioParametro("f2", "ParameterUpdated", "G.K", "APP-1", "otro-amb", EVENTO)));
        verifyNoInteractions(repositorio);

        assertEquals(1, filtrado.procesarAviso(new AvisoCambioParametro("f3", "ParameterUpdated", "G.K", "APP-1", "AMB-1", EVENTO)).aplicados());
        // Un evento sin ids no se descarta: el topic ya es de la identidad.
        assertEquals(1, filtrado.procesarAviso(new AvisoCambioParametro("f4", "ParameterUpdated", "G.K", null, " ", EVENTO)).aplicados());
    }

    @Test
    void sin_hora_en_el_evento_usa_la_de_recepcion() {
        when(fuente.obtener("G.K")).thenReturn(Optional.of(new ParametroExterno("G", "K", "v")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);

        useCase.procesarAviso(new AvisoCambioParametro("h1", "ParameterUpdated", "G.K", null, null, null));

        verify(repositorio).sincronizarParametroExterno("G", "K", "v", Operacion.UPSERT, "h1", AHORA);
    }

    @Test
    void reconciliacion_cuenta_cada_resultado_y_tolera_fallos_individuales() {
        when(fuente.listar()).thenReturn(List.of(
                new ParametroExterno("A", "1", "x"), new ParametroExterno("A", "2", "x"), new ParametroExterno("A", "3", "x"),
                new ParametroExterno("A", "4", "x"), new ParametroExterno("A", "5", "x"), new ParametroExterno("A", "6", "x"),
                new ParametroExterno("A", "7", "x"), new ParametroExterno("A", "8", "x")));
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("1"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.APLICADO);
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("2"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.SIN_CAMBIOS);
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("3"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.DESCONOCIDO);
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("4"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.OBSOLETO);
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("5"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.RECHAZADO);
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("6"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.DUPLICADO);
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("7"), any(), any(), isNull(), any())).thenThrow(new IllegalStateException("x"));
        when(repositorio.sincronizarParametroExterno(eq("A"), eq("8"), any(), any(), isNull(), any())).thenReturn(ResultadoSincronizacion.APLICADO);

        assertEquals(new ResumenSincronizacion(8, 2, 2, 1, 1, 1, 1), useCase.reconciliarTodo());
        verify(invalidacion).invalidateAll();
    }

    @Test
    void reconciliacion_con_el_catalogo_caido_propaga() {
        when(fuente.listar()).thenThrow(new ParameterCatalogException("caido"));

        assertThrows(ParameterCatalogException.class, () -> useCase.reconciliarTodo());
        verify(repositorio, never()).sincronizarParametroExterno(anyString(), anyString(), any(), any(), any(), any());
    }

    @Test
    void recuerda_un_numero_acotado_de_eventos() {
        when(fuente.obtener(anyString())).thenReturn(Optional.empty());
        for (int i = 0; i < SincronizarParametrosCatalogoUseCaseImpl.MAX_AVISOS_RECORDADOS + 5; i++) {
            useCase.procesarAviso(aviso("n" + i, "ParameterUpdated", "G.K"));
        }
        // El primero ya salio de la memoria: se vuelve a consultar; el ultimo sigue recordado.
        useCase.procesarAviso(aviso("n0", "ParameterUpdated", "G.K"));
        final int ultimo = SincronizarParametrosCatalogoUseCaseImpl.MAX_AVISOS_RECORDADOS + 4;
        useCase.procesarAviso(aviso("n" + ultimo, "ParameterUpdated", "G.K"));

        verify(fuente, times(SincronizarParametrosCatalogoUseCaseImpl.MAX_AVISOS_RECORDADOS + 6)).obtener("G.K");
    }

    private SincronizarParametrosCatalogoUseCaseImpl conRetencion(final ParameterValueRetentionPort retencion) {
        return new SincronizarParametrosCatalogoUseCaseImpl(fuente, repositorio, invalidacion, ".", null, null,
                Clock.fixed(AHORA, ZoneOffset.UTC), retencion);
    }

    @Test
    void un_valor_rechazado_por_la_bd_retiene_el_ultimo_valor_valido_del_evento() {
        final var retencion = mock(ParameterValueRetentionPort.class);
        when(fuente.obtener("RED.CIDRS_CAMPUS")).thenReturn(Optional.of(new ParametroExterno("RED", "CIDRS_CAMPUS", "no-es-un-cidr")));
        repositorioResponde(ResultadoSincronizacion.RECHAZADO);
        when(repositorio.obtenerValor("RED", "CIDRS_CAMPUS")).thenReturn(Optional.of("10.0.0.0/8"));

        final var resumen = conRetencion(retencion).procesarAviso(aviso("rj1", "ParameterUpdated", "RED.CIDRS_CAMPUS"));

        assertEquals(new ResumenSincronizacion(1, 0, 0, 0, 0, 1, 0), resumen);
        verify(retencion).retenerValorValido("RED", "CIDRS_CAMPUS", "no-es-un-cidr", "10.0.0.0/8");
        verify(retencion, never()).liberar(anyString(), anyString());
    }

    @Test
    void un_rechazo_sin_valor_en_la_bd_retiene_sin_valor_vigente() {
        final var retencion = mock(ParameterValueRetentionPort.class);
        when(fuente.obtener("RED.CIDRS_CAMPUS")).thenReturn(Optional.of(new ParametroExterno("RED", "CIDRS_CAMPUS", "x")));
        repositorioResponde(ResultadoSincronizacion.RECHAZADO);
        when(repositorio.obtenerValor("RED", "CIDRS_CAMPUS")).thenReturn(Optional.empty());

        conRetencion(retencion).procesarAviso(aviso("rj2", "ParameterUpdated", "RED.CIDRS_CAMPUS"));

        verify(retencion).retenerValorValido("RED", "CIDRS_CAMPUS", "x", null);
    }

    @Test
    void un_valor_aplicado_o_sin_cambios_libera_la_retencion() {
        final var retencion = mock(ParameterValueRetentionPort.class);
        when(fuente.obtener("RED.CIDRS_CAMPUS")).thenReturn(Optional.of(new ParametroExterno("RED", "CIDRS_CAMPUS", "10.0.0.0/8")));
        repositorioResponde(ResultadoSincronizacion.APLICADO);
        conRetencion(retencion).procesarAviso(aviso("ok1", "ParameterUpdated", "RED.CIDRS_CAMPUS"));
        repositorioResponde(ResultadoSincronizacion.SIN_CAMBIOS);
        conRetencion(retencion).procesarAviso(aviso("ok2", "ParameterUpdated", "RED.CIDRS_CAMPUS"));
        repositorioResponde(ResultadoSincronizacion.OBSOLETO);
        conRetencion(retencion).procesarAviso(aviso("ok3", "ParameterUpdated", "RED.CIDRS_CAMPUS"));

        verify(retencion, times(2)).liberar("RED", "CIDRS_CAMPUS");
        verify(retencion, never()).retenerValorValido(anyString(), anyString(), any(), any());
    }

    @Test
    void la_reconciliacion_tambien_retiene_el_valor_valido_de_lo_rechazado() {
        final var retencion = mock(ParameterValueRetentionPort.class);
        when(fuente.listar()).thenReturn(List.of(new ParametroExterno("RED", "CIDRS_CAMPUS", "no-es-un-cidr")));
        repositorioResponde(ResultadoSincronizacion.RECHAZADO);
        when(repositorio.obtenerValor("RED", "CIDRS_CAMPUS")).thenReturn(Optional.of("127.0.0.1/32"));

        assertEquals(new ResumenSincronizacion(1, 0, 0, 0, 0, 1, 0), conRetencion(retencion).reconciliarTodo());

        verify(retencion).retenerValorValido("RED", "CIDRS_CAMPUS", "no-es-un-cidr", "127.0.0.1/32");
    }
}
