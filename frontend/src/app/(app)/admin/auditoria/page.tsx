'use client';

import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Boton, Cargando, MensajeError, Tabla, Tarjeta, Titulo, Vacio } from '@/components/ui';
import { api } from '@/lib/api';
import type { RegistroAuditoria } from '@/lib/tipos';

interface Pagina {
  registros: RegistroAuditoria[];
  pagina: number;
  totalPaginas: number;
  totalRegistros: number;
}

export default function Auditoria() {
  const [pagina, setPagina] = useState(0);
  const datos = useQuery({ queryKey: ['auditoria', pagina], queryFn: () => api.get<Pagina>(`/auditoria?pagina=${pagina}&tamano=50`) });
  return (
    <div className="space-y-4">
      <Titulo subtitulo="Cambios sensibles: cuotas, tasas, anulaciones, roles y cierre del ciclo.">Auditoría</Titulo>
      <Tarjeta>
        {datos.isLoading ? <Cargando /> : datos.error ? <MensajeError error={datos.error} /> : !datos.data?.registros.length ? (
          <Vacio mensaje="Sin registros de auditoría." />
        ) : (
          <>
            <Tabla etiqueta="Auditoría">
              <thead><tr><th>Fecha</th><th>Usuario</th><th>Entidad</th><th>Acción</th><th>Detalle</th></tr></thead>
              <tbody>
                {datos.data.registros.map((a) => (
                  <tr key={a.id}>
                    <td>{new Date(a.fecha).toLocaleString('es-CO', { timeZone: 'America/Bogota' })}</td>
                    <td>{a.usuario}</td><td>{a.entidad} #{a.entidadId}</td><td>{a.accion}</td>
                    <td className="max-w-md whitespace-normal">{a.detalle}</td>
                  </tr>
                ))}
              </tbody>
            </Tabla>
            <div className="mt-3 flex items-center gap-2 text-sm">
              <Boton variante="secundario" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</Boton>
              <span>Página {pagina + 1} de {Math.max(1, datos.data.totalPaginas)}</span>
              <Boton variante="secundario" disabled={pagina + 1 >= datos.data.totalPaginas} onClick={() => setPagina(pagina + 1)}>Siguiente</Boton>
            </div>
          </>
        )}
      </Tarjeta>
    </div>
  );
}
