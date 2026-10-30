'use client';

import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Cargando, Dinero, Estado, MensajeError, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api } from '@/lib/api';
import { useCiclo } from '@/lib/consultas';
import { formatoFecha, nombreMesCiclo } from '@/lib/formato';
import type { Prestamo, ProyeccionIntereses } from '@/lib/tipos';

export default function Prestamos() {
  const ciclo = useCiclo();
  const prestamos = useQuery({ queryKey: ['prestamos'], queryFn: () => api.get<Prestamo[]>('/prestamos') });
  const proyeccion = useQuery({ queryKey: ['proyeccion'], queryFn: () => api.get<ProyeccionIntereses>('/prestamos/proyeccion-intereses') });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <Titulo subtitulo="Interés simple sobre el monto prestado. La primera cuota se paga el mes siguiente a la entrega.">Préstamos</Titulo>
        <Link href="/prestamos/nuevo" className="rounded-lg bg-marca-600 px-4 py-2 text-sm font-medium text-white hover:bg-marca-700">Nuevo préstamo</Link>
      </div>
      <Tarjeta>
        {prestamos.isLoading ? <Cargando /> : prestamos.error ? <MensajeError error={prestamos.error} /> : !prestamos.data?.length ? (
          <Vacio mensaje="No hay préstamos registrados." />
        ) : (
          <Tabla etiqueta="Préstamos">
            <thead><tr><th>#</th><th>Integrante</th><th>Fecha</th><th>Monto</th><th>Cuotas</th><th>Valor cuota</th><th>Pagadas</th><th>Saldo</th><th>Vencidas</th><th>Estado</th></tr></thead>
            <tbody>
              {prestamos.data.map((p) => (
                <tr key={p.id}>
                  <td><Link className="text-marca-700 underline" href={`/prestamos/${p.id}`}>{p.id}</Link></td>
                  <td>{p.nombreIntegrante}</td>
                  <td>{formatoFecha(p.fechaDesembolso)}</td>
                  <td><Dinero valor={p.monto} /></td>
                  <td>{p.numeroCuotas}</td>
                  <td><Dinero valor={p.valorCuota} /></td>
                  <td>{p.cuotasPagadas}</td>
                  <td><Dinero valor={p.saldoPendiente} /></td>
                  <td className={p.cuotasVencidas > 0 ? 'font-semibold text-red-700' : ''}>{p.cuotasVencidas}</td>
                  <td><Estado estado={p.estado} /></td>
                </tr>
              ))}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
      {proyeccion.data && ciclo.data && proyeccion.data.filas.length > 0 && (
        <Tarjeta titulo="Proyección de intereses por mes">
          <Tabla etiqueta="Proyección de intereses">
            <thead>
              <tr><th>Préstamo</th>{proyeccion.data.totalesPorMes.map((_, i) => <th key={i}>{nombreMesCiclo(ciclo.data!.fechaInicio, i + 1)}</th>)}</tr>
            </thead>
            <tbody>
              {proyeccion.data.filas.map((f) => (
                <tr key={f.prestamoId}>
                  <td>#{f.prestamoId} {f.nombreIntegrante}</td>
                  {f.interesesPorMes.map((v, i) => <td key={i}>{v > 0 ? <Dinero valor={v} /> : ''}</td>)}
                </tr>
              ))}
              <tr className="font-semibold">
                <td>Total ({<Dinero valor={proyeccion.data.total} />})</td>
                {proyeccion.data.totalesPorMes.map((v, i) => <td key={i}><Dinero valor={v} /></td>)}
              </tr>
            </tbody>
          </Tabla>
        </Tarjeta>
      )}
    </div>
  );
}
