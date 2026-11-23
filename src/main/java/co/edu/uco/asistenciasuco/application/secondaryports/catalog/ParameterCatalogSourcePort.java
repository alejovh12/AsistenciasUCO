package co.edu.uco.asistenciasuco.application.secondaryports.catalog;

import java.util.List;
import java.util.Optional;

/**
 * INT-004: fuente externa de los valores del catalogo de parametros (hoy el Catalogo de Parametros por M2M), leida para
 * reflejarlos en {@code dbo.CatalogoParametro}.
 *
 * <p>Trabaja con la clave canonica de Asistencias (grupo y clave): partir el nombre {@code GRUPO.CLAVE} del componente externo
 * es un detalle del adaptador. Un parametro externo cuyo nombre no tiene esa forma no se devuelve.</p>
 *
 * <p>Distingue "no existe" (respuesta vacia) de "no se pudo leer" ({@link ParameterCatalogPort.ParameterCatalogException}): un
 * fallo transitorio (token, red, caida) no debe confundirse con un parametro borrado, porque el evento se daria por procesado y
 * la BD quedaria con un valor ajeno al vigente.</p>
 */
public interface ParameterCatalogSourcePort {

    /**
     * Lee todos los parametros activos del alcance (aplicacion y ambiente) de la identidad de servicio.
     *
     * @throws ParameterCatalogPort.ParameterCatalogException si el componente no respondio (se debe reintentar).
     */
    List<ParametroExterno> listar();

    /**
     * Relee un parametro por su nombre completo (coincidencia exacta sin distinguir mayusculas). Siempre consulta al componente:
     * no usa la cache en memoria, porque confirma el valor despues de un evento.
     *
     * @return el parametro con su valor vigente; vacio si el componente no lo devuelve (borrado, inactivo o ajeno).
     * @throws ParameterCatalogPort.ParameterCatalogException si el componente no respondio (se debe reintentar).
     */
    Optional<ParametroExterno> obtener(String nombre);

    /**
     * Valor vigente de un parametro externo.
     *
     * @param grupo grupo (parte del nombre antes del separador).
     * @param clave clave (resto del nombre).
     * @param valor valor textual; un valor JSON se entrega como su texto JSON.
     */
    record ParametroExterno(String grupo, String clave, String valor) {
    }
}
