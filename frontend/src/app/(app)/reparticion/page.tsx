'use client';

import { useQuery } from '@tanstack/react-query';
import { Cargando, Dinero, MensajeError, Tabla, Tarjeta, Titulo } from '@/components/ui';
import { api } from '@/lib/api';
import { formatoPorcentaje } from '@/lib/formato';
import type { ConceptosReparticion, Reparticion } from '@/lib/tipos';

export default function PaginaReparticion() {
  const r = useQuery({ queryKey: ['reparticion'], queryFn: () => api.get<Reparticion>('/reparticion') });
  if (r.isLoading) return <Cargando filas={8} />;
  if (r.error) return <MensajeError error={r.error} />;
  const d = r.data;
  if (!d) return null;

  return (
    <div className="space-y-4">
      <Titulo subtitulo="Ej.: quien ahorra el 20 % del total recibe el 20 % de los intereses. Las rifas se reparten en partes iguales.">
        Repartición al cierre
      </Titulo>
      {d.proyectado && d.real && (
        <Tarjeta titulo="Conceptos">
          <Tabla etiqueta="Conceptos de la repartición">
            <thead><tr><th>Concepto</th><th>Proyectado</th><th>Real a la fecha</th></tr></thead>
            <tbody>
              {([
                ['Intereses de préstamos', 'interesesPrestamos'],
                ['(−) Gastos administrativos', 'gastosAdministrativos'],
                ['Intereses netos a repartir', 'interesesNetos'],
                ['Utilidad de rifas a repartir', 'utilidadActividades'],
                ['Utilidad de rifas por integrante', 'utilidadActividadesPorIntegrante'],
              ] as Array<[string, keyof ConceptosReparticion]>).map(([etiqueta, campo]) => (
                <tr key={campo}><td>{etiqueta}</td><td><Dinero valor={d.proyectado![campo]} /></td><td><Dinero valor={d.real![campo]} /></td></tr>
              ))}
            </tbody>
          </Tabla>
        </Tarjeta>
      )}
      <Tarjeta titulo="Por integrante">
        <Tabla etiqueta="Repartición por integrante">
          <thead>
            <tr>
              <th>Puesto</th><th>Integrante</th><th>Ahorro esperado</th><th>% part.</th><th>Intereses</th><th>Rifas</th><th>Rend. Nu</th><th>Total proy.</th>
              <th>Ahorro real</th><th>Intereses</th><th>Rifas</th><th>Rend. Nu</th><th>Total real</th>
            </tr>
          </thead>
          <tbody>
            {d.filas.map((f) => (
              <tr key={f.integranteId}>
                <td>{f.puesto}</td><td>{f.nombre}</td>
                <td><Dinero valor={f.ahorroEsperado} /></td><td>{formatoPorcentaje(f.participacionProyectada)}</td>
                <td><Dinero valor={f.interesesProyectados} /></td><td><Dinero valor={f.actividadesProyectadas} /></td>
                <td><Dinero valor={f.rendimientoNuProyectado} /></td><td className="font-semibold"><Dinero valor={f.totalProyectado} /></td>
                <td><Dinero valor={f.ahorroReal} /></td><td><Dinero valor={f.interesesReales} /></td>
                <td><Dinero valor={f.actividadesReales} alerta={f.actividadesReales < 0} /></td><td><Dinero valor={f.rendimientoNuReal} /></td>
                <td className="font-semibold"><Dinero valor={f.totalReal} /></td>
              </tr>
            ))}
            {d.totalProyectado !== null && (
              <tr className="font-semibold"><td /><td>TOTAL</td><td colSpan={5} /><td><Dinero valor={d.totalProyectado} /></td><td colSpan={4} /><td><Dinero valor={d.totalReal} /></td></tr>
            )}
          </tbody>
        </Tabla>
        <p className="mt-2 text-xs text-stone-500">Rendimiento Nu proyectado: estimado; baja si se saca plata de la cajita para préstamos.</p>
      </Tarjeta>
    </div>
  );
}
