'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Cargando, Exito, MensajeError, Selector, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api, urlDescarga } from '@/lib/api';
import { useCajitas } from '@/lib/consultas';
import { movimientoSchema, TIPOS_MANUALES } from '@/lib/esquemas';
import { formatoCOP, formatoFecha, hoyISO } from '@/lib/formato';
import type { Movimiento, TipoMovimiento } from '@/lib/tipos';

type Datos = z.infer<typeof movimientoSchema>;

const TODOS_LOS_TIPOS: Array<{ valor: TipoMovimiento; etiqueta: string }> = [
  ...TIPOS_MANUALES,
  { valor: 'PAGO_CUOTA_PRESTAMO', etiqueta: 'Pago cuota préstamo' },
  { valor: 'PAGO_BOLETAS_RIFA', etiqueta: 'Pago boletas rifa' },
  { valor: 'DESEMBOLSO_PRESTAMO', etiqueta: 'Desembolso préstamo' },
];

export default function Movimientos() {
  const cajitas = useCajitas();
  const qc = useQueryClient();
  const [filtro, setFiltro] = useState({ cajitaId: '', tipo: '', desde: '', hasta: '' });
  const query = new URLSearchParams(Object.entries(filtro).filter(([, v]) => v)).toString();
  const movimientos = useQuery({
    queryKey: ['movimientos', query],
    queryFn: () => api.get<Movimiento[]>(`/tesoreria/movimientos${query ? `?${query}` : ''}`),
  });
  const [ok, setOk] = useState<string | null>(null);
  const { register, handleSubmit, formState, reset } = useForm<z.input<typeof movimientoSchema>, unknown, Datos>({
    resolver: zodResolver(movimientoSchema),
    defaultValues: { fecha: hoyISO(), tipo: 'AHORRO' },
  });
  const crear = useMutation({
    mutationFn: (d: Datos) => api.post('/tesoreria/movimientos', d),
    onSuccess: () => {
      setOk('Movimiento registrado');
      reset({ fecha: hoyISO(), tipo: 'AHORRO' });
      qc.invalidateQueries();
    },
  });
  const anular = useMutation({
    mutationFn: ({ id, motivo }: { id: number; motivo: string }) => api.post(`/tesoreria/movimientos/${id}/anular`, { motivo }),
    onSuccess: () => qc.invalidateQueries(),
  });

  const pedirAnulacion = (m: Movimiento) => {
    const motivo = window.prompt(`Motivo para anular "${m.descripcion ?? m.tipoEtiqueta}" por ${formatoCOP(m.entrada || m.salida)}`);
    if (motivo && motivo.trim()) anular.mutate({ id: m.id, motivo: motivo.trim() });
  };

  return (
    <div className="space-y-4">
      <Titulo subtitulo="El libro de movimientos es la fuente única del dinero. Los movimientos no se borran: se anulan con un contramovimiento.">
        Movimientos de tesorería
      </Titulo>

      <Tarjeta titulo="Registrar movimiento">
        <form onSubmit={handleSubmit((d) => { setOk(null); crear.mutate(d); })} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5" noValidate>
          <Selector etiqueta="Cajita" {...register('cajitaId')} error={formState.errors.cajitaId?.message}>
            <option value="">Seleccione…</option>
            {cajitas.data?.map((c) => <option key={c.cajitaId} value={c.cajitaId}>{c.nombreIntegrante}</option>)}
          </Selector>
          <Campo etiqueta="Fecha" type="date" {...register('fecha')} error={formState.errors.fecha?.message} />
          <Selector etiqueta="Tipo" {...register('tipo')}>
            {TIPOS_MANUALES.map((t) => <option key={t.valor} value={t.valor}>{t.etiqueta}</option>)}
          </Selector>
          <Campo etiqueta="Descripción" {...register('descripcion')} />
          <Campo etiqueta="Valor (COP)" type="number" min={0} {...register('valor')} error={formState.errors.valor?.message} />
          <div className="sm:col-span-2 lg:col-span-5">
            <MensajeError error={crear.error} />
            <Exito mensaje={ok} />
            <Boton type="submit" className="mt-2" disabled={crear.isPending}>Registrar</Boton>
          </div>
        </form>
      </Tarjeta>

      <Tarjeta titulo="Libro de movimientos" acciones={
        <span className="flex gap-3 text-sm">
          <a className="text-marca-700 underline" href={urlDescarga(`/reportes/tesoreria${query ? `?${query}` : ''}`, 'xlsx')}>Excel</a>
          <a className="text-marca-700 underline" href={urlDescarga(`/reportes/tesoreria${query ? `?${query}` : ''}`, 'csv')}>CSV</a>
        </span>
      }>
        <div className="mb-3 grid gap-2 sm:grid-cols-4">
          <Selector etiqueta="Cajita" name="f-cajita" value={filtro.cajitaId} onChange={(e) => setFiltro({ ...filtro, cajitaId: e.target.value })}>
            <option value="">Todas</option>
            {cajitas.data?.map((c) => <option key={c.cajitaId} value={c.cajitaId}>{c.nombreIntegrante}</option>)}
          </Selector>
          <Selector etiqueta="Tipo" name="f-tipo" value={filtro.tipo} onChange={(e) => setFiltro({ ...filtro, tipo: e.target.value })}>
            <option value="">Todos</option>
            {TODOS_LOS_TIPOS.map((t) => <option key={t.valor} value={t.valor}>{t.etiqueta}</option>)}
          </Selector>
          <Campo etiqueta="Desde" type="date" name="f-desde" value={filtro.desde} onChange={(e) => setFiltro({ ...filtro, desde: e.target.value })} />
          <Campo etiqueta="Hasta" type="date" name="f-hasta" value={filtro.hasta} onChange={(e) => setFiltro({ ...filtro, hasta: e.target.value })} />
        </div>
        <MensajeError error={anular.error} />
        {movimientos.isLoading ? <Cargando /> : movimientos.error ? <MensajeError error={movimientos.error} /> : !movimientos.data?.length ? (
          <Vacio mensaje="No hay movimientos con esos filtros." />
        ) : (
          <Tabla etiqueta="Movimientos">
            <thead><tr><th>Fecha</th><th>Cajita de</th><th>Tesorero</th><th>Tipo</th><th>Descripción</th><th>Entrada (+)</th><th>Salida (−)</th><th /></tr></thead>
            <tbody>
              {movimientos.data.map((m) => {
                const anulado = Boolean(m.anuladoPor || m.anulaA);
                return (
                  <tr key={m.id} className={anulado ? 'text-stone-400 line-through decoration-stone-300' : ''}>
                    <td>{formatoFecha(m.fecha)}</td>
                    <td>{m.nombreIntegrante}</td>
                    <td>{m.nombreTesorero ?? '—'}</td>
                    <td>{m.tipoEtiqueta}</td>
                    <td className="max-w-xs truncate">{m.descripcion}</td>
                    <td className="tabular-nums">{m.entrada > 0 ? formatoCOP(m.entrada) : ''}</td>
                    <td className="tabular-nums">{m.salida > 0 ? formatoCOP(m.salida) : ''}</td>
                    <td>
                      {!anulado && !m.prestamoId && (
                        <button className="text-xs text-red-700 underline" onClick={() => pedirAnulacion(m)}>Anular</button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
    </div>
  );
}
