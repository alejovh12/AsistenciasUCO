package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.ConsultarCatalogosSistemaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.CategoriaJustificacionCatalogoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.MotivoCancelacionCatalogoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCatalogosSistemaControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ConsultarCatalogosSistemaInputPort port = mock(ConsultarCatalogosSistemaInputPort.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminCatalogosSistemaController(port)).build();

    @Test
    void lista_las_categorias_de_justificacion() throws Exception {
        when(port.consultarCategoriasJustificacion()).thenReturn(List.of(new CategoriaJustificacionCatalogoDTO(ID, "MED", "Médico / Salud", "Descripción", true)));
        mvc.perform(get("/api/v1/admin/categorias-justificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.datos[0].codigo").value("MED"))
                .andExpect(jsonPath("$.datos[0].nombre").value("Médico / Salud"))
                .andExpect(jsonPath("$.datos[0].descripcion").value("Descripción"))
                .andExpect(jsonPath("$.datos[0].requiereSoporte").value(true));
    }

    @Test
    void lista_los_motivos_de_cancelacion_con_estado() throws Exception {
        when(port.consultarMotivosCancelacion()).thenReturn(List.of(new MotivoCancelacionCatalogoDTO(ID, "OTRO", "Otro motivo", 5, false)));
        mvc.perform(get("/api/v1/admin/motivos-cancelacion-sesion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].codigo").value("OTRO"))
                .andExpect(jsonPath("$.datos[0].orden").value(5))
                .andExpect(jsonPath("$.datos[0].estaActivo").value(false));
    }

    @Test
    void catalogos_vacios_devuelven_lista_vacia() throws Exception {
        when(port.consultarCategoriasJustificacion()).thenReturn(List.of());
        when(port.consultarMotivosCancelacion()).thenReturn(List.of());
        mvc.perform(get("/api/v1/admin/categorias-justificacion")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/admin/motivos-cancelacion-sesion")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void no_expone_escritura() throws Exception {
        for (final String ruta : List.of("/api/v1/admin/categorias-justificacion", "/api/v1/admin/motivos-cancelacion-sesion")) {
            mvc.perform(post(ruta)).andExpect(status().is4xxClientError());
            mvc.perform(put(ruta + "/" + ID)).andExpect(status().is4xxClientError());
            mvc.perform(patch(ruta + "/" + ID + "/estado")).andExpect(status().is4xxClientError());
        }
    }

    @Test
    void constructor_rechaza_puerto_nulo() {
        assertThrows(NullPointerException.class, () -> new AdminCatalogosSistemaController(null));
    }
}
