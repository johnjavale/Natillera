'use client';

import { GraficaAhorro, GraficaCaja, GraficaDistribucion, GraficaIntereses, type PuntoMes } from '@/components/graficas';
import { Tarjeta } from '@/components/ui';
import { nombreMesCiclo } from '@/lib/formato';
import type { Resumen } from '@/lib/tipos';

export default function GraficasResumen({ resumen, fechaInicio }: { resumen: Resumen; fechaInicio: string }) {
  const puntos: PuntoMes[] = resumen.meses.map((m) => ({
    mes: nombreMesCiclo(fechaInicio, m.mes).split(' ')[0] ?? String(m.mes),
    esperado: m.ahorroEsperado,
    real: m.ahorroReal,
    caja: m.cajaDisponibleAcumulada,
    intereses: m.intereses,
  }));
  const d = resumen.distribucion;
  const distribucion = [
    { concepto: 'Ahorro', valor: d.ahorro },
    { concepto: 'Intereses', valor: d.intereses },
    { concepto: 'Rifas', valor: d.actividades },
    { concepto: 'Rendimiento Nu', valor: d.rendimientoNu },
  ];
  return (
    <div className="grid gap-3 lg:grid-cols-2">
      <Tarjeta><GraficaAhorro datos={puntos} /></Tarjeta>
      <Tarjeta><GraficaCaja datos={puntos} /></Tarjeta>
      <Tarjeta><GraficaIntereses datos={puntos} /></Tarjeta>
      <Tarjeta><GraficaDistribucion datos={distribucion} /></Tarjeta>
    </div>
  );
}
