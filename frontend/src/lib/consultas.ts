'use client';

import { useQuery } from '@tanstack/react-query';
import { api } from './api';
import type { DatosSesion } from './sesion';
import type { Cajita, Ciclo, Integrante } from './tipos';

export function useSesion() {
  return useQuery({
    queryKey: ['sesion'],
    queryFn: async (): Promise<DatosSesion> => {
      const res = await fetch('/api/auth/me');
      if (!res.ok) throw new Error('Sin sesión');
      return res.json();
    },
    staleTime: 5 * 60_000,
  });
}

export function useCiclo() {
  return useQuery({ queryKey: ['ciclo'], queryFn: () => api.get<Ciclo>('/ciclo') });
}

export function useIntegrantes() {
  return useQuery({ queryKey: ['integrantes'], queryFn: () => api.get<Integrante[]>('/integrantes') });
}

export function useCajitas() {
  return useQuery({ queryKey: ['cajitas'], queryFn: () => api.get<Cajita[]>('/tesoreria/cajitas') });
}
