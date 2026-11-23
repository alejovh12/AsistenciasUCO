package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports.CrearRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.primaryports.RetirarRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.CrearRecesoAcademicoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.RecesoAcademicoCreadoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataMensajeResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.RecesoAcademicoPeriodoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DEC-081: dias de receso academico del periodo. Solo los gestiona el ADMINISTRADOR ({@code /api/v1/admin/**} en SecurityConfig); la
 * validacion (fecha dentro del periodo y futura, no festivo, no repetida, periodo abierto) y el retiro bloqueado de un dia ya aplicado
 * (REC_010, 409) son de la DB. Decano y coordinador consultan por {@code GET /api/v1/coordinador/periodos-academicos/{idPeriodo}/recesos}.
 */
@RestController
@RequestMapping("/api/v1/admin/periodos-academicos/{idPeriodo}/recesos")
public final class AdminRecesoAcademicoController {

    private final CrearRecesoAcademicoInputPort crearReceso;
    private final RetirarRecesoAcademicoInputPort retirarReceso;
    private final ConsultarRecesosPeriodoInputPort consultarRecesos;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminRecesoAcademicoController(
            final CrearRecesoAcademicoInputPort crearReceso,
            final RetirarRecesoAcademicoInputPort retirarReceso,
            final ConsultarRecesosPeriodoInputPort consultarRecesos,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.crearReceso = Objects.requireNonNull(crearReceso, "CrearRecesoAcademicoInputPort es obligatorio.");
        this.retirarReceso = Objects.requireNonNull(retirarReceso, "RetirarRecesoAcademicoInputPort es obligatorio.");
        this.consultarRecesos = Objects.requireNonNull(consultarRecesos, "ConsultarRecesosPeriodoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiListResponse<RecesoAcademicoPeriodoResponse>> consultar(@PathVariable final UUID idPeriodo) {
        final List<RecesoAcademicoPeriodoResponse> recesos = consultarRecesos.execute(
                        new ConsultarRecesosPeriodoInputPort.Comando(idPeriodo, authenticatedUserResolver.requireAuthenticatedUserId()))
                .stream().map(RecesoAcademicoPeriodoResponse::from).toList();
        return ResponseEntity.ok(new ApiListResponse<>(true, recesos, recesos.size()));
    }

    @AuditableOperation(action = "CREAR_RECESO_ACADEMICO", resourceType = "RECESO_ACADEMICO", resourceIdPathVariable = "idPeriodo",
            metadataRequestFields = {"fecha"})
    @PostMapping
    public ResponseEntity<ApiDataMensajeResponse<RecesoAcademicoCreadoResponse>> crear(
            @PathVariable final UUID idPeriodo,
            @Valid @RequestBody final CrearRecesoAcademicoRequest request
    ) {
        final CrearRecesoAcademicoInputPort.Resultado resultado = crearReceso.execute(new CrearRecesoAcademicoInputPort.Comando(
                idPeriodo, request.getFecha(), request.getMotivo(), authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiDataMensajeResponse<>(true, RecesoAcademicoCreadoResponse.from(resultado), resultado.mensajeUsuario()));
    }

    @AuditableOperation(action = "RETIRAR_RECESO_ACADEMICO", resourceType = "RECESO_ACADEMICO", resourceIdPathVariable = "idReceso")
    @DeleteMapping("/{idReceso}")
    public ResponseEntity<ApiDataMensajeResponse<Void>> retirar(@PathVariable final UUID idPeriodo, @PathVariable final UUID idReceso) {
        final RetirarRecesoAcademicoInputPort.Resultado resultado = retirarReceso.execute(new RetirarRecesoAcademicoInputPort.Comando(
                idReceso, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataMensajeResponse<>(true, null, resultado.mensajeUsuario()));
    }
}