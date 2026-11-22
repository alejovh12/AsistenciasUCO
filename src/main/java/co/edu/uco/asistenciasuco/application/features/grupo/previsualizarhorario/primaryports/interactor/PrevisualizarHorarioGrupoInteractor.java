package co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.grupo.common.mapper.FranjaHorarioMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.PrevisualizarHorarioGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizacionHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.primaryports.dto.PrevisualizarHorarioGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.PrevisualizarHorarioGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.previsualizarhorario.usecase.domain.PrevisualizarHorarioGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PrevisualizacionHorarioGrupoProjection;

import java.util.Objects;

public final class PrevisualizarHorarioGrupoInteractor implements PrevisualizarHorarioGrupoInputPort {

    private final PrevisualizarHorarioGrupoUseCase useCase;

    public PrevisualizarHorarioGrupoInteractor(final PrevisualizarHorarioGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "PrevisualizarHorarioGrupoUseCase es obligatorio.");
    }

    @Override
    public PrevisualizacionHorarioGrupoDTO execute(final PrevisualizarHorarioGrupoDTO dto) {
        final var proyeccion = useCase.execute(new PrevisualizarHorarioGrupoDomain(dto.idGrupo(), dto.idAsignatura(), dto.idPeriodoAcademico(),
                dto.idDocente(), FranjaHorarioMapper.toDomains(dto.franjas()), dto.usuarioEjecutor()));
        return aDto(proyeccion);
    }

    static PrevisualizacionHorarioGrupoDTO aDto(final PrevisualizacionHorarioGrupoProjection p) {
        final var r = p.resumen();
        final Integer minutosMateria = r.horasSemanalesMateria() == null ? null : r.horasSemanalesMateria() * 60;
        final Integer programadosConConservados = r.minutosProgramados() + (r.minutosConservados() == null ? 0 : r.minutosConservados());
        final Integer diferenciaPeriodo = r.minutosEsperadosMateria() == null ? null : programadosConConservados - r.minutosEsperadosMateria();
        return new PrevisualizacionHorarioGrupoDTO(
                r.idGrupo(),
                new PrevisualizacionHorarioGrupoDTO.Asignatura(r.idAsignatura(), r.codigoAsignatura(), r.nombreAsignatura(), r.horasSemanalesMateria()),
                new PrevisualizacionHorarioGrupoDTO.Periodo(r.idPeriodo(), r.nombrePeriodo(), r.fechaInicioPeriodo(), r.fechaFinPeriodo(),
                        r.diasPeriodo(), r.diasPeriodo() / 7, r.estadoPeriodo()),
                r.zonaHoraria(),
                p.franjas().stream().map(f -> new PrevisualizacionHorarioGrupoDTO.Franja(f.codigoDia(), f.nombreDia(), f.horaInicio(), f.horaFin(),
                        f.minutos())).toList(),
                new PrevisualizacionHorarioGrupoDTO.ResumenSemanal(r.franjasPorSemana(), r.minutosSemanales(), minutosMateria,
                        minutosMateria == null ? null : r.minutosSemanales() - minutosMateria,
                        minutosMateria == null ? null : r.minutosSemanales() == minutosMateria),
                new PrevisualizacionHorarioGrupoDTO.ResumenPeriodo(r.sesionesCalendario(), r.sesionesProgramadas(), r.sesionesEnFestivo(),
                        r.sesionesNoProgramables(), r.minutosCalendario(), r.minutosProgramados(), r.minutosPerdidosFestivo(),
                        r.minutosNoProgramables(), r.minutosEsperadosMateria(), diferenciaPeriodo,
                        diferenciaPeriodo == null ? null : diferenciaPeriodo >= 0,
                        r.sesionesEnReceso(), r.minutosPerdidosReceso()),
                p.festivos().stream().map(f -> new PrevisualizacionHorarioGrupoDTO.Festivo(f.fecha(), f.nombre(), f.sesionesAfectadas())).toList(),
                p.recesos().stream().map(x -> new PrevisualizacionHorarioGrupoDTO.Receso(x.fecha(), x.motivo(), x.sesionesAfectadas())).toList(),
                p.sesiones().stream().map(s -> new PrevisualizacionHorarioGrupoDTO.Sesion(s.numero(), s.numeroSemana(), s.fecha(), s.horaInicio(),
                        s.horaFin(), s.fechaHoraInicioUtc(), s.fechaHoraFinUtc(), s.minutos(), s.estado(), s.nombreFestivo(),
                        s.motivoReceso())).toList(),
                r.esGrupoExistente() ? new PrevisualizacionHorarioGrupoDTO.CambioSobreGrupoExistente(
                        valor(r.sesionesConservadas()), valor(r.minutosConservados()), valor(r.sesionesFuturasQueSeRetiran()),
                        valor(r.sesionesFuturasQueSeCrean()), valor(r.sesionesFuturasQueSeConservan())) : null,
                p.conflictos().stream().map(c -> new PrevisualizacionHorarioGrupoDTO.Conflicto(c.codigo(), c.severidad(), c.mensaje(), c.franja())).toList(),
                r.puedeConfirmar());
    }

    private static int valor(final Integer numero) {
        return numero == null ? 0 : numero;
    }
}
