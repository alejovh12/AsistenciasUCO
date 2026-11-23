package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * INT-004: interpreta la respuesta de {@code GET /parametros/consulta/m2m} del Catalogo de Parametros.
 *
 * <p>La guia del equipo de Parametros solo dice que la respuesta trae {@code aplicacion}, {@code ambiente} y
 * {@code parametros}; no documenta cada campo. Por eso el mapeo es tolerante y acepta las dos formas conocidas de cada
 * elemento de {@code parametros}:</p>
 * <ul>
 *   <li>anidada (la del Catalogo desde 0.1.7, conservada): {@code {"parametro":{"nombre","activo"},"metadatos":[{"metadato":{"valor"}}]}};</li>
 *   <li>plana: {@code {"nombre","activo","valor"}} o {@code {"nombre","activo","metadatos":[{"valor"}]}}.</li>
 * </ul>
 * <p>De cada parametro activo se toma el primer valor no nulo; uno sin nombre, inactivo o sin valor se omite. Un valor que es un
 * objeto o arreglo JSON se entrega como su texto JSON. La raiz tambien puede ser directamente el arreglo.</p>
 */
final class CatalogoParametrosM2mMapper {

    private static final tools.jackson.databind.ObjectMapper JSON = JsonMapper.builder().build();

    private CatalogoParametrosM2mMapper() {
    }

    /** @return nombre completo ({@code GRUPO.CLAVE}) a valor, en el orden de la respuesta. */
    static Map<String, String> valoresPorNombre(final String body) {
        final JsonNode raiz;
        try {
            raiz = JSON.readTree(body);
        } catch (final RuntimeException exception) {
            throw new ParameterCatalogException("Respuesta invalida del Catalogo de Parametros.", exception);
        }
        final JsonNode parametros = raiz.isArray() ? raiz : raiz.path("parametros");
        final Map<String, String> resultado = new LinkedHashMap<>();
        if (!parametros.isArray()) {
            return resultado;
        }
        for (final JsonNode item : parametros) {
            final JsonNode parametro = item.path("parametro").isObject() ? item.path("parametro") : item;
            final String nombre = parametro.path("nombre").asString("").trim();
            if (nombre.isEmpty() || !parametro.path("activo").asBoolean(true)) {
                continue;
            }
            final String valor = primerValor(item, parametro);
            if (valor != null) {
                resultado.putIfAbsent(nombre, valor);
            }
        }
        return resultado;
    }

    private static String primerValor(final JsonNode item, final JsonNode parametro) {
        for (final JsonNode metadato : item.path("metadatos")) {
            final JsonNode valor = metadato.path("metadato").isObject() ? metadato.path("metadato").path("valor") : metadato.path("valor");
            final String texto = texto(valor);
            if (texto != null) {
                return texto;
            }
        }
        return texto(parametro.path("valor"));
    }

    private static String texto(final JsonNode valor) {
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        return valor.isValueNode() ? valor.asString() : valor.toString();
    }
}
