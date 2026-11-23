package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import java.util.Optional;

/**
 * INT-004: access token de la identidad de servicio con que el backend consulta el Catalogo de Parametros por M2M.
 */
public interface CatalogoParametrosTokenProvider {

    /**
     * Token vigente; si no hay uno guardado lo pide a Keycloak (bloquea hasta el timeout de la peticion).
     *
     * @throws co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException si
     *         Keycloak no entrega token.
     */
    String token();

    /** Token guardado si todavia no vence; nunca llama a Keycloak (lo usan las lecturas que no deben esperar). */
    Optional<String> tokenEnCache();

    /** Descarta el token guardado (el Catalogo respondio 401: el siguiente pedido pide uno nuevo). */
    void invalidar();
}
