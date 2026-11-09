package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain.ConfiguracionReclamosDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ConfiguracionReclamosProjection;

import java.util.Arrays;
import java.util.List;

public final class ConsultarConfiguracionReclamosRepositoryMapper {

    private static final String SEPARADOR_TIPOS = ",";

    private ConsultarConfiguracionReclamosRepositoryMapper() {
    }

    /** Precondicion: la proyeccion trae todos sus numeros (el caso de uso lo verifica). */
    public static ConfiguracionReclamosDomain toDomain(final ConfiguracionReclamosProjection p) {
        return new ConfiguracionReclamosDomain(p.maximoSoportes(), p.tamanioMaximoBytes(), tipos(p.tiposPermitidos()),
                p.limiteSubidasPorMinuto(), p.diasLimiteJustificacion());
    }

    private static List<String> tipos(final String tiposPermitidos) {
        if (tiposPermitidos == null) {
            return List.of();
        }
        return Arrays.stream(tiposPermitidos.split(SEPARADOR_TIPOS)).map(String::strip).filter(tipo -> !tipo.isEmpty()).toList();
    }
}
