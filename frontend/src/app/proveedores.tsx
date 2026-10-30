'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { ApiError } from '@/lib/api';

export function Proveedores({ children }: { children: ReactNode }) {
  const [cliente] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            retry: (intentos, error) =>
              !(error instanceof ApiError && error.status >= 400 && error.status < 500) && intentos < 2,
          },
        },
      }),
  );
  return <QueryClientProvider client={cliente}>{children}</QueryClientProvider>;
}
