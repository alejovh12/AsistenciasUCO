package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.interactor.SubirArchivoInteractor;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl.SubirArchivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor.SolicitarRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl.SolicitarRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo.ArchivoController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.testsupport.files.SoporteFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 D.2 (CONTRACT D 3.3, DEC-044 (7)): secuencia de TRES subidas del mismo estudiante en la que la segunda lleva la firma
 * EICAR. El doble de ClamAV la rechaza con el codigo existente del antivirus (400 VALIDATION_ERROR) y SIN fileId ni
 * fila de soporte; la primera y la tercera quedan DRAFT y radicar el reclamo con esos dos fileId responde 202 con los dos soportes.
 * Un fileId que no quedo como borrador (el rechazado) no se puede adjuntar. Casos de uso, interactores y controladores reales;
 * solo los puertos secundarios (almacenamiento, antivirus, metadata y repositorio) son dobles en memoria.
 */
class RadicarReclamoConVariosSoportesAntivirusTest {

    private static final UUID ESTUDIANTE_USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE_ID = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    private final JsonMapper json = JsonMapper.builder().build();
    private final DobleClamAv clamAv = new DobleClamAv();
    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final MetadataEnMemoria metadata = new MetadataEnMemoria();
    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria(metadata);
    private final MockMvc mvc = construirMvc();

    private MockMvc construirMvc() {
        final AuthenticatedUserResolver identidad = () -> ESTUDIANTE_USUARIO;
        final InstitutionalScopePort alcance = mock(InstitutionalScopePort.class);
        when(alcance.findEstudianteIdByUsuario(ESTUDIANTE_USUARIO)).thenReturn(Optional.of(ESTUDIANTE_ID));
        final ArchivoController archivos = new ArchivoController(
                new SubirArchivoInteractor(new SubirArchivoUseCaseImpl(almacen, clamAv, metadata)),
                mock(DescargarArchivoInputPort.class), identidad);
        final AsistenciaController asistencias = new AsistenciaController(
                mock(RegistrarAsistenciasSesionInputPort.class),
                new SolicitarRevisionAsistenciaInteractor(new SolicitarRevisionAsistenciaUseCaseImpl(repositorio, alcance)),
                identidad);
        return MockMvcBuilders.standaloneSetup(archivos, asistencias)
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();
    }

    private static byte[] pdf(final String texto) {
        return ("%PDF-1.4\n" + texto + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1);
    }

    private UUID subirValido(final String nombre) throws Exception {
        final MvcResult resultado = mvc.perform(multipart("/api/v1/archivos/subir")
                        .file(new MockMultipartFile("archivo", nombre, "application/pdf", pdf("soporte " + nombre))))
                .andExpect(status().isCreated())
                .andReturn();
        final JsonNode datos = json.readTree(resultado.getResponse().getContentAsString()).path("datos");
        return UUID.fromString(datos.path("fileId").asString());
    }

