package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto.ConfiguracionReclamosDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain.ConfiguracionReclamosDomain;

public final class ConsultarConfiguracionReclamosMapper {

    private ConsultarConfiguracionReclamosMapper() {
    }

    public static ConfiguracionReclamosDTO toDTO(final ConfiguracionReclamosDomain domain) {
        return new ConfiguracionReclamosDTO(domain.maximoSoportes(), domain.tamanioMaximoBytes(), domain.tiposPermitidos(),
                domain.limiteSubidasPorMinuto(), domain.diasLimiteJustificacion());
    }
}
