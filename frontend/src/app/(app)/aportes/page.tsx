'use client';

import { useQuery } from '@tanstack/react-query';
import { Cargando, Dinero, MensajeError, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api } from '@/lib/api';
import { useCiclo } from '@/lib/consultas';
import { formatoPorcentaje, nombreMesCiclo } from '@/lib/formato';
import type { MatrizAportes } from '@/lib/tipos';

/** Vista matriz integrantes × meses (hoja "Ahorros Mensuales"). */
export default function Aportes() {
  const ciclo = useCiclo();
  const matriz = useQuery({ queryKey: ['aportes'], queryFn: () => api.get<MatrizAportes>('/integrantes/aportes') });
  if (matriz.isLoading || ciclo.isLoading) return <Cargando filas={8} />;
  if (matriz.error) return <MensajeError error={matriz.error} />;
  const m = matriz.data;
  const c = ciclo.data;
  if (!m || !c) return null;

  return (
    <div className="space-y-4">
      <Titulo subtitulo={`Meses transcurridos: ${m.mesesTranscurridos}. En rojo, quien está atrasado.`}>Ahorros mensuales</Titulo>
      <Tarjeta>
        {m.filas.length === 0 ? <Vacio mensaje="No hay integrantes en su alcance." /> : (
          <Tabla etiqueta="Ahorros mensuales">
            <thead>
              <tr>
                <th>Integrante</th>
                {Array.from({ length: m.numeroMeses }, (_, i) => <th key={i}>{nombreMesCiclo(c.fechaInicio, i + 1)}</th>)}
                <th>Total</th><th>Esperado</th><th>% cumpl.</th><th>Atraso</th>
              </tr>
            </thead>
            <tbody>
              {m.filas.map((f) => (
                <tr key={f.integranteId}>
                  <td>{f.nombre}</td>
                  {f.aportesPorMes.map((v, i) => <td key={i}>{v ? <Dinero valor={v} /> : ''}</td>)}
                  <td><Dinero valor={f.totalAportado} /></td>
                  <td><Dinero valor={f.ahorroEsperado} /></td>
                  <td>{formatoPorcentaje(f.cumplimiento)}</td>
                  <td><Dinero valor={f.atraso} alerta={f.enAtraso} /></td>
                </tr>
              ))}
              <tr className="font-semibold">
                <td>Total real</td>
                {m.totales.map((t) => <td key={t.mes}><Dinero valor={t.totalReal} /></td>)}
                <td colSpan={4} />
              </tr>
              <tr className="text-stone-600">
                <td>Total esperado</td>
                {m.totales.map((t) => <td key={t.mes}><Dinero valor={t.totalEsperado} /></td>)}
                <td colSpan={4} />
              </tr>
              <tr className="text-stone-600">
                <td>Acumulado real</td>
                {m.totales.map((t) => <td key={t.mes}><Dinero valor={t.acumuladoReal} /></td>)}
                <td colSpan={4} />
              </tr>
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
    </div>
  );
}
