package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;




import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiErrorCatalogTest {

    @Test
    void validationException_con_codigo_conocido_resuelve_badRequest() {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromApplicationException(
                new ValidationException(UsuarioErrorCode.ERR_NOMBRE_PERSONA_INVALIDO, "Nombre invalido tecnico.")
        );

        assertEquals("ERR_NOMBRE_PERSONA_INVALIDO", descriptor.code());
        assertEquals(HttpStatus.BAD_REQUEST, descriptor.status());
        assertEquals("Los nombres y apellidos solo pueden contener letras, espacios, apostrofes y guiones.", descriptor.message());
    }

    @Test
    void notFound_con_codigo_de_dominio_resuelve_notFound() {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromApplicationException(
                new ResourceNotFoundException(EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE)
        );

        assertEquals("ERR_ESTUDIANTE_NO_EXISTE", descriptor.code());
        assertEquals(HttpStatus.NOT_FOUND, descriptor.status());
    }

    @Test
    void conflict_con_codigo_de_dominio_resuelve_conflict() {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromApplicationException(
                new ConflictException(GrupoErrorCode.ERR_MATRICULA_DUPLICADA, "Matricula duplicada tecnica.")
        );

        assertEquals("ERR_MATRICULA_DUPLICADA", descriptor.code());
        assertEquals(HttpStatus.CONFLICT, descriptor.status());
        assertEquals("La matricula ya se encuentra registrada para este grupo.", descriptor.message());
    }

    @Test
    void errorTecnico_no_expone_mensaje_original() {
        final ApiErrorDescriptor descriptor = ApiErrorCatalog.fromTechnicalException(
                new DatabaseOperationException("SQLException password token", new RuntimeException("db"))
        );

        assertEquals("DATABASE_OPERATION_ERROR", descriptor.code());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, descriptor.status());
        assertEquals("Ocurrio un error interno. Utilice el codigo de seguimiento para soporte.", descriptor.message());
    }
}
