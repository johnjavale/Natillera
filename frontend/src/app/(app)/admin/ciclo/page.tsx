'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Cargando, Exito, MensajeError, Tarjeta, Titulo } from '@/components/ui';
import { api } from '@/lib/api';
import { useCiclo } from '@/lib/consultas';
import { cicloSchema } from '@/lib/esquemas';
import { formatoFecha, formatoPorcentaje } from '@/lib/formato';
import type { Ciclo } from '@/lib/tipos';

type Datos = z.infer<typeof cicloSchema>;

export default function Configuracion() {
  const ciclo = useCiclo();
  if (ciclo.isLoading) return <Cargando filas={6} />;
  if (!ciclo.data) return <MensajeError error={ciclo.error} />;
  return <Formulario ciclo={ciclo.data} />;
}

function Formulario({ ciclo }: { ciclo: Ciclo }) {
  const qc = useQueryClient();
  const [ok, setOk] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<z.input<typeof cicloSchema>, unknown, Datos>({ resolver: zodResolver(cicloSchema), defaultValues: ciclo });
  const guardar = useMutation({
    mutationFn: (d: Datos) => api.put<Ciclo>('/ciclo', d),
    onSuccess: () => { setOk('Configuración guardada'); qc.invalidateQueries(); },
  });
  const estado = useMutation({
    mutationFn: (e: Ciclo['estado']) => api.put<Ciclo>('/ciclo/estado', { estado: e }),
    onSuccess: () => qc.invalidateQueries(),
  });
  const cerrado = ciclo.estado === 'CERRADO';

  return (
    <div className="space-y-4">
      <Titulo subtitulo={`Cierre: ${formatoFecha(ciclo.fechaCierre)} · tasa mensual Nu equivalente ${formatoPorcentaje(ciclo.tasaMensualNu)}`}>
        Configuración del ciclo
      </Titulo>
      <Tarjeta titulo={`Estado: ${ciclo.estado}`}>
        <p className="mb-3 text-sm text-stone-600">En inscripción cada integrante puede proponer su cuota. En activo, solo el administrador la cambia. Cerrado: no se crean préstamos ni rifas.</p>
        <div className="flex flex-wrap gap-2">
          <Boton variante="secundario" disabled={cerrado || ciclo.estado === 'INSCRIPCION'} onClick={() => estado.mutate('INSCRIPCION')}>Abrir inscripción</Boton>
          <Boton variante="secundario" disabled={cerrado || ciclo.estado === 'ACTIVO'} onClick={() => estado.mutate('ACTIVO')}>Iniciar ciclo</Boton>
          <Boton variante="peligro" disabled={cerrado} onClick={() => window.confirm('¿Cerrar el ciclo? No se puede deshacer.') && estado.mutate('CERRADO')}>Cerrar ciclo</Boton>
        </div>
        <div className="mt-2"><MensajeError error={estado.error} /></div>
      </Tarjeta>
      <Tarjeta titulo="Parámetros">
        <form onSubmit={handleSubmit((d) => { setOk(null); guardar.mutate(d); })} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" noValidate>
          <Campo etiqueta="Nombre de la natillera" {...register('nombre')} error={formState.errors.nombre?.message} />
          <Campo etiqueta="Fecha de inicio" type="date" {...register('fechaInicio')} />
          <Campo etiqueta="Número de meses" type="number" min={1} max={12} {...register('numeroMeses')} error={formState.errors.numeroMeses?.message} />
          <Campo etiqueta="Tasa de interés mensual de préstamos" type="number" step="0.001" {...register('tasaInteresMensualPrestamos')} ayuda="0.03 = 3 %" />
          <Campo etiqueta="Gastos administrativos del año" type="number" min={0} {...register('gastosAdministrativos')} />
          <Campo etiqueta="Máximo de cuotas por préstamo" type="number" min={1} max={12} {...register('maxCuotasPrestamo')} />
          <Campo etiqueta="Tasa E.A. de las cajitas Nu" type="number" step="0.001" {...register('tasaEACajitasNu')} ayuda="Revise la tasa vigente en la app de Nu" />
          <Campo etiqueta="Máximo de rifas" type="number" min={0} max={12} {...register('maxActividades')} />
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...register('permitirPrestamoDespuesDelCierre')} /> Permitir préstamos que terminan después del cierre
          </label>
          <div className="sm:col-span-2 lg:col-span-3">
            <MensajeError error={guardar.error} />
            <Exito mensaje={ok} />
            <Boton type="submit" className="mt-2" disabled={cerrado || guardar.isPending}>Guardar</Boton>
          </div>
        </form>
      </Tarjeta>
    </div>
  );
}
