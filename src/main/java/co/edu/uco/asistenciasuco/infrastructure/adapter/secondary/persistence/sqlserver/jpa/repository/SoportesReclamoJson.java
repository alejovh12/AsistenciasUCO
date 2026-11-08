package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SoporteReclamoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lee la columna {@code soportesJson} de {@code usp_consultar_detalle_solicitud_revision_docente} y
 * {@code usp_consultar_solicitudes_revision_estudiante} (CULM-001 D.2): arreglo
 * {@code [{idArchivo, nombreOriginal, tipoContenido, tamanioBytes}]} ordenado por adjuncion ({@code '[]'} sin soportes).
 */
final class SoportesReclamoJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private SoportesReclamoJson() {
    }

    /** Sin JSON (nulo o en blanco) no hay soportes; un JSON que no cumple la forma del contrato es un fallo tecnico. */
    static List<SoporteReclamoProjection> leer(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        final JsonNode raiz;
        try {
            raiz = MAPPER.readTree(json);
        } catch (final JacksonException exception) {
            throw new DatabaseOperationException("El JSON de soportes del reclamo no es valido.", exception);
        }
        if (!raiz.isArray()) {
            throw new DatabaseOperationException("El JSON de soportes del reclamo no es un arreglo.", new IllegalStateException(json));
        }
        final List<SoporteReclamoProjection> soportes = new ArrayList<>();
        for (final JsonNode nodo : raiz) {
            soportes.add(new SoporteReclamoProjection(
                    identificador(nodo.path("idArchivo")),
                    texto(nodo.path("nombreOriginal")),
                    texto(nodo.path("tipoContenido")),
                    nodo.path("tamanioBytes").asLong(0L)));
        }
        return List.copyOf(soportes);
    }

    private static UUID identificador(final JsonNode nodo) {
        if (nodo.isMissingNode() || nodo.isNull()) {
            throw new DatabaseOperationException("Un soporte del reclamo no trae idArchivo.", new IllegalStateException("idArchivo"));
        }
        try {
            return UUID.fromString(nodo.asString().strip());
        } catch (final IllegalArgumentException exception) {
            throw new DatabaseOperationException("Un soporte del reclamo trae un idArchivo no valido.", exception);
        }
    }

    private static String texto(final JsonNode nodo) {
        return nodo.isMissingNode() || nodo.isNull() ? null : nodo.asString();
    }
}
