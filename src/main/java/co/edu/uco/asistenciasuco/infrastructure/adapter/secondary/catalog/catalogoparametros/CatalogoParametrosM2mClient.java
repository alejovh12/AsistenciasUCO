package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * INT-004: cliente HTTP de la consulta M2M del Catalogo de Parametros ({@code GET {baseUrl}/parametros/consulta/m2m}, con
 * {@code ?nombre=} opcional). El Bearer es el token de la identidad de servicio; el Catalogo resuelve aplicacion y ambiente desde
 * el {@code client_id} del JWT, asi que nunca se envian. Ni el token ni el secreto viajan en mensajes de error.
 *
 * <p>Un 401 descarta el token guardado: el siguiente pedido pide uno nuevo a Keycloak (en la lectura sincrona se reintenta una
 * vez con token nuevo, porque el token pudo revocarse antes de vencer).</p>
 */
final class CatalogoParametrosM2mClient {

    static final String RUTA = "/parametros/consulta/m2m";

    private final HttpClient httpClient;
    private final CatalogoParametrosProviderProperties properties;
    private final CatalogoParametrosTokenProvider tokenProvider;

    CatalogoParametrosM2mClient(final HttpClient httpClient, final CatalogoParametrosProviderProperties properties,
                                final CatalogoParametrosTokenProvider tokenProvider) {
        this.httpClient = Objects.requireNonNull(httpClient, "El HttpClient es obligatorio.");
        this.properties = Objects.requireNonNull(properties, "Las propiedades del catalogo son obligatorias.");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "El proveedor de token es obligatorio.");
    }

    URI uri(final String nombre) {
        final String base = properties.baseUrl() + RUTA;
        return URI.create(nombre == null ? base : base + "?nombre=" + URLEncoder.encode(nombre, StandardCharsets.UTF_8));
    }

    /** Peticion con el token vigente (si no hay uno guardado, lo pide: puede esperar a Keycloak). */
    HttpRequest peticion(final String nombre) {
        return peticion(nombre, tokenProvider.token());
    }

    HttpRequest peticion(final String nombre, final String token) {
        return HttpRequest.newBuilder(uri(nombre))
                .timeout(properties.timeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    /** Envio sincrono; ante un 401 descarta el token y reintenta una sola vez con uno nuevo. */
    HttpResponse<String> enviar(final String nombre) {
        final HttpResponse<String> primera = enviarUna(nombre);
        if (primera.statusCode() != 401) {
            return primera;
        }
        tokenProvider.invalidar();
        return enviarUna(nombre);
    }

    private HttpResponse<String> enviarUna(final String nombre) {
        try {
            return httpClient.send(peticion(nombre), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            throw new ParameterCatalogException("No fue posible consultar el Catalogo de Parametros.", exception);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ParameterCatalogException("Consulta al Catalogo de Parametros interrumpida.", exception);
        }
    }

    /** Valida el estado HTTP (un 401 tambien descarta el token) y devuelve nombre completo a valor. */
    Map<String, String> interpretar(final HttpResponse<String> response) {
        final int status = response.statusCode();
        if (status == 401) {
            tokenProvider.invalidar();
        }
        if (status < 200 || status >= 300) {
            throw new ParameterCatalogException("El Catalogo de Parametros respondio HTTP " + status + ".");
        }
        return java.util.Collections.unmodifiableMap(CatalogoParametrosM2mMapper.valoresPorNombre(response.body()));
    }
}
