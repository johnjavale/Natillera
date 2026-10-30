'use client';

import { useQuery } from '@tanstack/react-query';
import dynamic from 'next/dynamic';
import { Cargando, Dinero, Kpi, MensajeError, Tabla, Tarjeta, Titulo } from '@/components/ui';
import { api, urlDescarga } from '@/lib/api';
import { useCiclo } from '@/lib/consultas';
import { formatoCOP, formatoPorcentaje, nombreMesCiclo } from '@/lib/formato';
import type { Resumen } from '@/lib/tipos';

// Recharts solo en el cliente y en un chunk aparte (no carga en el resto de páginas)
const Graficas = dynamic(() => import('./graficas-resumen'), { ssr: false, loading: () => <Cargando filas={4} /> });

export default function Dashboard() {
  const ciclo = useCiclo();
  const resumen = useQuery({ queryKey: ['resumen'], queryFn: () => api.get<Resumen>('/resumen') });
  if (resumen.isLoading || ciclo.isLoading) return <Cargando filas={8} />;
  if (resumen.error) return <MensajeError error={resumen.error} />;
  const r = resumen.data;
  const c = ciclo.data;
  if (!r || !c) return null;
  const k = r.kpis;

  return (
    <div className="space-y-4">
      <Titulo subtitulo={`${c.nombre} · ${c.numeroIntegrantes} integrantes · mes ${r.mesesTranscurridos} de ${c.numeroMeses} · estado ${c.estado.toLowerCase()}`}>
        Resumen de la natillera
      </Titulo>

      {r.alertaCajaNegativa && (
        <div role="alert" className="rounded-xl border border-red-300 bg-red-50 p-3 text-sm font-medium text-red-800">
          ▲ Se está prestando más dinero del que hay disponible: la caja acumulada queda negativa en{' '}
          {r.meses.filter((m) => m.alertaCajaNegativa).map((m) => nombreMesCiclo(c.fechaInicio, m.mes)).join(', ')}.
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Kpi etiqueta="Ahorro esperado del año" valor={formatoCOP(k.ahorroEsperadoAnual)} />
        <Kpi etiqueta="Ahorro real a la fecha" valor={formatoCOP(k.ahorroRealALaFecha)} />
        <Kpi etiqueta="Total prestado" valor={formatoCOP(k.totalPrestado)} nota={`Por cobrar ${formatoCOP(k.saldoPorCobrar)}`} />
        <Kpi etiqueta="Intereses" valor={formatoCOP(k.interesesProyectados)} nota={`Recibidos ${formatoCOP(k.interesesRecibidos)}`} />
        <Kpi etiqueta="Utilidad esperada rifas" valor={formatoCOP(k.utilidadEsperadaActividades)} />
        <Kpi etiqueta="Rendimiento cajitas Nu (proy.)" valor={formatoCOP(k.rendimientoProyectadoNu)} />
        <Kpi etiqueta="Total a repartir (proy.)" valor={formatoCOP(k.totalARepartirProyectado)} nota={`Rentabilidad ${formatoPorcentaje(k.rentabilidadProyectada)}`} />
        <Kpi etiqueta="Saldo en cajitas" valor={formatoCOP(k.saldoTotalCajitas)} alerta={k.diferenciaNu !== 0}
          nota={k.diferenciaNu !== 0 ? `Diferencia con Nu ${formatoCOP(k.diferenciaNu)}` : 'Cuadra con la app Nu'} />
      </div>

      <Graficas resumen={r} fechaInicio={c.fechaInicio} />

      <Tarjeta titulo="Resumen mensual" acciones={
        <span className="flex gap-3 text-sm">
          <a className="text-marca-700 underline" href={urlDescarga('/reportes/resumen', 'xlsx')}>Excel</a>
          <a className="text-marca-700 underline" href={urlDescarga('/reportes/resumen', 'csv')}>CSV</a>
        </span>
      }>
        <Tabla etiqueta="Resumen mensual">
          <thead>
            <tr><th>Mes</th><th>Ahorro esperado</th><th>Ahorro real</th><th>Préstamos</th><th>Capital rec.</th><th>Intereses</th><th>Rifas</th><th>Flujo neto</th><th>Caja acumulada</th><th>Fondo total</th></tr>
          </thead>
          <tbody>
            {r.meses.map((m) => (
              <tr key={m.mes} className={m.mes <= r.mesesTranscurridos ? '' : 'text-stone-500'}>
                <td>{nombreMesCiclo(c.fechaInicio, m.mes)}</td>
                <td><Dinero valor={m.ahorroEsperado} /></td>
                <td><Dinero valor={m.ahorroReal} /></td>
                <td><Dinero valor={m.prestamosEntregados} /></td>
                <td><Dinero valor={m.capitalRecuperado} /></td>
                <td><Dinero valor={m.intereses} /></td>
                <td><Dinero valor={m.utilidadActividades} /></td>
                <td><Dinero valor={m.flujoNeto} /></td>
                <td><Dinero valor={m.cajaDisponibleAcumulada} alerta={m.alertaCajaNegativa} /></td>
                <td><Dinero valor={m.fondoTotalAcumulado} /></td>
              </tr>
            ))}
          </tbody>
        </Tabla>
      </Tarjeta>
    </div>
  );
}
