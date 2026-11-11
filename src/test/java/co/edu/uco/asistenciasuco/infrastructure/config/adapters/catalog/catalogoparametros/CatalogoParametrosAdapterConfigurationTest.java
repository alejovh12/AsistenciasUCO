package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros.CatalogoParametrosHttpParameterCatalogAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite.ParametroConValorPorDefectoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoParametrosAdapterConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CatalogoParametrosAdapterConfiguration.class);

    @Test
    void con_el_proveedor_activo_expone_el_delegado_del_tipo_del_adaptador() {
        runner.withPropertyValues(
                "app.adapters.parameter-catalog.provider=catalogoparametros",
                "app.providers.catalogo-parametros.base-url=http://localhost:8081/catalogo-parametros/api/v1/",
                "app.providers.catalogo-parametros.token-id=11111111-2222-3333-4444-555555555555",
                "app.providers.catalogo-parametros.secreto=valor-secreto-de-prueba",
                "app.providers.catalogo-parametros.application-id=",
                "app.providers.catalogo-parametros.environment-id="
        ).run(context -> {
            assertEquals(null, context.getStartupFailure());
            assertTrue(context.containsBean(ParametroConValorPorDefectoConfiguration.DELEGADO));
            assertInstanceOf(CatalogoParametrosHttpParameterCatalogAdapter.class,
                    context.getBean(ParametroConValorPorDefectoConfiguration.DELEGADO, ParameterCatalogPort.class));
        });
    }

    @Test
    void con_otro_proveedor_la_configuracion_no_aporta_el_bean_aunque_los_valores_esten_vacios() {
        runner.withPropertyValues(
                "app.adapters.parameter-catalog.provider=sqlserver",
                "app.providers.catalogo-parametros.token-id=",
                "app.providers.catalogo-parametros.secreto="
        ).run(context -> {
            assertEquals(null, context.getStartupFailure());
            assertFalse(context.containsBean(ParametroConValorPorDefectoConfiguration.DELEGADO));
        });
    }

    @Test
    void con_el_proveedor_activo_sin_secreto_el_contexto_falla() {
        runner.withPropertyValues(
                "app.adapters.parameter-catalog.provider=catalogoparametros",
                "app.providers.catalogo-parametros.base-url=http://localhost:8081/x",
                "app.providers.catalogo-parametros.token-id=11111111-2222-3333-4444-555555555555",
                "app.providers.catalogo-parametros.secreto="
        ).run(context -> assertNotNull(context.getStartupFailure()));
    }
}
