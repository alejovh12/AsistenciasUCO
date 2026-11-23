package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.ValidarTokenMatriculaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.validation.MatriculaQrRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU176B (CA-04): un estudiante con sesion iniciada se matricula con el token o PIN de la ventana. Reemplaza al antiguo
 * stub 501 de {@code POST /api/v1/estudiante/matricular-grupo}. El rol ESTUDIANTE lo exige {@code /api/v1/estudiante/**}.
 */
@RestController
@RequestMapping("/api/v1/estudiante")
public final class EstudianteMatriculaQrController {

    public record MatriculaGrupoResponse(UUID idGrupo) {
    }

    private static final MatriculaQrRequestValidator VALIDATOR = new MatriculaQrRequestValidator();

    private final MatricularEstudianteAutenticadoInputPort matricular;
    private final ValidarTokenMatriculaInputPort validarToken;
    private final NetworkClientIpResolver ipResolver;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public EstudianteMatriculaQrController(
            final MatricularEstudianteAutenticadoInputPort matricular,
            final ValidarTokenMatriculaInputPort validarToken,
            final NetworkClientIpResolver ipResolver,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.matricular = Objects.requireNonNull(matricular, "MatricularEstudianteAutenticadoInputPort es obligatorio.");
        this.validarToken = Objects.requireNonNull(validarToken, "ValidarTokenMatriculaInputPort es obligatorio.");
        this.ipResolver = Objects.requireNonNull(ipResolver, "NetworkClientIpResolver es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    /**
     * Revision final P3: validacion con sesion iniciada. Mismos datos del curso que la publica, pero {@code programasElegibles} es la
     * interseccion entre los programas del estudiante y los del grupo (vacio si no hay nada que elegir).
     */
    @PostMapping("/matricula-qr/validar")
    public ResponseEntity<ApiDataResponse<MatriculaQrPublicaController.CursoMatriculaQrResponse>> validar(
            @Valid @RequestBody final MatriculaQrRequests.Credencial request,
            final HttpServletRequest http
    ) {
        RequestValidationGuard.validate(VALIDATOR.validarCredencial(request));
        final var curso = validarToken.execute(new ValidarTokenMatriculaInputPort.Comando(
                request.token(), request.pin(), ipResolver.resolve(http), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, new MatriculaQrPublicaController.CursoMatriculaQrResponse(
                curso.idGrupo(), curso.nombreAsignatura(), curso.nombreGrupo(), curso.nombreDocente(),
                curso.fechaExpiracionUtc(), curso.cupoDisponible(), List.of(),
                curso.programasElegibles().stream()
                        .map(p -> new MatriculaQrPublicaController.ProgramaElegibleResponse(p.idPrograma(), p.nombre()))
                        .toList())));
    }

    @PostMapping("/matricular-grupo")
    public ResponseEntity<ApiDataResponse<MatriculaGrupoResponse>> matricularGrupo(
            @Valid @RequestBody final MatriculaQrRequests.Credencial request,
            final HttpServletRequest http
    ) {
        RequestValidationGuard.validate(VALIDATOR.validarCredencial(request));
        final var resultado = matricular.execute(new MatricularEstudianteAutenticadoInputPort.Comando(
                request.token(), request.pin(), ipResolver.resolve(http), authenticatedUserResolver.requireAuthenticatedUserId(),
                request.idPrograma()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, new MatriculaGrupoResponse(resultado.idGrupo())));
    }
}
