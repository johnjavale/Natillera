'use client';

import { useState } from 'react';
import { Selector, Tarjeta, Titulo } from '@/components/ui';
import { urlDescarga } from '@/lib/api';
import { useIntegrantes, useSesion } from '@/lib/consultas';

const REPORTES = [
  { ruta: '/reportes/consolidado', nombre: 'Consolidado por integrante', descripcion: 'Cuota, aportes, atraso, cajita, préstamos, boletas y total a recibir.' },
  { ruta: '/reportes/deudores', nombre: '¿Quién debe y cuánto?', descripcion: 'Atraso de aportes, saldo de préstamos y boletas sin pagar.' },
  { ruta: '/reportes/tesoreria', nombre: 'Tesorería', descripcion: 'Saldos por cajita y tesorero, conciliación con Nu y movimientos.' },
  { ruta: '/reportes/resumen', nombre: 'Resumen mensual y anual', descripcion: 'Flujo de caja, KPIs y rentabilidad (solo administrador).', soloAdmin: true },
];

export default function Reportes() {
  const sesion = useSesion();
  const integrantes = useIntegrantes();
  const [integrante, setIntegrante] = useState('');
  const esAdmin = sesion.data?.roles.includes('ADMIN') ?? false;

  return (
    <div className="space-y-4">
      <Titulo subtitulo="Cada reporte respeta su alcance: el tesorero ve solo a sus integrantes.">Reportes</Titulo>
      <div className="grid gap-3 sm:grid-cols-2">
        {REPORTES.filter((r) => !r.soloAdmin || esAdmin).map((r) => (
          <Tarjeta key={r.ruta} titulo={r.nombre}>
            <p className="mb-3 text-sm text-stone-600">{r.descripcion}</p>
            <Descargas ruta={r.ruta} />
          </Tarjeta>
        ))}
        <Tarjeta titulo="Estado de cuenta individual">
          <Selector etiqueta="Integrante" name="integrante" value={integrante} onChange={(e) => setIntegrante(e.target.value)} className="mb-3">
            <option value="">Seleccione…</option>
            {integrantes.data?.map((i) => <option key={i.id} value={i.id}>{i.nombre}</option>)}
          </Selector>
          {integrante && <Descargas ruta={`/reportes/estado-cuenta/${integrante}`} />}
        </Tarjeta>
      </div>
      <p className="text-xs text-stone-500">También puede imprimir cualquier página con Ctrl+P: la versión impresa oculta el menú.</p>
    </div>
  );
}

function Descargas({ ruta }: { ruta: string }) {
  return (
    <div className="flex gap-2">
      <a href={urlDescarga(ruta, 'xlsx')} className="rounded-lg bg-marca-600 px-3 py-2 text-sm font-medium text-white hover:bg-marca-700">Excel (.xlsx)</a>
      <a href={urlDescarga(ruta, 'csv')} className="rounded-lg border border-stone-300 px-3 py-2 text-sm hover:bg-stone-50">CSV</a>
    </div>
  );
}
