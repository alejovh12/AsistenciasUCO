package co.edu.uco.asistenciasuco.application.features.coordinador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.interactor.ConsultarAlertaTempranaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.ConsultarAlertaTempranaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertaTempranaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertasPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.ConsultarBitacoraEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.interactor.ConsultarBitacoraEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.ConsultarBitacoraEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraSesionDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ConsultarFichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.interactor.ConsultarFichaEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.ConsultarFichaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.FichaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain.ResumenAsignaturaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.interactor.ConsultarKpisCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.ConsultarKpisCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.KpisCoordinadorDomain;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Cubre interactores y mappers de las cuatro consultas del coordinador con datos completos (sin mockStatic). */
class IndicadoresCoordinadorPrimaryPortsTest {

    @SuppressWarnings("unchecked")
    private static <T> T lleno(final Class<T> tipo) {
        try {
            final RecordComponent[] componentes = tipo.getRecordComponents();
            final Class<?>[] tipos = new Class<?>[componentes.length];
            final Object[] valores = new Object[componentes.length];
            for (int i = 0; i < componentes.length; i++) {
                tipos[i] = componentes[i].getType();
                valores[i] = valor(componentes[i]);
            }
            final Constructor<T> constructor = tipo.getDeclaredConstructor(tipos);
            constructor.setAccessible(true);
            return constructor.newInstance(valores);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object valor(final RecordComponent c) {
        final Class<?> t = c.getType();
        if (t == String.class) return "x";
        if (t == UUID.class) return UUID.randomUUID();
        if (t == int.class || t == Integer.class) return 1;
        if (t == long.class || t == Long.class) return 1L;
        if (t == boolean.class || t == Boolean.class) return true;
        if (t == BigDecimal.class) return BigDecimal.ONE;
        if (t == Instant.class) return Instant.EPOCH;
        if (t == OffsetDateTime.class) return OffsetDateTime.now();
        if (t == LocalDateTime.class) return LocalDateTime.now();
        if (t == List.class) return List.of();
        return null;
    }

    @Test
    void kpis_mapea_dto_a_dominio_y_dominio_a_dto() {
        final ConsultarKpisCoordinadorUseCase useCase = mock(ConsultarKpisCoordinadorUseCase.class);
        final KpisCoordinadorDomain dominio = lleno(KpisCoordinadorDomain.class);
        when(useCase.execute(any())).thenReturn(dominio);
        final KpisCoordinadorDTO dto = new ConsultarKpisCoordinadorInteractor(useCase).execute(lleno(ConsultarKpisCoordinadorDTO.class));
        assertEquals(dominio.totalEstudiantes(), dto.totalEstudiantes());
        assertEquals(dominio.porcentajeCumplimiento(), dto.porcentajeCumplimiento());
        assertThrows(NullPointerException.class, () -> new ConsultarKpisCoordinadorInteractor(null));
    }

    @Test
    void alertas_mapea_filas_y_total() {
        final ConsultarAlertaTempranaUseCase useCase = mock(ConsultarAlertaTempranaUseCase.class);
        final AlertasPaginaDomain pagina = new AlertasPaginaDomain(List.of(lleno(AlertaTempranaDomain.class)), 7);
        when(useCase.execute(any())).thenReturn(pagina);
        final AlertasPaginaDTO dto = new ConsultarAlertaTempranaInteractor(useCase).execute(lleno(ConsultarAlertaTempranaDTO.class));
        assertEquals(1, dto.alertas().size());
        assertEquals(7, dto.totalRegistros());
        assertThrows(NullPointerException.class, () -> new ConsultarAlertaTempranaInteractor(null));
    }

    @Test
    void bitacora_mapea_sesiones_y_total() {
        final ConsultarBitacoraEstudianteUseCase useCase = mock(ConsultarBitacoraEstudianteUseCase.class);
        final BitacoraPaginaDomain pagina = new BitacoraPaginaDomain(List.of(lleno(BitacoraSesionDomain.class)), 9);
        when(useCase.execute(any())).thenReturn(pagina);
        final BitacoraPaginaDTO dto = new ConsultarBitacoraEstudianteInteractor(useCase).execute(lleno(ConsultarBitacoraEstudianteDTO.class));
        assertEquals(1, dto.sesiones().size());
        assertEquals(9, dto.totalRegistros());
        assertThrows(NullPointerException.class, () -> new ConsultarBitacoraEstudianteInteractor(null));
    }

    @Test
    void ficha_mapea_resumen_por_asignatura() {
        final ConsultarFichaEstudianteUseCase useCase = mock(ConsultarFichaEstudianteUseCase.class);
        final FichaEstudianteDomain base = lleno(FichaEstudianteDomain.class);
        final List<ResumenAsignaturaDomain> resumen = List.of(lleno(ResumenAsignaturaDomain.class));
        final FichaEstudianteDomain ficha = conResumen(base, resumen);
        when(useCase.execute(any())).thenReturn(ficha);
        final FichaEstudianteDTO dto = new ConsultarFichaEstudianteInteractor(useCase).execute(lleno(ConsultarFichaEstudianteDTO.class));
        assertNotNull(dto);
        assertEquals(1, dto.resumenPorAsignatura().size());
        assertThrows(NullPointerException.class, () -> new ConsultarFichaEstudianteInteractor(null));
    }

    private static FichaEstudianteDomain conResumen(final FichaEstudianteDomain base, final List<ResumenAsignaturaDomain> resumen) {
        try {
            final RecordComponent[] cs = FichaEstudianteDomain.class.getRecordComponents();
            final Class<?>[] tipos = new Class<?>[cs.length];
            final Object[] valores = new Object[cs.length];
            for (int i = 0; i < cs.length; i++) {
                tipos[i] = cs[i].getType();
                valores[i] = cs[i].getType() == List.class ? resumen : cs[i].getAccessor().invoke(base);
            }
            return FichaEstudianteDomain.class.getDeclaredConstructor(tipos).newInstance(valores);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
