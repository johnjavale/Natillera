'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Cargando, Dinero, Exito, MensajeError, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api } from '@/lib/api';
import { useSesion } from '@/lib/consultas';
import { actividadSchema } from '@/lib/esquemas';
import { formatoFecha, hoyISO } from '@/lib/formato';
import type { Actividad } from '@/lib/tipos';

type DatosActividad = z.infer<typeof actividadSchema>;

export default function Actividades() {
  const sesion = useSesion();
  const actividades = useQuery({ queryKey: ['actividades'], queryFn: () => api.get<Actividad[]>('/actividades') });
  const roles = sesion.data?.roles ?? [];
  const esAdmin = roles.includes('ADMIN');
  const gestiona = esAdmin || roles.includes('TESORERO');
  const [editando, setEditando] = useState<Actividad | 'nueva' | null>(null);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <Titulo subtitulo="Máximo 2 rifas por ciclo. La utilidad se reparte en partes iguales.">Rifas</Titulo>
        {esAdmin && <Boton onClick={() => setEditando('nueva')}>Nueva rifa</Boton>}
      </div>
      {editando && <FormularioActividad actividad={editando === 'nueva' ? null : editando} alCerrar={() => setEditando(null)} />}
      {actividades.isLoading ? <Cargando /> : actividades.error ? <MensajeError error={actividades.error} /> : !actividades.data?.length ? (
        <Vacio mensaje="Aún no hay rifas." />
      ) : actividades.data.map((a) => (
        <Tarjeta key={a.id} titulo={`${a.nombre} · sorteo ${formatoFecha(a.fechaSorteo)} (mes ${a.mesDeLaActividad})`}
          acciones={esAdmin && <Boton variante="secundario" onClick={() => setEditando(a)}>Editar</Boton>}>
          <dl className="mb-3 grid grid-cols-2 gap-2 text-sm sm:grid-cols-4">
            <div><dt className="text-stone-600">Boleta</dt><dd><Dinero valor={a.valorBoleta} /> × {a.boletasPorIntegrante}</dd></div>
            <div><dt className="text-stone-600">Costos</dt><dd><Dinero valor={a.costos} /></dd></div>
            <div><dt className="text-stone-600">Utilidad esperada</dt><dd><Dinero valor={a.utilidadEsperada} /> (<Dinero valor={a.utilidadEsperadaPorIntegrante} /> c/u)</dd></div>
            <div><dt className="text-stone-600">Utilidad real</dt><dd><Dinero valor={a.utilidadReal} alerta={a.utilidadReal < 0} /></dd></div>
            <div><dt className="text-stone-600">Recaudo esperado</dt><dd><Dinero valor={a.recaudoEsperado} /></dd></div>
            <div><dt className="text-stone-600">Recaudo real</dt><dd><Dinero valor={a.recaudoReal} /></dd></div>
            <div><dt className="text-stone-600">Ganador</dt><dd>{a.ganador || '—'}</dd></div>
          </dl>
          <Tabla etiqueta={`Boletas de ${a.nombre}`}>
            <thead><tr><th>Integrante</th><th>Boletas</th><th>A pagar</th><th>Pagado</th><th>Pendiente</th>{gestiona && <th />}</tr></thead>
            <tbody>
              {a.boletas.map((b) => (
                <FilaBoletas key={b.integranteId} actividad={a} fila={b} gestiona={gestiona} esAdmin={esAdmin} />
              ))}
            </tbody>
          </Tabla>
        </Tarjeta>
      ))}
    </div>
  );
}