    @Test
    void la_segunda_subida_con_eicar_se_rechaza_sin_fileId_ni_fila_y_las_otras_dos_se_radican_en_un_reclamo() throws Exception {
        final UUID primero = subirValido("uno.pdf");

        mvc.perform(multipart("/api/v1/archivos/subir")
                        .file(new MockMultipartFile("archivo", "infectado.pdf", "application/pdf",
                                pdf(SoporteFixtures.EICAR_TEST_STRING))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.datos").doesNotExist())
                .andExpect(jsonPath("$.fileId").doesNotExist());

        final UUID tercero = subirValido("tres.pdf");

        // Solo existen las filas DRAFT de la primera y la tercera; el infectado no dejo bytes ni metadata
        assertEquals(List.of(primero, tercero), List.copyOf(metadata.borradores.keySet()));
        assertEquals(2, almacen.objetos.size());
        assertEquals(3, clamAv.escaneos, "Cada subida pasa por el antivirus antes de almacenarse");

        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"MED","justificacion":"Cita médica y laboratorio",
                                 "soporteArchivoIds":["%s","%s"]}
                                """.formatted(SESION, primero, tercero)))
                .andExpect(status().isAccepted());

        assertEquals(List.of(primero, tercero), repositorio.ultimaSolicitud.soportes());
        assertEquals("MED", repositorio.ultimaSolicitud.categoria());
        assertEquals(ESTUDIANTE_ID, repositorio.ultimaSolicitud.estudiante());
        assertEquals(ESTUDIANTE_USUARIO, repositorio.ultimaSolicitud.usuarioEjecutor());
        assertTrue(metadata.borradores.isEmpty(), "Al radicar los dos soportes dejan de ser borradores");
    }

    @Test
    void radicar_con_el_fileId_del_archivo_infectado_o_inexistente_no_se_acepta() throws Exception {
        final UUID valido = subirValido("uno.pdf");
        mvc.perform(multipart("/api/v1/archivos/subir")
                        .file(new MockMultipartFile("archivo", "infectado.pdf", "application/pdf", pdf(SoporteFixtures.EICAR_TEST_STRING))))
                .andExpect(status().isBadRequest());
        final UUID inexistente = UUID.randomUUID();

        mvc.perform(post("/api/v1/asistencias/revisiones").contentType("application/json")
                        .content("""
                                {"sesionId":"%s","categoria":"MED","justificacion":"Cita médica",
                                 "soporteArchivoIds":["%s","%s"]}
                                """.formatted(SESION, valido, inexistente)))
                .andExpect(status().isNotFound());

        assertEquals(List.of(valido), List.copyOf(metadata.borradores.keySet()), "Ante el rechazo el soporte valido sigue DRAFT");
    }

    // ------------------------------------------------------------------------------------------------------ dobles

    /** Doble de ClamAV (clamd INSTREAM): firma EICAR => INFECTED; cuenta cuantos archivos analizo. */
    private static final class DobleClamAv implements MalwareScanPort {
        private int escaneos;

        @Override
        public ScanResult scan(final byte[] content) {
            escaneos++;
            return new String(content, StandardCharsets.ISO_8859_1).contains(SoporteFixtures.EICAR_TEST_STRING)
                    ? ScanResult.infected("Eicar-Signature") : ScanResult.clean();
        }
    }

    private static final class AlmacenEnMemoria implements FileStoragePort {
        private final Map<String, StoredObject> objetos = new LinkedHashMap<>();

        @Override
        public StoredObject store(final StoreObjectCommand command) {
            final StoredObject objeto = new StoredObject(command.fileId(), command.content(), command.metadata());
            objetos.put(command.fileId(), objeto);
            return objeto;
        }

        @Override
        public StoredObject read(final String fileId) {
            return objetos.get(fileId);
        }

        @Override
        public boolean exists(final String fileId) {
            return objetos.containsKey(fileId);
        }

        @Override
        public void delete(final String fileId) {
            objetos.remove(fileId);
        }
    }

    /** Metadata de ArchivoSoporte: solo filas DRAFT (lo que registra usp_registrar_archivo_soporte). */
    private static final class MetadataEnMemoria implements SoporteArchivoMetadataPort {
        private final Map<UUID, RegistrarSoporteBorrador> borradores = new LinkedHashMap<>();

        @Override
        public void registrarBorrador(final RegistrarSoporteBorrador comando) {
            borradores.put(comando.fileId(), comando);
        }

        @Override
        public Optional<SoporteArchivoAutorizado> consultarAutorizado(final UUID fileId, final UUID usuarioEjecutor) {
            return Optional.empty();
        }
    }

    /** Repositorio de reclamos: como usp_radicar_solicitud_revision_asistencia_v3, solo adjunta borradores propios (si no, ARC_001/404). */
    private static final class RepositorioEnMemoria implements AsistenciaRepositoryPort {
        private final MetadataEnMemoria metadata;
        private SolicitarRevisionAsistenciaRepositoryDTO ultimaSolicitud;

        private RepositorioEnMemoria(final MetadataEnMemoria metadata) {
            this.metadata = metadata;
        }

        @Override
        public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
            final List<UUID> noDisponibles = new ArrayList<>();
            for (final UUID soporte : dto.soportes()) {
                final SoporteArchivoMetadataPort.RegistrarSoporteBorrador borrador = metadata.borradores.get(soporte);
                if (borrador == null || !borrador.usuarioPropietario().equals(dto.usuarioEjecutor())) {
                    noDisponibles.add(soporte);
                }
            }
            if (!noDisponibles.isEmpty()) {
                throw new ResourceNotFoundException("El soporte indicado no esta disponible para esta solicitud.");
            }
            dto.soportes().forEach(metadata.borradores::remove);
            ultimaSolicitud = dto;
            assertFalse(dto.soportes().isEmpty());
        }

        @Override
        public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(final ConsultarAsistenciasPorGrupoRepositoryDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resolverSolicitudRevisionAsistencia(final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String retirarSolicitudRevision(final UUID idSolicitudRevision, final UUID usuarioEjecutor) {
            throw new UnsupportedOperationException();
        }
    }
}
