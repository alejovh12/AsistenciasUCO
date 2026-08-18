package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsultarTiposIdentificacionMapperTest {

    @Test
    void toDTOs_retorna_lista_vacia_si_la_entrada_es_null() {
        assertEquals(0, ConsultarTiposIdentificacionMapper.toDTOs(null).size());
    }

    @Test
    void toDTOs_expone_primitivos_de_transporte() {
        final UUID id = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");

        final List<TipoIdentificacionDTO> resultado = ConsultarTiposIdentificacionMapper.toDTOs(List.of(
                TipoIdentificacionDomain.reconstruir(id, "CC", "Cedula")
        ));

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(id, resultado.get(0).getId());
        assertEquals("CC", resultado.get(0).getTipoIdentificacion());
        assertEquals("Cedula", resultado.get(0).getNombre());
    }

    @Test
    void toDTOs_preserva_varios_registros_en_el_mismo_orden() {
        final List<TipoIdentificacionDTO> resultado = ConsultarTiposIdentificacionMapper.toDTOs(List.of(
                TipoIdentificacionDomain.reconstruir(
                        UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                        "CC",
                        "Cedula"
                ),
                TipoIdentificacionDomain.reconstruir(
                        UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d"),
                        "PA",
                        "Pasaporte"
                )
        ));

        assertEquals("CC", resultado.get(0).getTipoIdentificacion());
        assertEquals("PA", resultado.get(1).getTipoIdentificacion());
    }
}