function FilaBoletas({ actividad, fila, gestiona, esAdmin }: {
  actividad: Actividad;
  fila: Actividad['boletas'][number];
  gestiona: boolean;
  esAdmin: boolean;
}) {
  const qc = useQueryClient();
  const pagar = useMutation({
    mutationFn: (monto: number) => api.post(`/actividades/${actividad.id}/pagos`, { integranteId: fila.integranteId, fecha: hoyISO(), monto }),
    onSuccess: () => qc.invalidateQueries(),
    onError: (e) => window.alert(e.message),
  });
  const boletas = useMutation({
    mutationFn: (n: number) => api.put(`/actividades/${actividad.id}/boletas/${fila.integranteId}`, { boletas: n }),
    onSuccess: () => qc.invalidateQueries(),
    onError: (e) => window.alert(e.message),
  });
  return (
    <tr>
      <td>{fila.nombre}</td>
      <td>
        {esAdmin ? (
          <button className="underline" aria-label={`Cambiar boletas de ${fila.nombre}`} onClick={() => {
            const n = window.prompt('Número de boletas', String(fila.boletas));
            if (n !== null && /^\d+$/.test(n)) boletas.mutate(Number(n));
          }}>{fila.boletas}</button>
        ) : fila.boletas}
      </td>
      <td><Dinero valor={fila.valorAPagar} /></td>
      <td><Dinero valor={fila.pagado} /></td>
      <td><Dinero valor={fila.pendiente} alerta={fila.pendiente > 0} /></td>
      {gestiona && (
        <td>
          {fila.pendiente > 0 && (
            <button className="text-xs text-marca-700 underline" disabled={pagar.isPending} onClick={() => {
              const v = window.prompt('Valor pagado (COP)', String(fila.pendiente));
              if (v && Number(v) > 0) pagar.mutate(Number(v));
            }}>Registrar pago</button>
          )}
        </td>
      )}
    </tr>
  );
}

function FormularioActividad({ actividad, alCerrar }: { actividad: Actividad | null; alCerrar: () => void }) {
  const qc = useQueryClient();
  const [ok, setOk] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<z.input<typeof actividadSchema>, unknown, DatosActividad>({
    resolver: zodResolver(actividadSchema),
    defaultValues: actividad ? {
      nombre: actividad.nombre, fechaSorteo: actividad.fechaSorteo, valorBoleta: actividad.valorBoleta,
      boletasPorIntegrante: actividad.boletasPorIntegrante, costoPremio: actividad.costoPremio,
      otrosGastos: actividad.otrosGastos, ganador: actividad.ganador ?? '',
    } : { fechaSorteo: hoyISO(), boletasPorIntegrante: 20, costoPremio: 0, otrosGastos: 0 },
  });
  const guardar = useMutation({
    mutationFn: (d: DatosActividad) => actividad ? api.put(`/actividades/${actividad.id}`, d) : api.post('/actividades', d),
    onSuccess: () => {
      setOk('Guardado');
      qc.invalidateQueries();
    },
  });
  return (
    <Tarjeta titulo={actividad ? `Editar ${actividad.nombre}` : 'Nueva rifa'} acciones={<Boton variante="secundario" onClick={alCerrar}>Cerrar</Boton>}>
      <form onSubmit={handleSubmit((d) => guardar.mutate(d))} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" noValidate>
        <Campo etiqueta="Nombre" {...register('nombre')} error={formState.errors.nombre?.message} />
        <Campo etiqueta="Fecha del sorteo" type="date" {...register('fechaSorteo')} error={formState.errors.fechaSorteo?.message} />
        <Campo etiqueta="Valor de la boleta" type="number" min={0} {...register('valorBoleta')} />
        <Campo etiqueta="Boletas por integrante" type="number" min={0} {...register('boletasPorIntegrante')} />
        <Campo etiqueta="Costo del premio" type="number" min={0} {...register('costoPremio')} />
        <Campo etiqueta="Otros gastos" type="number" min={0} {...register('otrosGastos')} />
        <Campo etiqueta="Ganador (nombre / número)" {...register('ganador')} />
        <div className="sm:col-span-2 lg:col-span-4">
          <MensajeError error={guardar.error} />
          <Exito mensaje={ok} />
          <Boton type="submit" className="mt-2" disabled={guardar.isPending}>Guardar</Boton>
        </div>
      </form>
    </Tarjeta>
  );
}
