'use client';

import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Cargando, Dinero, MensajeError, Selector, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api, urlDescarga } from '@/lib/api';
import type { Deudor, ResumenTesoreria } from '@/lib/tipos';

/** ¿Quién debe y cuánto? Atraso de aportes, préstamos y boletas sin pagar. */
export default function Deudores() {
  const [filtro, setFiltro] = useState({ tesoreroId: '', tipo: '', soloMora: '' });
  const query = new URLSearchParams(Object.entries(filtro).filter(([, v]) => v)).toString();
  const ruta = `/reportes/deudores${query ? `?${query}` : ''}`;
  const deudores = useQuery({ queryKey: ['deudores', query], queryFn: () => api.get<Deudor[]>(ruta) });
  const tesoreria = useQuery({ queryKey: ['tesoreria-resumen'], queryFn: () => api.get<ResumenTesoreria>('/tesoreria/resumen') });

  return (
    <div className="space-y-4">
      <Titulo subtitulo="Ordenado de mayor a menor deuda total.">¿Quién debe y cuánto?</Titulo>
      <Tarjeta acciones={
        <span className="flex gap-3 text-sm">
          <a className="text-marca-700 underline" href={urlDescarga(ruta, 'xlsx')}>Excel</a>
          <a className="text-marca-700 underline" href={urlDescarga(ruta, 'csv')}>CSV</a>
        </span>
      } titulo="Filtros">
        <div className="mb-4 grid gap-2 sm:grid-cols-3">
          <Selector etiqueta="Tesorero" name="tesorero" value={filtro.tesoreroId} onChange={(e) => setFiltro({ ...filtro, tesoreroId: e.target.value })}>
            <option value="">Todos</option>
            {tesoreria.data?.tesoreros.map((t) => <option key={t.id} value={t.id}>{t.nombre}</option>)}
          </Selector>
          <Selector etiqueta="Tipo de deuda" name="tipo" value={filtro.tipo} onChange={(e) => setFiltro({ ...filtro, tipo: e.target.value })}>
            <option value="">Todas</option>
            <option value="APORTES">Atraso de aportes</option>
            <option value="PRESTAMOS">Saldo de préstamos</option>
            <option value="BOLETAS">Boletas sin pagar</option>
          </Selector>
          <Selector etiqueta="Mora" name="mora" value={filtro.soloMora} onChange={(e) => setFiltro({ ...filtro, soloMora: e.target.value })}>
            <option value="">Todos</option>
            <option value="true">Solo en mora</option>
          </Selector>
        </div>
        {deudores.isLoading ? <Cargando /> : deudores.error ? <MensajeError error={deudores.error} /> : !deudores.data?.length ? (
          <Vacio mensaje="Nadie debe con estos filtros. 🎉" />
        ) : (
          <Tabla etiqueta="Deudores">
            <thead><tr><th>Integrante</th><th>Tesorero</th><th>Atraso aportes</th><th>Saldo préstamos</th><th>Vencido</th><th>Boletas</th><th>Total</th></tr></thead>
            <tbody>
              {deudores.data.map((d) => (
                <tr key={d.integranteId}>
                  <td>{d.nombre} {d.enMora && <span className="ml-1 text-xs font-semibold text-red-700">▲ mora</span>}</td>
                  <td>{d.nombreTesorero ?? '—'}</td>
                  <td><Dinero valor={d.atrasoAportes} alerta={d.atrasoAportes > 0} /></td>
                  <td><Dinero valor={d.saldoPrestamos} /></td>
                  <td><Dinero valor={d.vencidoPrestamos} alerta={d.vencidoPrestamos > 0} /></td>
                  <td><Dinero valor={d.boletasPendientes} /></td>
                  <td className="font-semibold"><Dinero valor={d.total} /></td>
                </tr>
              ))}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
    </div>
  );
}
