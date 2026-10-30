'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { useState } from 'react';
import { Boton, Campo, Cargando, Dinero, Estado, Exito, MensajeError, Selector, Tabla, Tarjeta, Titulo } from '@/components/ui';
import { api } from '@/lib/api';
import { useCajitas, useCiclo } from '@/lib/consultas';
import { formatoFecha, formatoPorcentaje, hoyISO, nombreMesCiclo } from '@/lib/formato';
import type { Prestamo } from '@/lib/tipos';

export default function DetallePrestamo() {
  const { id } = useParams<{ id: string }>();
  const qc = useQueryClient();
  const ciclo = useCiclo();
  const cajitas = useCajitas();
  const prestamo = useQuery({ queryKey: ['prestamo', id], queryFn: () => api.get<Prestamo>(`/prestamos/${id}`) });
  const [fecha, setFecha] = useState(hoyISO());
  const [cajita, setCajita] = useState('');
  const [ok, setOk] = useState<string | null>(null);

  const pagar = useMutation({
    mutationFn: () => api.post<Prestamo>(`/prestamos/${id}/pagos`, { fecha, cajitaDestinoId: cajita ? Number(cajita) : undefined }),
    onSuccess: () => {
      setOk('Pago registrado');
      qc.invalidateQueries();
    },
  });
  const anular = useMutation({
    mutationFn: ({ pagoId, motivo }: { pagoId: number; motivo: string }) => api.post(`/prestamos/${id}/pagos/${pagoId}/anular`, { motivo }),
    onSuccess: () => qc.invalidateQueries(),
  });

  if (prestamo.isLoading || ciclo.isLoading) return <Cargando filas={6} />;
  if (prestamo.error) return <MensajeError error={prestamo.error} />;
  const p = prestamo.data;
  const c = ciclo.data;
  if (!p || !c) return null;

  return (
    <div className="space-y-4">
      <Titulo subtitulo={`Desembolso ${formatoFecha(p.fechaDesembolso)} · mes ${p.mesDelPrestamo} · tasa ${formatoPorcentaje(p.tasaMensual)} mensual`}>
        Préstamo #{p.id} · {p.nombreIntegrante} <Estado estado={p.estado} />
      </Titulo>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Tarjeta><p className="text-xs text-stone-600">Monto</p><p className="text-lg font-semibold"><Dinero valor={p.monto} /></p></Tarjeta>
        <Tarjeta><p className="text-xs text-stone-600">Total a pagar</p><p className="text-lg font-semibold"><Dinero valor={p.totalAPagar} /></p></Tarjeta>
        <Tarjeta><p className="text-xs text-stone-600">Saldo pendiente</p><p className="text-lg font-semibold"><Dinero valor={p.saldoPendiente} /></p></Tarjeta>
        <Tarjeta><p className="text-xs text-stone-600">Vencido</p><p className="text-lg font-semibold"><Dinero valor={p.saldoVencido} alerta={p.saldoVencido > 0} /></p></Tarjeta>
      </div>

      {p.estado !== 'PAGADO' && (
        <Tarjeta titulo="Registrar pago de la siguiente cuota">
          <form className="flex flex-wrap items-end gap-3" onSubmit={(e) => { e.preventDefault(); setOk(null); pagar.mutate(); }}>
            <Campo etiqueta="Fecha del pago" type="date" name="fecha" value={fecha} onChange={(e) => setFecha(e.target.value)} />
            <Selector etiqueta="Entra a la cajita" name="cajita" value={cajita} onChange={(e) => setCajita(e.target.value)}>
              <option value="">La de origen del préstamo</option>
              {cajitas.data?.map((cj) => <option key={cj.cajitaId} value={cj.cajitaId}>{cj.nombreIntegrante}</option>)}
            </Selector>
            <Boton type="submit" disabled={pagar.isPending}>Registrar pago</Boton>
          </form>
          <div className="mt-2"><MensajeError error={pagar.error} /><Exito mensaje={ok} /></div>
        </Tarjeta>
      )}

      <Tarjeta titulo="Plan de pagos">
        <Tabla etiqueta="Plan de pagos">
          <thead><tr><th>Cuota</th><th>Vence</th><th>Capital</th><th>Interés</th><th>Valor</th><th>Estado</th></tr></thead>
          <tbody>
            {p.plan.map((q) => (
              <tr key={q.numero}>
                <td>{q.numero}</td><td>{nombreMesCiclo(c.fechaInicio, q.mesVencimiento)}</td>
                <td><Dinero valor={q.capital} /></td><td><Dinero valor={q.interes} /></td><td><Dinero valor={q.valor} /></td>
                <td className={q.pagada ? 'text-hoja-700' : q.vencida ? 'font-semibold text-red-700' : ''}>{q.pagada ? '✓ Pagada' : q.vencida ? '▲ Vencida' : 'Pendiente'}</td>
              </tr>
            ))}
          </tbody>
        </Tabla>
      </Tarjeta>

      <Tarjeta titulo="Pagos registrados">
        <MensajeError error={anular.error} />
        <Tabla etiqueta="Pagos">
          <thead><tr><th>Cuota</th><th>Fecha</th><th>Monto</th><th>Estado</th><th /></tr></thead>
          <tbody>
            {p.pagos.map((pg) => (
              <tr key={pg.id} className={pg.anulado ? 'text-stone-400' : ''}>
                <td>{pg.numeroCuota}</td><td>{formatoFecha(pg.fecha)}</td><td><Dinero valor={pg.monto} /></td>
                <td>{pg.anulado ? `Anulado: ${pg.motivoAnulacion}` : 'Vigente'}</td>
                <td>{!pg.anulado && (
                  <button className="text-xs text-red-700 underline" onClick={() => {
                    const motivo = window.prompt('Motivo de la anulación');
                    if (motivo?.trim()) anular.mutate({ pagoId: pg.id, motivo: motivo.trim() });
                  }}>Anular</button>
                )}</td>
              </tr>
            ))}
          </tbody>
        </Tabla>
      </Tarjeta>
    </div>
  );
}
