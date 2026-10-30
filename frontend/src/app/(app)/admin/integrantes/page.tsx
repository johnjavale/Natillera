'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Cargando, Dinero, Exito, MensajeError, Selector, Tabla, Tarjeta, Titulo } from '@/components/ui';
import { api } from '@/lib/api';
import { useIntegrantes } from '@/lib/consultas';
import { integranteSchema } from '@/lib/esquemas';
import { formatoPorcentaje } from '@/lib/formato';
import type { Integrante } from '@/lib/tipos';

type Datos = z.infer<typeof integranteSchema>;

export default function Integrantes() {
  const integrantes = useIntegrantes();
  const [editando, setEditando] = useState<Integrante | 'nuevo' | null>(null);
  const total = (integrantes.data ?? []).filter((i) => i.activo);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <Titulo subtitulo="Cuota, tesorero que guarda su cajita y ahorro esperado.">Integrantes</Titulo>
        <Boton onClick={() => setEditando('nuevo')}>Nuevo integrante</Boton>
      </div>
      {editando && <Formulario integrante={editando === 'nuevo' ? null : editando} alCerrar={() => setEditando(null)} />}
      <Tarjeta>
        {integrantes.isLoading ? <Cargando /> : integrantes.error ? <MensajeError error={integrantes.error} /> : (
          <Tabla etiqueta="Integrantes">
            <thead><tr><th>#</th><th>Nombre</th><th>Cuota</th><th>Ahorro esperado</th><th>% del total</th><th>Tesorero</th><th>Cajita Nu</th><th>Rend. proyectado*</th><th>Saldo</th><th /></tr></thead>
            <tbody>
              {integrantes.data?.map((i) => (
                <tr key={i.id} className={i.activo ? '' : 'text-stone-400'}>
                  <td>{i.id}</td><td>{i.nombre}{!i.activo && ' (inactivo)'}</td>
                  <td><Dinero valor={i.cuotaMensual} /></td><td><Dinero valor={i.ahorroEsperado} /></td>
                  <td>{formatoPorcentaje(i.porcentajeDelTotal)}</td><td>{i.nombreTesorero ?? '—'}</td>
                  <td>{i.nombreCajitaNu}</td><td><Dinero valor={i.rendimientoProyectadoCajita} /></td>
                  <td><Dinero valor={i.saldoCajita} /></td>
                  <td><button className="text-xs text-marca-700 underline" onClick={() => setEditando(i)}>Editar</button></td>
                </tr>
              ))}
              <tr className="font-semibold">
                <td /><td>TOTAL</td>
                <td><Dinero valor={total.reduce((s, i) => s + i.cuotaMensual, 0)} /></td>
                <td><Dinero valor={total.reduce((s, i) => s + i.ahorroEsperado, 0)} /></td>
                <td colSpan={3} />
                <td><Dinero valor={total.reduce((s, i) => s + i.rendimientoProyectadoCajita, 0)} /></td>
                <td colSpan={2} />
              </tr>
            </tbody>
          </Tabla>
        )}
        <p className="mt-2 text-xs text-stone-500">* Estimado: cada cuota gana la tasa mensual de Nu los meses que faltan. Si se saca plata de la cajita para préstamos, el rendimiento real baja.</p>
      </Tarjeta>
    </div>
  );
}

function Formulario({ integrante, alCerrar }: { integrante: Integrante | null; alCerrar: () => void }) {
  const qc = useQueryClient();
  const [ok, setOk] = useState<string | null>(null);
  const tesoreros = useQuery({ queryKey: ['tesoreros'], queryFn: () => api.get<Array<{ id: number; nombre: string }>>('/tesoreria/tesoreros') });
  const { register, handleSubmit, formState } = useForm<z.input<typeof integranteSchema>, unknown, Datos>({
    resolver: zodResolver(integranteSchema),
    defaultValues: integrante ? {
      nombre: integrante.nombre, telefono: integrante.telefono ?? '', cuotaMensual: integrante.cuotaMensual,
      tesoreroId: integrante.tesoreroId ?? undefined, nombreCajitaNu: integrante.nombreCajitaNu, activo: integrante.activo,
    } : { activo: true, cuotaMensual: 0 },
  });
  const guardar = useMutation({
    mutationFn: (d: Datos) => integrante ? api.put(`/integrantes/${integrante.id}`, d) : api.post('/integrantes', d),
    onSuccess: () => { setOk('Guardado (los cambios de cuota quedan en auditoría)'); qc.invalidateQueries(); },
  });
  return (
    <Tarjeta titulo={integrante ? `Editar ${integrante.nombre}` : 'Nuevo integrante'} acciones={<Boton variante="secundario" onClick={alCerrar}>Cerrar</Boton>}>
      <form onSubmit={handleSubmit((d) => guardar.mutate(d))} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" noValidate>
        <Campo etiqueta="Nombre" {...register('nombre')} error={formState.errors.nombre?.message} />
        <Campo etiqueta="Teléfono" {...register('telefono')} error={formState.errors.telefono?.message} />
        <Campo etiqueta="Cuota mensual" type="number" min={0} step={1000} {...register('cuotaMensual')} error={formState.errors.cuotaMensual?.message} />
        <Selector etiqueta="Tesorero que guarda su cajita" {...register('tesoreroId')}>
          <option value="">Sin asignar</option>
          {tesoreros.data?.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
        </Selector>
        <Campo etiqueta="Nombre de la cajita en Nu" {...register('nombreCajitaNu')} ayuda='Por defecto "Natillera - Nombre"' />
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" {...register('activo')} /> Activo</label>
        <div className="sm:col-span-2 lg:col-span-3">
          <MensajeError error={guardar.error} />
          <Exito mensaje={ok} />
          <Boton type="submit" className="mt-2" disabled={guardar.isPending}>Guardar</Boton>
        </div>
      </form>
    </Tarjeta>
  );
}
