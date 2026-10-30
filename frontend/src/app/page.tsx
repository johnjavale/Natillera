import { redirect } from 'next/navigation';

/** El middleware redirige a la página de inicio de cada rol; esto es solo el respaldo. */
export default function Inicio() {
  redirect('/login');
}
