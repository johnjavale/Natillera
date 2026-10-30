'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Cargando, Dinero, MensajeError, Selector, Tabla, Tarjeta, Titulo } from '@/components/ui';
import { api } from '@/lib/api';
import { useCajitas, useCiclo, useIntegrantes } from '@/lib/consultas';
import { prestamoSchema } from '@/lib/esquemas';
import { formatoCOP, hoyISO, nombreMesCiclo } from '@/lib/formato';
import type { Prestamo } from '@/lib/tipos';

export default function NuevoPrestamo() {
  const ciclo = useCiclo();
  const integrantes = useIntegrantes();
  const cajitas = useCajitas();
  if (ciclo.isLoading || integrantes.isLoading || cajitas.isLoading) return <Cargando filas={5} />;
  if (!ciclo.data) return <MensajeError error={ciclo.error} />;
  return <Formulario maxCuotas={ciclo.data.maxCuotasPrestamo} tasa={ciclo.data.tasaInteresMensualPrestamos} fechaInicio={ciclo.data.fechaInicio}
    hoy={ciclo.data.hoy} integrantes={integrantes.data ?? []} cajitas={cajitas.data ?? []} />;
}

function Formulario({ maxCuotas, tasa, fechaInicio, hoy, integrantes, cajitas }: {
  maxCuotas: number;
  tasa: number;
  fechaInicio: string;
  hoy: string;
  integrantes: Array<{ id: number; nombre: string; activo: boolean }>;
  cajitas: Array<{ cajitaId: number; nombreIntegrante: string; saldo: number }>;
}) {
  const esquema = useMemo(() => prestamoSchema(maxCuotas), [maxCuotas]);
  type Datos = z.infer<typeof esquema>;
  const router = useRouter();
  const qc = useQueryClient();
  const [simulacion, setSimulacion] = useState<Prestamo | null>(null);
  const { register, handleSubmit, formState, getValues } = useForm<z.input<typeof esquema>, unknown, Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { fechaDesembolso: hoy || hoyISO(), numeroCuotas: 6, tasaMensual: tasa },
  });

  const simular = useMutation({ mutationFn: (d: Datos) => api.post<Prestamo>('/prestamos/simular', d), onSuccess: setSimulacion });
  const crear = useMutation({
    mutationFn: (d: Datos) => api.post<Prestamo>('/prestamos', d),
    onSuccess: (p) => {
      qc.invalidateQueries();
      router.push(`/prestamos/${p.id}`);
    },
  });

  return (
    <div className="space-y-4">
      <Titulo subtitulo={`Máximo ${maxCuotas} cuotas. El préstamo no puede terminar después del cierre ni superar el saldo de la cajita.`}>
        Nuevo préstamo
      </Titulo>
      <Tarjeta>
        <form onSubmit={handleSubmit((d) => simular.mutate(d))} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" noValidate>
          <Selector etiqueta="Integrante" {...register('integranteId')} error={formState.errors.integranteId?.message}>
            <option value="">Seleccione…</option>
            {integrantes.filter((i) => i.activo).map((i) => <option key={i.id} value={i.id}>{i.nombre}</option>)}
          </Selector>
          <Selector etiqueta="Sale de la cajita" {...register('cajitaOrigenId')} error={formState.errors.cajitaOrigenId?.message}>
            <option value="">Seleccione…</option>
            {cajitas.map((c) => <option key={c.cajitaId} value={c.cajitaId}>{c.nombreIntegrante} ({formatoCOP(c.saldo)})</option>)}
          </Selector>
          <Campo etiqueta="Fecha de desembolso" type="date" {...register('fechaDesembolso')} error={formState.errors.fechaDesembolso?.message} />
          <Campo etiqueta="Monto (COP)" type="number" min={0} step={10000} {...register('monto')} error={formState.errors.monto?.message} />
          <Campo etiqueta="Número de cuotas" type="number" min={1} max={maxCuotas} {...register('numeroCuotas')} error={formState.errors.numeroCuotas?.message} />
          <Campo etiqueta="Tasa mensual" type="number" step="0.001" min={0} max={1} {...register('tasaMensual')} ayuda="0.03 = 3 % mensual" />
          <div className="flex flex-wrap gap-2 sm:col-span-2 lg:col-span-3">
            <Boton type="submit" variante="secundario" disabled={simular.isPending}>Simular</Boton>
            <Boton type="button" disabled={!simulacion || crear.isPending} onClick={() => crear.mutate(esquema.parse(getValues()))}>
              Crear préstamo
            </Boton>
          </div>
          <div className="sm:col-span-2 lg:col-span-3"><MensajeError error={simular.error ?? crear.error} /></div>
        </form>
      </Tarjeta>
      {simulacion && (
        <Tarjeta titulo="Plan de pagos">
          <p className="mb-2 text-sm">
            Interés por cuota <Dinero valor={simulacion.interesPorCuota} /> · cuota <Dinero valor={simulacion.valorCuota} /> ·
            total intereses <Dinero valor={simulacion.totalIntereses} /> · total a pagar <strong><Dinero valor={simulacion.totalAPagar} /></strong>
          </p>
          <Tabla etiqueta="Plan de pagos simulado">
            <thead><tr><th>Cuota</th><th>Vence</th><th>Capital</th><th>Interés</th><th>Valor</th></tr></thead>
            <tbody>
              {simulacion.plan.map((c) => (
                <tr key={c.numero}><td>{c.numero}</td><td>{nombreMesCiclo(fechaInicio, c.mesVencimiento)}</td>
                  <td><Dinero valor={c.capital} /></td><td><Dinero valor={c.interes} /></td><td><Dinero valor={c.valor} /></td></tr>
              ))}
            </tbody>
          </Tabla>
          <p className="mt-2 text-xs text-stone-500">Cada cuota se redondea al peso; el residuo del capital se ajusta en la última.</p>
        </Tarjeta>
      )}
    </div>
  );
}
