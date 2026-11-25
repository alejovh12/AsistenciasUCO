package co.edu.uco.asistenciasuco.infrastructure.cache;

import tools.jackson.core.type.TypeReference;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/** REDIS-001: proveedor {@code none}: cada consulta va a la fuente (comportamiento previo a la cache). */
public final class SinCacheConsultas implements CacheConsultas {

    @Override
    public <T> T obtener(final RegionCache region, final String clave, final TypeReference<T> tipo, final Supplier<T> cargador) {
        return Objects.requireNonNull(cargador, "El cargador es obligatorio.").get();
    }

    @Override
    public void invalidar(final RegionCache region, final String clave) {
        // Sin cache no hay nada que invalidar.
    }

    @Override
    public void etiquetar(final RegionCache region, final Collection<String> etiquetas, final Collection<String> claves) {
        // Sin cache no hay claves que asociar.
    }

    @Override
    public void invalidarEtiqueta(final RegionCache region, final String etiqueta) {
        // Sin cache no hay nada que invalidar.
    }

    @Override
    public String proveedor() {
        return "none";
    }
}
