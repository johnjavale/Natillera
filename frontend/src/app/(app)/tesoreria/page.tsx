'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Boton, Campo, Cargando, Dinero, Exito, MensajeError, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api } from '@/lib/api';
import { useCajitas } from '@/lib/consultas';
import { formatoFecha, hoyISO } from '@/lib/formato';
import { saldoReportadoSchema } from '@/lib/esquemas';
import type { Cajita, ResumenTesoreria } from '@/lib/tipos';

export default function Tesoreria() {
  const cajitas = useCajitas();
  const resumen = useQuery({ queryKey: ['tesoreria-resumen'], queryFn: () => api.get<ResumenTesoreria>('/tesoreria/resumen') });
  const [conciliando, setConciliando] = useState<Cajita | null>(null);

  return (
    <div className="space-y-4">
      <Titulo subtitulo="Saldo según el registro de movimientos vs. lo que muestra la app de Nu.">Cajitas y conciliación</Titulo>
      {resumen.data && (
        <div className="grid gap-3 sm:grid-cols-2">
          {resumen.data.porTesorero.map((t) => (
            <Tarjeta key={t.tesoreroId} titulo={`Cuenta Nu de ${t.nombre}`}>
              <dl className="grid grid-cols-2 gap-1 text-sm">
                <dt className="text-stone-600">Cajitas</dt><dd>{t.numeroCajitas}</dd>
                <dt className="text-stone-600">Saldo según registro</dt><dd><Dinero valor={t.saldoRegistro} /></dd>
                <dt className="text-stone-600">Saldo app Nu</dt><dd><Dinero valor={t.saldoApp} /></dd>
                <dt className="text-stone-600">Diferencia</dt><dd><Dinero valor={t.diferencia} alerta={t.diferencia !== 0} /></dd>
              </dl>
            </Tarjeta>
          ))}
        </div>
      )}
      <Tarjeta titulo="Cajitas">
        {cajitas.isLoading ? <Cargando /> : cajitas.error ? <MensajeError error={cajitas.error} /> : !cajitas.data?.length ? (
          <Vacio mensaje="No tiene cajitas asignadas." />
        ) : (
          <Tabla etiqueta="Cajitas">
            <thead><tr><th>Integrante</th><th>Cajita</th><th>Tesorero</th><th>Saldo registro</th><th>Saldo app Nu</th><th>Diferencia</th><th /></tr></thead>
            <tbody>
              {cajitas.data.map((c) => (
                <tr key={c.cajitaId}>
                  <td>{c.nombreIntegrante}</td>
                  <td>{c.nombreCajita}</td>
                  <td>{c.nombreTesorero ?? '—'}</td>
                  <td><Dinero valor={c.saldo} alerta={c.saldo < 0} /></td>
                  <td>{c.saldoApp === null ? '—' : <><Dinero valor={c.saldoApp} /> <span className="text-xs text-stone-500">({formatoFecha(c.fechaSaldoApp)})</span></>}</td>
                  <td><Dinero valor={c.diferencia} alerta={!c.cuadra} /></td>
                  <td><Boton variante="secundario" className="min-h-8 px-2 py-1 text-xs" onClick={() => setConciliando(c)}>Reportar saldo Nu</Boton></td>
                </tr>
              ))}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
      {conciliando && <ReportarSaldo cajita={conciliando} alCerrar={() => setConciliando(null)} />}
    </div>
  );
}

function ReportarSaldo({ cajita, alCerrar }: { cajita: Cajita; alCerrar: () => void }) {
  const qc = useQueryClient();
  const [fecha, setFecha] = useState(hoyISO());
  const [saldo, setSaldo] = useState(cajita.saldoApp === null ? '' : String(cajita.saldoApp));
  const [errorLocal, setErrorLocal] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);
  const m = useMutation({
    mutationFn: (datos: { fecha: string; saldoApp: number }) => api.post(`/tesoreria/cajitas/${cajita.cajitaId}/saldo-reportado`, datos),
    onSuccess: () => {
      setOk('Saldo registrado');
      qc.invalidateQueries({ queryKey: ['cajitas'] });
      qc.invalidateQueries({ queryKey: ['tesoreria-resumen'] });
    },
  });
  const enviar = (ev: React.FormEvent) => {
    ev.preventDefault();
    const r = saldoReportadoSchema.safeParse({ fecha, saldoApp: saldo });
    if (!r.success) return setErrorLocal(r.error.issues[0]?.message ?? 'Datos inválidos');
    setErrorLocal(null);
    m.mutate(r.data);
  };
  return (
    <Tarjeta titulo={`Saldo en la app Nu · ${cajita.nombreCajita}`} acciones={<Boton variante="secundario" onClick={alCerrar}>Cerrar</Boton>}>
      <form onSubmit={enviar} className="flex flex-wrap items-end gap-3">
        <Campo etiqueta="Fecha" type="date" name="fecha" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        <Campo etiqueta="Saldo que muestra la app" type="number" name="saldoApp" min={0} value={saldo} onChange={(e) => setSaldo(e.target.value)} error={errorLocal ?? undefined} />
        <Boton type="submit" disabled={m.isPending}>Guardar</Boton>
      </form>
      <div className="mt-2"><MensajeError error={m.error} /><Exito mensaje={ok} /></div>
    </Tarjeta>
  );
}
