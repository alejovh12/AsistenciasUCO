package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.ActualizarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.primaryports.CerrarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.ConsultarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.ConsultarSesionesPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.GenerarSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MAINT-003 guard while UTC-D06 (historical provenance) is pending.
 *
 * <p>No controller may expose /api/v2/** until the owner decision on
 * HISTORICAL_TZ_UNDETERMINED rows is versioned, and the v1 session wire must keep
 * rejecting offsets: the inactive HttpUtcInstantCodec must never leak into v1.
 */
class SesionUtcActivationGuardTest {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    // AS-IS v1 code observed for offset input; UTC-D02 proposes VALIDATION_ERROR for v2 (decision pending).
    private static final String V1_INVALID_DATE_TIME_CODE = "ERR_FECHA_HORA_INVALIDA";
    private final CrearSesionInputPort create = mock(CrearSesionInputPort.class);
    private final ActualizarSesionInputPort update = mock(ActualizarSesionInputPort.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SesionController(
                    create,
                    mock(ConsultarSesionInputPort.class),
                    mock(ConsultarSesionesPorGrupoInputPort.class),
                    mock(CerrarSesionInputPort.class),
                    update,
                    mock(GenerarSesionesGrupoInputPort.class),
                    UUID::randomUUID))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
            .build();

    @Test
    void noControllerMapsApiV2WhileHistoricalProvenanceIsUndecided() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        final List<String> v2Mappings = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            final Class<?> controller = Class.forName(candidate.getBeanClassName());
            final List<String> classPaths = paths(findMergedAnnotation(controller, RequestMapping.class));
            for (Method method : controller.getDeclaredMethods()) {
                for (String methodPath : paths(findMergedAnnotation(method, RequestMapping.class))) {
                    for (String classPath : classPaths.isEmpty() ? List.of("") : classPaths) {
                        final String full = classPath + methodPath;
                        if (full.startsWith("/api/v2")) {
                            v2Mappings.add(controller.getSimpleName() + "#" + method.getName() + " -> " + full);
                        }
                    }
                }
            }
            classPaths.stream().filter(path -> path.startsWith("/api/v2"))
                    .forEach(path -> v2Mappings.add(controller.getSimpleName() + " -> " + path));
        }
        assertEquals(List.of(), v2Mappings,
                "UTC v2 no puede activarse antes de versionar la decisión UTC-D06 (HISTORICAL_TZ_UNDETERMINED)");
    }

    @Test
    void v1CreateStillRejectsOffsetsInsteadOfConvertingThem() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType("application/json")
                        .content("""
                                {"grupo":"%s","nombre":"Tema válido",
                                 "fechaHoraInicio":"2026-07-15T16:00:00+02:00","fechaHoraFin":"2026-07-15T17:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(V1_INVALID_DATE_TIME_CODE));
        verifyNoInteractions(create);
    }

    @Test
    void v1PatchStillRejectsOffsetsInsteadOfConvertingThem() throws Exception {
        mvc.perform(patch("/api/v1/sesiones/{id}", UUID.randomUUID()).contentType("application/json")
                        .content("""
                                {"nombre":"Tema válido",
                                 "fechaHoraInicio":"2026-07-15T14:00:00Z","fechaHoraFin":"2026-07-15T15:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(V1_INVALID_DATE_TIME_CODE));
        verifyNoInteractions(update);
    }

    private static List<String> paths(final RequestMapping mapping) {
        if (mapping == null) {
            return List.of();
        }
        final List<String> all = new ArrayList<>(Arrays.asList(mapping.path()));
        all.addAll(Arrays.asList(mapping.value()));
        return all.stream().distinct().toList();
    }
}
