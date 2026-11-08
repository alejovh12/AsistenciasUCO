package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.admin.exception.FacultadErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** F-ADM-S05: FAC_002 debe mapear a FacultadErrorCode.FAC_002 (HTTP 409 Conflict) con mensaje de titularidad explicito. */
class DbFailureClassifierFacultadTest {

    @Test
    void fac_002_se_clasifica_como_facultad_error_code_fac_002() {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("El decano ya es titular", "DBCODE=FAC_002|detalle", "actualizarFacultad");

        assertEquals(FacultadErrorCode.FAC_002, clasificado);
        assertEquals("FAC_002", clasificado.code());
        assertEquals("El decano seleccionado ya es titular de otra facultad activa.", clasificado.defaultMessage());
        assertEquals(ErrorKind.CONFLICT, clasificado.kind());
    }
}
