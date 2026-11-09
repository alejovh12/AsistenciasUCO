package co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports;

import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaCsvParseada;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;

import java.util.List;

public interface CsvCargaUsuariosParserPort {
    List<FilaCsvParseada> parsear(byte[] contenidoBytes, PerfilDestinoCarga perfilDestino);
}
