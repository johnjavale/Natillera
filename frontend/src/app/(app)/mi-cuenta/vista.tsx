'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Boton, Cargando, Campo, Dinero, Estado, Kpi, MensajeError, Tabla, Tarjeta, Titulo, Vacio, Exito } from '@/components/ui';
import { api, urlDescarga } from '@/lib/api';
import { useCiclo, useSesion } from '@/lib/consultas';
import { formatoCOP, formatoFecha, formatoPorcentaje, nombreMesCiclo } from '@/lib/formato';
import type { EstadoCuenta } from '@/lib/tipos';

export function VistaMiCuenta() {
  const sesion = useSesion();
  const ciclo = useCiclo();
  const id = sesion.data?.integranteId;
  const cuenta = useQuery({
    queryKey: ['estado-cuenta', id],
    queryFn: () => api.get<EstadoCuenta>(`/reportes/estado-cuenta/${id}`),
    enabled: Boolean(id),
  });

  if (sesion.isLoading || cuenta.isLoading || ciclo.isLoading) return <Cargando filas={6} />;
  if (!id) return <Vacio mensaje="Su usuario no está vinculado a un integrante de la natillera." />;
  if (cuenta.error) return <MensajeError error={cuenta.error} />;
  const e = cuenta.data;
  const c = ciclo.data;
  if (!e || !c) return null;
  const r = e.resumen;

  return (
    <div className="space-y-4">
      <Titulo subtitulo={`Mes ${c.mesesTranscurridos} de ${c.numeroMeses} · tesorero: ${r.nombreTesorero ?? 'sin asignar'}`}>
        Hola, {r.nombre}
      </Titulo>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Kpi etiqueta="Mi cuota mensual" valor={formatoCOP(r.cuotaMensual)} />
        <Kpi etiqueta="Total aportado" valor={formatoCOP(r.totalAportado)} nota={`${formatoPorcentaje(r.cumplimiento)} del año`} />
        <Kpi etiqueta="Atraso" valor={formatoCOP(r.atraso)} alerta={r.atraso > 0} nota={r.atraso > 0 ? 'Póngase al día con su tesorero' : 'Al día'} />
        <Kpi etiqueta="Saldo de mi cajita Nu" valor={formatoCOP(r.saldoCajita)} />
      </div>

      {c.estado === 'INSCRIPCION' && <EditarCuota actual={r.cuotaMensual} />}

      <Tarjeta titulo="Mi ahorro mes a mes" acciones={
        <a className="text-sm text-marca-700 underline" href={urlDescarga(`/reportes/estado-cuenta/${id}`, 'xlsx')}>Descargar Excel</a>
      }>
        <Tabla etiqueta="Ahorro esperado vs real por mes">
          <thead><tr><th>Mes</th><th>Esperado</th><th>Real</th></tr></thead>
          <tbody>
            {e.aportesPorMes.map((v, i) => {
              const vencido = i + 1 <= c.mesesTranscurridos;
              return (
                <tr key={i}>
                  <td>{nombreMesCiclo(c.fechaInicio, i + 1)}</td>
                  <td><Dinero valor={r.cuotaMensual} /></td>
                  <td><Dinero valor={v} alerta={vencido && v < r.cuotaMensual} /></td>
                </tr>
              );
            })}
          </tbody>
        </Tabla>
      </Tarjeta>

      <Tarjeta titulo="Mis préstamos">
        {e.prestamos.length === 0 ? <Vacio mensaje="No tiene préstamos." /> : e.prestamos.map((p) => (
          <div key={p.id} className="mb-4">
            <p className="mb-2 flex flex-wrap items-center gap-2 text-sm">
              <strong>{formatoCOP(p.monto)}</strong> en {p.numeroCuotas} cuotas de {formatoCOP(p.valorCuota)} · saldo{' '}
              <Dinero valor={p.saldoPendiente} /> <Estado estado={p.estado} />
            </p>
            <Tabla etiqueta="Plan de pagos">
              <thead><tr><th>Cuota</th><th>Vence</th><th>Capital</th><th>Interés</th><th>Valor</th><th>Estado</th></tr></thead>
              <tbody>
                {p.plan.map((q) => (
                  <tr key={q.numero}>
                    <td>{q.numero}</td>
                    <td>{nombreMesCiclo(c.fechaInicio, q.mesVencimiento)}</td>
                    <td><Dinero valor={q.capital} /></td>
                    <td><Dinero valor={q.interes} /></td>
                    <td><Dinero valor={q.valor} /></td>
                    <td className={q.pagada ? 'text-hoja-700' : q.vencida ? 'font-semibold text-red-700' : ''}>
                      {q.pagada ? '✓ Pagada' : q.vencida ? '▲ Vencida' : 'Pendiente'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Tabla>
          </div>
        ))}
      </Tarjeta>

      <Tarjeta titulo="Mis boletas de rifas">
        {e.boletas.length === 0 ? <Vacio mensaje="Aún no hay rifas." /> : (
          <Tabla etiqueta="Boletas">
            <thead><tr><th>Rifa</th><th>Boletas</th><th>A pagar</th><th>Pagado</th><th>Pendiente</th></tr></thead>
            <tbody>
              {e.boletas.map((b) => (
                <tr key={b.actividadId}>
                  <td>{b.actividad}</td><td>{b.boletas}</td><td><Dinero valor={b.valorAPagar} /></td>
                  <td><Dinero valor={b.pagado} /></td><td><Dinero valor={b.pendiente} alerta={b.pendiente > 0} /></td>
                </tr>
              ))}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>

      {e.reparto && (
        <Tarjeta titulo={`Mi repartición · puesto ${e.reparto.puesto} por ahorro`}>
          <p className="mb-2 text-sm text-stone-600">
            Los intereses se reparten según lo que cada quien ahorra: usted aporta el {formatoPorcentaje(e.reparto.participacionProyectada)} del
            ahorro total y recibe ese mismo porcentaje de los intereses. Las rifas se reparten en partes iguales.
          </p>
          <Tabla etiqueta="Repartición proyectada y real">
            <thead><tr><th>Concepto</th><th>Proyectado</th><th>Real a la fecha</th></tr></thead>
            <tbody>
              <tr><td>Ahorro</td><td><Dinero valor={e.reparto.ahorroEsperado} /></td><td><Dinero valor={e.reparto.ahorroReal} /></td></tr>
              <tr><td>Intereses de préstamos</td><td><Dinero valor={e.reparto.interesesProyectados} /></td><td><Dinero valor={e.reparto.interesesReales} /></td></tr>
              <tr><td>Rifas</td><td><Dinero valor={e.reparto.actividadesProyectadas} /></td><td><Dinero valor={e.reparto.actividadesReales} /></td></tr>
              <tr><td>Rendimiento de mi cajita Nu*</td><td><Dinero valor={e.reparto.rendimientoNuProyectado} /></td><td><Dinero valor={e.reparto.rendimientoNuReal} /></td></tr>
              <tr className="font-semibold"><td>Total a recibir</td><td><Dinero valor={e.reparto.totalProyectado} /></td><td><Dinero valor={e.reparto.totalReal} /></td></tr>
            </tbody>
          </Tabla>
          <p className="mt-2 text-xs text-stone-500">* Estimado: si se saca plata de la cajita para préstamos, el rendimiento real baja.</p>
        </Tarjeta>
      )}

      <Tarjeta titulo="Movimientos de mi cajita">
        {e.movimientos.length === 0 ? <Vacio mensaje="Sin movimientos." /> : (
          <Tabla etiqueta="Movimientos">
            <thead><tr><th>Fecha</th><th>Tipo</th><th>Descripción</th><th>Entrada</th><th>Salida</th></tr></thead>
            <tbody>
              {e.movimientos.map((m) => (
                <tr key={m.id} className={m.anuladoPor || m.anulaA ? 'text-stone-400' : ''}>
                  <td>{formatoFecha(m.fecha)}</td><td>{m.tipoEtiqueta}</td><td>{m.descripcion}</td>
                  <td>{m.entrada > 0 ? formatoCOP(m.entrada) : ''}</td><td>{m.salida > 0 ? formatoCOP(m.salida) : ''}</td>
                </tr>
              ))}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
    </div>
  );
}

function EditarCuota({ actual }: { actual: number }) {
  const qc = useQueryClient();
  const [valor, setValor] = useState(String(actual));
  const [ok, setOk] = useState<string | null>(null);
  const m = useMutation({
    mutationFn: () => api.put('/integrantes/me/cuota', { cuotaMensual: Number(valor) }),
    onSuccess: () => {
      setOk('Cuota actualizada');
      qc.invalidateQueries();
    },
  });
  return (
    <Tarjeta titulo="Proponer mi cuota mensual">
      <p className="mb-3 text-sm text-stone-600">Puede cambiarla mientras la natillera está en inscripción. Después solo la cambia el administrador.</p>
      <form className="flex flex-wrap items-end gap-3" onSubmit={(ev) => { ev.preventDefault(); setOk(null); m.mutate(); }}>
        <Campo etiqueta="Cuota mensual (COP)" name="cuota" type="number" min={0} step={1000} value={valor} onChange={(ev) => setValor(ev.target.value)} className="w-48" />
        <Boton type="submit" disabled={m.isPending}>Guardar</Boton>
      </form>
      <div className="mt-2"><MensajeError error={m.error} /><Exito mensaje={ok} /></div>
    </Tarjeta>
  );
}
