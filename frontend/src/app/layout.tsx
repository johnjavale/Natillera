import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';
import { Proveedores } from './proveedores';
import './globals.css';

export const metadata: Metadata = {
  title: 'Natillera Familiar',
  description: 'Ahorramos juntos, crecemos juntos',
  icons: { icon: '/natillera_familiar.svg' },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: '#e8891b',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="es">
      <body>
        <Proveedores>{children}</Proveedores>
      </body>
    </html>
  );
}
