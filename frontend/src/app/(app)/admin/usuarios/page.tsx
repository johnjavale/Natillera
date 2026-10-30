'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import type { z } from 'zod';
import { Boton, Campo, Cargando, Exito, MensajeError, Selector, Tabla, Tarjeta, Titulo } from '@/components/ui';
import { api } from '@/lib/api';
import { useIntegrantes } from '@/lib/consultas';
import { usuarioSchema } from '@/lib/esquemas';
import type { Rol } from '@/lib/rutas';
import type { Usuario } from '@/lib/tipos';

type Datos = z.infer<typeof usuarioSchema>;
const ROLES: Rol[] = ['ADMIN', 'TESORERO', 'INTEGRANTE'];

export default function Usuarios() {
  const qc = useQueryClient();
  const usuarios = useQuery({ queryKey: ['usuarios'], queryFn: () => api.get<Usuario[]>('/usuarios') });
  const integrantes = useIntegrantes();
  const [ok, setOk] = useState<string | null>(null);
  const { register, handleSubmit, formState, reset } = useForm<z.input<typeof usuarioSchema>, unknown, Datos>({ resolver: zodResolver(usuarioSchema), defaultValues: { roles: ['INTEGRANTE'] } });

  const crear = useMutation({
    mutationFn: (d: Datos) => api.post('/usuarios', d),
    onSuccess: () => { setOk('Usuario creado. Deberá cambiar la contraseña en su primer ingreso.'); reset({ roles: ['INTEGRANTE'] }); qc.invalidateQueries(); },
  });
  const roles = useMutation({
    mutationFn: ({ u, nuevos }: { u: Usuario; nuevos: Rol[] }) => api.put(`/usuarios/${u.id}/roles`, { roles: nuevos, integranteId: u.integranteId }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['usuarios'] }),
  });
  const activo = useMutation({
    mutationFn: (u: Usuario) => api.put(`/usuarios/${u.id}/activo`, { activo: !u.activo }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['usuarios'] }),
  });
  const restablecer = useMutation({
    mutationFn: ({ id, clave }: { id: number; clave: string }) => api.post(`/usuarios/${id}/restablecer-password`, { contrasenaTemporal: clave }),
    onSuccess: () => setOk('Contraseña temporal asignada'),
  });

  const alternarRol = (u: Usuario, rol: Rol) => {
    const nuevos = u.roles.includes(rol) ? u.roles.filter((r) => r !== rol) : [...u.roles, rol];
    if (nuevos.length) roles.mutate({ u, nuevos });
  };

  return (
    <div className="space-y-4">
      <Titulo subtitulo="Un usuario puede tener varios roles: el tesorero o el administrador también suelen ahorrar.">Usuarios y roles</Titulo>
      <Tarjeta titulo="Nuevo usuario">
        <form onSubmit={handleSubmit((d) => { setOk(null); crear.mutate(d); })} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" noValidate>
          <Campo etiqueta="Nombre" {...register('nombre')} error={formState.errors.nombre?.message} />
          <Campo etiqueta="Correo" type="email" {...register('correo')} error={formState.errors.correo?.message} />
          <Campo etiqueta="Celular" {...register('celular')} error={formState.errors.celular?.message} />
          <fieldset className="text-sm">
            <legend className="mb-1 font-medium text-stone-700">Roles</legend>
            {ROLES.map((r) => <label key={r} className="mr-3 inline-flex items-center gap-1"><input type="checkbox" value={r} {...register('roles')} /> {r}</label>)}
            {formState.errors.roles && <p className="text-xs text-red-700">{formState.errors.roles.message}</p>}
          </fieldset>
          <Selector etiqueta="Vincular a integrante" {...register('integranteId')}>
            <option value="">Ninguno</option>
            {integrantes.data?.filter((i) => !i.usuarioId).map((i) => <option key={i.id} value={i.id}>{i.nombre}</option>)}
          </Selector>
          <Campo etiqueta="Contraseña temporal" type="text" autoComplete="off" {...register('contrasenaTemporal')} error={formState.errors.contrasenaTemporal?.message} />
          <div className="sm:col-span-2 lg:col-span-3">
            <MensajeError error={crear.error} />
            <Exito mensaje={ok} />
            <Boton type="submit" className="mt-2" disabled={crear.isPending}>Crear usuario</Boton>
          </div>
        </form>
      </Tarjeta>
      <Tarjeta titulo="Usuarios">
        <MensajeError error={roles.error ?? activo.error ?? restablecer.error} />
        {usuarios.isLoading ? <Cargando /> : (
          <Tabla etiqueta="Usuarios">
            <thead><tr><th>Nombre</th><th>Ingreso</th>{ROLES.map((r) => <th key={r}>{r}</th>)}<th>Integrante</th><th>Estado</th><th /></tr></thead>
            <tbody>
              {usuarios.data?.map((u) => (
                <tr key={u.id} className={u.activo ? '' : 'text-stone-400'}>
                  <td>{u.nombre}</td><td>{u.correo ?? u.celular}</td>
                  {ROLES.map((r) => (
                    <td key={r}><input type="checkbox" aria-label={`${r} de ${u.nombre}`} checked={u.roles.includes(r)} onChange={() => alternarRol(u, r)} /></td>
                  ))}
                  <td>{u.integranteId ?? '—'}</td>
                  <td>{u.activo ? 'Activo' : 'Inactivo'}{u.debeCambiarPassword && ' · cambio pendiente'}</td>
                  <td className="space-x-2">
                    <button className="text-xs underline" onClick={() => activo.mutate(u)}>{u.activo ? 'Desactivar' : 'Activar'}</button>
                    <button className="text-xs underline" onClick={() => {
                      const clave = window.prompt('Contraseña temporal (mínimo 10 caracteres, letras y números)');
                      if (clave) restablecer.mutate({ id: u.id, clave });
                    }}>Restablecer contraseña</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Tabla>
        )}
      </Tarjeta>
    </div>
  );
}
