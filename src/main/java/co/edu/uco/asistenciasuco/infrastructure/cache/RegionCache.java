package co.edu.uco.asistenciasuco.infrastructure.cache;

/**
 * REDIS-001: regiones de la cache de consultas. Cada region agrupa lecturas con la misma tolerancia a datos viejos; el
 * vencimiento (TTL) de cada una se configura en {@code app.adapters.cache.ttl.*}.
 *
 * <ul>
 *   <li>{@link #CATALOGOS}: catalogos de solo consulta (se cargan por semilla; regla del proyecto). Cambian solo con una
 *       migracion, por eso toleran un TTL largo.</li>
 *   <li>{@link #INDICADORES}: KPIs y comparativas de coordinador, decano y administrador. Son agregados costosos que se
 *       consultan muchas veces y no exigen precision al segundo.</li>
 *   <li>{@link #PORTAL}: tablero personal del estudiante y del docente (materias, horarios, resumen, plan). La clave
 *       siempre incluye al usuario; un TTL corto acota lo viejo y el registro por QR invalida las del estudiante.</li>
 * </ul>
 */
public enum RegionCache {
    CATALOGOS("catalogos"),
    INDICADORES("indicadores"),
    PORTAL("portal");

    private final String nombre;

    RegionCache(final String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
