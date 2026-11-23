package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DEC-081 (contrato JPA/SP con SQL Server real, migracion 68): los cuatro SP del receso academico se invocan por el adaptador con sus
 * parametros nombrados (UUID, DATE, NVARCHAR) y sus rechazos llegan como errores semanticos con el codigo de la DB, no como fallos
 * tecnicos: un usuario inexistente recibe {@code ERR_USUARIO_NO_EXISTE} (USU_001, 404) tanto en el comando (crear) como en el retiro y
 * en las lecturas ({@code THROW 51000} con {@code DBCODE}).
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class RecesoAcademicoSqlServerIT {

    @Autowired
    private RecesoAcademicoRepositoryPort receso;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static void esperarRechazoDeNegocio(final String codigo, final org.junit.jupiter.api.function.Executable llamada) {
        final ApplicationException rechazo = assertThrows(ApplicationException.class, llamada);
        assertEquals(codigo, rechazo.getCode());
    }

    @Test
    void crear_con_usuario_inexistente_responde_usu_001_por_la_tupla_canonica() {
        esperarRechazoDeNegocio("ERR_USUARIO_NO_EXISTE", () -> receso.crear(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(30),
                "IT DEC-081", UUID.randomUUID()));
    }

    @Test
    void retirar_con_usuario_inexistente_responde_usu_001() {
        esperarRechazoDeNegocio("ERR_USUARIO_NO_EXISTE", () -> receso.retirar(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void consultar_con_usuario_inexistente_responde_usu_001_por_dbcode() {
        esperarRechazoDeNegocio("ERR_USUARIO_NO_EXISTE", () -> receso.consultar(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void consultar_destinatarios_con_usuario_inexistente_responde_usu_001() {
        esperarRechazoDeNegocio("ERR_USUARIO_NO_EXISTE", () -> receso.consultarDestinatarios(UUID.randomUUID(), UUID.randomUUID()));
    }
}