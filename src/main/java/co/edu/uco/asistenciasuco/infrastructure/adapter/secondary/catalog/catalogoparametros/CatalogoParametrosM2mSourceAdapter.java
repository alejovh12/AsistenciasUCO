package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * INT-004: {@link ParameterCatalogSourcePort} sobre la consulta M2M del Catalogo de Parametros. A diferencia del proveedor con cache
 * ({@link CatalogoParametrosHttpParameterCatalogAdapter}), cada llamada consulta al componente: confirma el valor vigente despues de
 * un evento de Pulsar o durante la reconciliacion. Parte el nombre {@code GRUPO.CLAVE} por la primera aparicion del separador; un
 * nombre sin esa forma, o sin valor (vacio), no se devuelve: no hay nada que copiar y la BD conserva su valor local.
 */
public final class CatalogoParametrosM2mSourceAdapter implements ParameterCatalogSourcePort {

    private final CatalogoParametrosM2mClient cliente;
    private final String separador;

    public CatalogoParametrosM2mSourceAdapter(final HttpClient httpClient, final CatalogoParametrosProviderProperties properties,
                                              final CatalogoParametrosTokenProvider tokenProvider) {
        Objects.requireNonNull(properties, "Las propiedades del catalogo son obligatorias.");
        this.cliente = new CatalogoParametrosM2mClient(httpClient, properties, tokenProvider);
        this.separador = properties.separador();
    }

    @Override
    public List<ParametroExterno> listar() {
        return convertir(cliente.interpretar(cliente.enviar(null)));
    }

    /**
     * La consulta por nombre puede traer coincidencias parciales: se conserva solo la de nombre exacto (sin distinguir
     * mayusculas), para no aplicar el valor de otro parametro.
     */
    @Override
    public Optional<ParametroExterno> obtener(final String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        final String buscado = nombre.trim();
        final Map<String, String> respuesta = cliente.interpretar(cliente.enviar(buscado));
        return respuesta.entrySet().stream()
                .filter(entrada -> entrada.getKey().equalsIgnoreCase(buscado))
                .findFirst()
                .flatMap(entrada -> dividir(entrada.getKey(), entrada.getValue()));
    }

    private List<ParametroExterno> convertir(final Map<String, String> valores) {
        final List<ParametroExterno> parametros = new ArrayList<>();
        valores.forEach((nombre, valor) -> dividir(nombre, valor).ifPresent(parametros::add));
        return List.copyOf(parametros);
    }

    private Optional<ParametroExterno> dividir(final String nombre, final String valor) {
        final int corte = nombre.indexOf(separador);
        if (valor == null || valor.isBlank() || corte <= 0 || corte + separador.length() >= nombre.length()) {
            return Optional.empty();
        }
        return Optional.of(new ParametroExterno(nombre.substring(0, corte).trim(),
                nombre.substring(corte + separador.length()).trim(), valor));
    }
}
