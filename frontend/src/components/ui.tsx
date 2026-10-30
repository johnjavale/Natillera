'use client';

import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import { forwardRef } from 'react';
import { ApiError } from '@/lib/api';
import { formatoCOP } from '@/lib/formato';

export function Tarjeta({ titulo, children, acciones, className = '' }: {
  titulo?: ReactNode;
  children: ReactNode;
  acciones?: ReactNode;
  className?: string;
}) {
  return (
    <section className={`rounded-2xl border border-stone-200 bg-white p-4 shadow-sm sm:p-5 ${className}`}>
      {(titulo || acciones) && (
        <header className="mb-3 flex flex-wrap items-center justify-between gap-2">
          {titulo && <h2 className="text-base font-semibold text-stone-800">{titulo}</h2>}
          {acciones}
        </header>
      )}
      {children}
    </section>
  );
}

export function Boton({ variante = 'primario', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: 'primario' | 'secundario' | 'peligro';
}) {
  const estilos = {
    primario: 'bg-marca-600 text-white hover:bg-marca-700',
    secundario: 'border border-stone-300 bg-white text-stone-800 hover:bg-stone-50',
    peligro: 'bg-red-700 text-white hover:bg-red-800',
  }[variante];
  return (
    <button
      {...props}
      className={`inline-flex min-h-10 items-center justify-center rounded-lg px-4 py-2 text-sm font-medium focus:outline-none focus-visible:ring-2 focus-visible:ring-marca-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${estilos} ${className}`}
    />
  );
}

interface CampoProps {
  etiqueta: string;
  error?: string;
  ayuda?: string;
}

export const Campo = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & CampoProps>(
  function Campo({ etiqueta, error, ayuda, id, className = '', ...props }, ref) {
    const campoId = id ?? props.name;
    return (
      <div className={className}>
        <label htmlFor={campoId} className="mb-1 block text-sm font-medium text-stone-700">{etiqueta}</label>
        <input
          ref={ref}
          id={campoId}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${campoId}-error` : undefined}
          className="block w-full rounded-lg border border-stone-300 px-3 py-2 text-base focus:border-marca-500 focus:outline-none focus:ring-2 focus:ring-marca-200 aria-[invalid=true]:border-red-600"
          {...props}
        />
        {ayuda && !error && <p className="mt-1 text-xs text-stone-500">{ayuda}</p>}
        {error && <p id={`${campoId}-error`} role="alert" className="mt-1 text-xs text-red-700">{error}</p>}
      </div>
    );
  },
);

export const Selector = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement> & CampoProps>(
  function Selector({ etiqueta, error, id, children, className = '', ...props }, ref) {
    const campoId = id ?? props.name;
    return (
      <div className={className}>
        <label htmlFor={campoId} className="mb-1 block text-sm font-medium text-stone-700">{etiqueta}</label>
        <select
          ref={ref}
          id={campoId}
          aria-invalid={Boolean(error)}
          className="block w-full rounded-lg border border-stone-300 bg-white px-3 py-2 text-base focus:border-marca-500 focus:outline-none focus:ring-2 focus:ring-marca-200"
          {...props}
        >
          {children}
        </select>
        {error && <p role="alert" className="mt-1 text-xs text-red-700">{error}</p>}
      </div>
    );
  },
);

/** Monto en COP; en rojo si `alerta` (atraso, diferencia de conciliación, caja negativa). */
export function Dinero({ valor, alerta = false, className = '' }: { valor: number | null | undefined; alerta?: boolean; className?: string }) {
  return (
    <span className={`tabular-nums ${alerta ? 'font-semibold text-red-700' : ''} ${className}`}>
      {formatoCOP(valor)}
    </span>
  );
}

export function Estado({ estado }: { estado: 'VIGENTE' | 'EN_MORA' | 'PAGADO' }) {
  const cfg = {
    VIGENTE: { texto: 'Vigente', clase: 'bg-sky-50 text-sky-800 ring-sky-200', icono: '●' },
    EN_MORA: { texto: 'En mora', clase: 'bg-red-50 text-red-800 ring-red-200', icono: '▲' },
    PAGADO: { texto: 'Pagado', clase: 'bg-hoja-50 text-hoja-700 ring-green-200', icono: '✓' },
  }[estado];
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ring-1 ${cfg.clase}`}>
      <span aria-hidden>{cfg.icono}</span>
      {cfg.texto}
    </span>
  );
}

export function Kpi({ etiqueta, valor, nota, alerta = false }: { etiqueta: string; valor: ReactNode; nota?: string; alerta?: boolean }) {
  return (
    <div className={`rounded-xl border p-3 ${alerta ? 'border-red-200 bg-red-50' : 'border-stone-200 bg-white'}`}>
      <p className="text-xs text-stone-600">{etiqueta}</p>
      <p className={`mt-1 text-lg font-semibold tabular-nums ${alerta ? 'text-red-700' : 'text-stone-900'}`}>{valor}</p>
      {nota && <p className="mt-0.5 text-xs text-stone-500">{nota}</p>}
    </div>
  );
}

export function Cargando({ filas = 3 }: { filas?: number }) {
  return (
    <div role="status" aria-label="Cargando" className="space-y-2">
      {Array.from({ length: filas }).map((_, i) => (
        <div key={i} className="h-10 animate-pulse rounded-lg bg-stone-100" />
      ))}
    </div>
  );
}

export function Vacio({ mensaje }: { mensaje: string }) {
  return <p className="rounded-lg border border-dashed border-stone-300 p-6 text-center text-sm text-stone-500">{mensaje}</p>;
}

/** Muestra el mensaje del ProblemDetail del backend. */
export function MensajeError({ error }: { error: unknown }) {
  if (!error) return null;
  const texto = error instanceof ApiError ? error.message : 'Ocurrió un error inesperado. Intente de nuevo.';
  const errores = error instanceof ApiError ? error.problema.errores : undefined;
  return (
    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">
      <p>{texto}</p>
      {errores && (
        <ul className="mt-1 list-disc pl-5">
          {Object.entries(errores).map(([campo, msg]) => <li key={campo}>{msg}</li>)}
        </ul>
      )}
    </div>
  );
}

export function Exito({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null;
  return <p role="status" className="rounded-lg border border-green-200 bg-hoja-50 p-3 text-sm text-hoja-700">{mensaje}</p>;
}

/** Tabla con scroll horizontal propio (la página nunca hace scroll horizontal en el celular). */
export function Tabla({ children, etiqueta }: { children: ReactNode; etiqueta: string }) {
  return (
    <div className="-mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
      <table aria-label={etiqueta} className="min-w-full text-sm [&_td]:whitespace-nowrap [&_td]:border-b [&_td]:border-stone-100 [&_td]:px-2 [&_td]:py-2 [&_th]:whitespace-nowrap [&_th]:border-b [&_th]:border-stone-200 [&_th]:px-2 [&_th]:py-2 [&_th]:text-left [&_th]:text-xs [&_th]:font-semibold [&_th]:text-stone-600">
        {children}
      </table>
    </div>
  );
}

export function Titulo({ children, subtitulo }: { children: ReactNode; subtitulo?: ReactNode }) {
  return (
    <div className="mb-4">
      <h1 className="text-xl font-bold text-stone-900 sm:text-2xl">{children}</h1>
      {subtitulo && <p className="mt-1 text-sm text-stone-600">{subtitulo}</p>}
    </div>
  );
}
