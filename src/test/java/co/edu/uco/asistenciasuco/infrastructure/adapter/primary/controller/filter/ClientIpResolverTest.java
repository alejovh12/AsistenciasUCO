package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** M-08: la IP de la bitacora respeta {@code X-Forwarded-For} solo desde proxies confiables. */
class ClientIpResolverTest {

    private static MockHttpServletRequest request(final String remoto, final String reenviado) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoto);
        if (reenviado != null) {
            request.addHeader("X-Forwarded-For", reenviado);
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    private static ClientIpResolver conProxies(final String proxies) {
        final ObjectProvider<NetworkClientIpResolver> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(new NetworkClientIpResolver(() -> Optional.of(proxies)));
        return new ClientIpResolver(proveedor);
    }

    @Test
    void sin_resolvedor_de_red_usa_la_direccion_remota_como_antes() {
        assertEquals("10.0.0.9", new ClientIpResolver().resolve(request("10.0.0.9", "1.2.3.4")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sin_resolvedor_disponible_en_el_contexto_usa_la_direccion_remota() {
        final ObjectProvider<NetworkClientIpResolver> vacio = mock(ObjectProvider.class);
        when(vacio.getIfAvailable()).thenReturn(null);

        assertEquals("10.0.0.9", new ClientIpResolver(vacio).resolve(request("10.0.0.9", "1.2.3.4")));
    }

    @Test
    void sin_proxies_configurados_ignora_el_encabezado_reenviado() {
        assertEquals("10.0.0.9", conProxies("").resolve(request("10.0.0.9", "1.2.3.4")));
    }

    @Test
    void desde_un_proxy_confiable_registra_la_ip_del_cliente() {
        assertEquals("203.0.113.7", conProxies("10.0.0.0/24").resolve(request("10.0.0.9", "203.0.113.7")));
    }

    @Test
    void desde_un_origen_no_confiable_el_encabezado_falsificado_no_cuenta() {
        assertEquals("198.51.100.4", conProxies("10.0.0.0/24").resolve(request("198.51.100.4", "203.0.113.7")));
    }
}
