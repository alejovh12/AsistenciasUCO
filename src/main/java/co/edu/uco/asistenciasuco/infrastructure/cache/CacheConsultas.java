package co.edu.uco.asistenciasuco.infrastructure.cache;

import tools.jackson.core.type.TypeReference;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * REDIS-001: cache de lecturas con el patron <b>cache-aside</b> (lazy loading). Quien consulta pide el valor; si no esta,
 * lo carga de la fuente (BD), lo guarda con el TTL de la region y lo devuelve. Las escrituras siguen yendo a la BD (fuente
 * de verdad) y solo invalidan claves; la cache nunca es autoridad.
 *
 * <p>Contrato interno de Infrastructure: Application no lo conoce (los decoradores envuelven InputPorts desde el
 * Composition Root). Una falla de la cache nunca falla la consulta: se degrada a la fuente.</p>
 */
public interface CacheConsultas {

    /**
     * Devuelve el valor de {@code region}/{@code clave} o lo carga con {@code cargador}. Un valor {@code null} no se
     * guarda. {@code tipo} permite reconstruir el valor cuando la cache lo serializa (Redis).
     */
    <T> T obtener(RegionCache region, String clave, TypeReference<T> tipo, Supplier<T> cargador);

    /** Elimina una clave de la region (no falla si no existe ni si la cache no responde). */
    void invalidar(RegionCache region, String clave);

    /**
     * Asocia {@code claves} de la region a cada una de las {@code etiquetas} (por ejemplo {@code grupo:<id>}), para borrarlas
     * juntas con {@link #invalidarEtiqueta}. La asociacion vence con el TTL de la region; nunca falla la consulta.
     */
    void etiquetar(RegionCache region, Collection<String> etiquetas, Collection<String> claves);

    /** Elimina todas las claves asociadas a {@code etiqueta} en la region (y la propia etiqueta). Nunca falla. */
    void invalidarEtiqueta(RegionCache region, String etiqueta);

    /** Nombre del proveedor activo, para logs y diagnostico. */
    String proveedor();
}
