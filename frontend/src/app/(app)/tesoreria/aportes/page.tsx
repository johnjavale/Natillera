'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Boton, Campo, Cargando, Dinero, Exito, MensajeError, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api } from '@/lib/api';
import { useCajitas, useIntegrantes } from '@/lib/consultas';
import { aportesLoteSchema } from '@/lib/esquemas';
import { hoyISO } from '@/lib/formato';

/** Registro rápido en lote de los aportes del mes para los integrantes del tesorero. */
export default function AportesDelMes() {
  const integrantes = useIntegrantes();
  const cajitas = useCajitas();
  const qc = useQueryClient();
  const [fecha, setFecha] = useState(hoyISO());
  const [descripcion, setDescripcion] = useState('Cuota del mes');
  const [montos, setMontos] = useState<Record<number, string>>({});
  const [errorLocal, setErrorLocal] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  // Solo se ofrecen los integrantes cuya cajita administra el usuario
  const administrados = new Set((cajitas.data ?? []).map((c) => c.integranteId));
  const lista = (integrantes.data ?? []).filter((i) => i.activo && administrados.has(i.id));

  useEffect(() => {
    if (integrantes.data) {
      setMontos(Object.fromEntries(integrantes.data.map((i) => [i.id, String(i.cuotaMensual)])));
    }
  }, [integrantes.data]);

  const m = useMutation({
    mutationFn: (cuerpo: unknown) => api.post<unknown[]>('/tesoreria/aportes-lote', cuerpo),
    onSuccess: (r) => {
      setOk(`Se registraron ${r.length} aportes`);
      qc.invalidateQueries();
    },
  });

  const enviar = (ev: React.FormEvent) => {
    ev.preventDefault();
    setOk(null);
    const r = aportesLoteSchema.safeParse({
      fecha,
      descripcion,
      aportes: lista.map((i) => ({ integranteId: i.id, monto: montos[i.id] ?? '0' })),
    });
    if (!r.success) return setErrorLocal(r.error.issues[0]?.message ?? 'Datos inválidos');
    setErrorLocal(null);
    m.mutate(r.data);
  };

  return (
    <div className="space-y-4">
      <Titulo subtitulo="Escriba lo que cada persona aportó. Deje en 0 a quien no aportó.">Aportes del mes</Titulo>
      <Tarjeta>
        {integrantes.isLoading || cajitas.isLoading ? <Cargando /> : lista.length === 0 ? <Vacio mensaje="No tiene integrantes asignados." /> : (
          <form onSubmit={enviar} className="space-y-4">
            <div className="flex flex-wrap gap-3">
              <Campo etiqueta="Fecha" type="date" name="fecha" value={fecha} onChange={(e) => setFecha(e.target.value)} />
              <Campo etiqueta="Descripción" name="descripcion" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
            </div>
            <Tabla etiqueta="Aportes por integrante">
              <thead><tr><th>Integrante</th><th>Cuota</th><th>Aporte</th></tr></thead>
              <tbody>
                {lista.map((i) => (
                  <tr key={i.id}>
                    <td><label htmlFor={`aporte-${i.id}`}>{i.nombre}</label></td>
                    <td><Dinero valor={i.cuotaMensual} /></td>
                    <td>
                      <input id={`aporte-${i.id}`} type="number" min={0} step={1000} inputMode="numeric"
                        className="w-36 rounded-lg border border-stone-300 px-2 py-1"
                        value={montos[i.id] ?? ''} onChange={(e) => setMontos({ ...montos, [i.id]: e.target.value })} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </Tabla>
            {errorLocal && <p role="alert" className="text-sm text-red-700">{errorLocal}</p>}
            <MensajeError error={m.error} />
            <Exito mensaje={ok} />
            <Boton type="submit" disabled={m.isPending}>{m.isPending ? 'Guardando…' : 'Registrar aportes'}</Boton>
          </form>
        )}
      </Tarjeta>
    </div>
  );
}
