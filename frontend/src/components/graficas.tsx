'use client';

import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { formatoCOP } from '@/lib/formato';

/** Slots de la paleta categórica validada (claro): azul, naranja; rojo reservado para estado crítico. */
const SERIE_1 = '#2a78d6';
const SERIE_2 = '#eb6834';
const CRITICO = '#c62828';
const TEXTO = '#52514e';
const GRILLA = '#e7e5e4';

const corto = (v: number) => (Math.abs(v) >= 1_000_000 ? `${(v / 1_000_000).toFixed(1)} M` : `${Math.round(v / 1000)} mil`);

const ejes = {
  tick: { fill: TEXTO, fontSize: 12 },
  axisLine: { stroke: GRILLA },
  tickLine: false,
};

function Marco({ titulo, children, alto = 240 }: { titulo: string; children: React.ReactElement; alto?: number }) {
  return (
    <figure>
      <figcaption className="mb-2 text-sm font-semibold text-stone-800">{titulo}</figcaption>
      <div style={{ height: alto }}>
        <ResponsiveContainer width="100%" height="100%">{children}</ResponsiveContainer>
      </div>
    </figure>
  );
}

export interface PuntoMes {
  mes: string;
  esperado: number;
  real: number;
  caja: number;
  intereses: number;
}

export function GraficaAhorro({ datos }: { datos: PuntoMes[] }) {
  return (
    <Marco titulo="Ahorro esperado vs. real por mes">
      <BarChart data={datos} barGap={2} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid vertical={false} stroke={GRILLA} />
        <XAxis dataKey="mes" {...ejes} />
        <YAxis tickFormatter={corto} width={56} {...ejes} />
        <Tooltip formatter={(v: number) => formatoCOP(v)} cursor={{ fill: '#f5f5f4' }} />
        <Legend wrapperStyle={{ fontSize: 12, color: TEXTO }} />
        <Bar dataKey="esperado" name="Esperado" fill={SERIE_1} radius={[4, 4, 0, 0]} isAnimationActive={false} />
        <Bar dataKey="real" name="Real" fill={SERIE_2} radius={[4, 4, 0, 0]} isAnimationActive={false} />
      </BarChart>
    </Marco>
  );
}

export function GraficaCaja({ datos }: { datos: PuntoMes[] }) {
  return (
    <Marco titulo="Caja disponible acumulada (proyectada)">
      <LineChart data={datos} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid vertical={false} stroke={GRILLA} />
        <XAxis dataKey="mes" {...ejes} />
        <YAxis tickFormatter={corto} width={56} {...ejes} />
        <Tooltip formatter={(v: number) => formatoCOP(v)} />
        <ReferenceLine y={0} stroke={TEXTO} strokeDasharray="3 3" />
        <Line
          type="monotone"
          dataKey="caja"
          name="Caja acumulada"
          stroke={SERIE_1}
          strokeWidth={2}
          isAnimationActive={false}
          dot={(p: { cx?: number; cy?: number; payload?: PuntoMes; index?: number }) => (
            <circle key={p.index} cx={p.cx} cy={p.cy} r={4} fill={(p.payload?.caja ?? 0) < 0 ? CRITICO : SERIE_1} stroke="#fff" strokeWidth={2} />
          )}
        />
      </LineChart>
    </Marco>
  );
}

export function GraficaIntereses({ datos }: { datos: PuntoMes[] }) {
  return (
    <Marco titulo="Intereses de préstamos por mes (proyectados)">
      <BarChart data={datos} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid vertical={false} stroke={GRILLA} />
        <XAxis dataKey="mes" {...ejes} />
        <YAxis tickFormatter={corto} width={56} {...ejes} />
        <Tooltip formatter={(v: number) => formatoCOP(v)} cursor={{ fill: '#f5f5f4' }} />
        <Bar dataKey="intereses" name="Intereses" fill={SERIE_1} radius={[4, 4, 0, 0]} isAnimationActive={false} />
      </BarChart>
    </Marco>
  );
}

export function GraficaDistribucion({ datos }: { datos: Array<{ concepto: string; valor: number }> }) {
  return (
    <Marco titulo="Distribución del reparto al cierre (proyectado)" alto={200}>
      <BarChart data={datos} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 0 }}>
        <CartesianGrid horizontal={false} stroke={GRILLA} />
        <XAxis type="number" tickFormatter={corto} {...ejes} />
        <YAxis type="category" dataKey="concepto" width={110} {...ejes} />
        <Tooltip formatter={(v: number) => formatoCOP(v)} cursor={{ fill: '#f5f5f4' }} />
        <Bar dataKey="valor" name="Valor" radius={[0, 4, 4, 0]} isAnimationActive={false}>
          {datos.map((d) => <Cell key={d.concepto} fill={SERIE_1} />)}
        </Bar>
      </BarChart>
    </Marco>
  );
}
