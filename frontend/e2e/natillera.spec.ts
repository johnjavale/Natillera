import { expect, test, type Page } from '@playwright/test';

const CLAVE = 'Natillera2027*';

async function ingresar(page: Page, login: string) {
  await page.goto('/login');
  await page.getByLabel('Correo o celular').fill(login);
  await page.getByLabel('Contraseña').fill(CLAVE);
  await page.getByRole('button', { name: 'Ingresar' }).click();
  // espera a que el BFF guarde las cookies y redirija fuera del login
  await page.waitForURL((url) => !url.pathname.startsWith('/login'));
}

function menu(page: Page) {
  return page.getByRole('navigation', { name: 'Principal' });
}

test.describe('Login y navegación por rol', () => {
  test('credenciales incorrectas muestran el error del backend', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Correo o celular').fill('admin@natillera.local');
    await page.getByLabel('Contraseña').fill('Incorrecta123');
    await page.getByRole('button', { name: 'Ingresar' }).click();
    await expect(page.getByRole('alert').filter({ hasText: 'incorrectos' })).toHaveText('Usuario o contraseña incorrectos');
  });

  test('el integrante solo ve "Mi cuenta" y las rifas', async ({ page }) => {
    await ingresar(page, 'integrante3@natillera.local');
    await expect(page).toHaveURL(/\/mi-cuenta$/);
    await expect(page.getByRole('heading', { name: 'Hola, Samay Moreno' })).toBeVisible();
    const enlaces = await menu(page).getByRole('link').allInnerTexts();
    expect(enlaces).toEqual(['Mi cuenta', 'Rifas']);
    await expect(page.getByText('Mis préstamos')).toBeVisible();
    // la guarda de rutas lo devuelve a su inicio si intenta entrar a administración
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/mi-cuenta$/);
    await page.goto('/tesoreria');
    await expect(page).toHaveURL(/\/mi-cuenta$/);
  });

  test('el integrante no puede pedir datos de otro por la API (IDOR → 404)', async ({ page }) => {
    await ingresar(page, 'integrante3@natillera.local');
    await expect(page).toHaveURL(/\/mi-cuenta$/);
    const propio = await page.request.get('/api/backend/reportes/estado-cuenta/3');
    expect(propio.status()).toBe(200);
    const ajeno = await page.request.get('/api/backend/reportes/estado-cuenta/7');
    expect(ajeno.status()).toBe(404);
  });

  test('el tesorero ve sus cajitas y no la administración', async ({ page }) => {
    await ingresar(page, 'tesorero1@natillera.local');
    await expect(page).toHaveURL(/\/tesoreria$/);
    const enlaces = await menu(page).getByRole('link').allInnerTexts();
    expect(enlaces).toContain('Aportes del mes');
    expect(enlaces).not.toContain('Usuarios');
    await expect(page.getByRole('table', { name: 'Cajitas' }).getByRole('row')).toHaveCount(6);
  });

  test('cerrar sesión borra las cookies y vuelve al login', async ({ page, context }) => {
    await ingresar(page, 'integrante7@natillera.local');
    await expect(page).toHaveURL(/\/mi-cuenta$/);
    const cookies = await context.cookies();
    const access = cookies.find((c) => c.name === 'nat_at');
    expect(access?.httpOnly).toBe(true);
    expect(access?.sameSite).toBe('Lax');
    await menu(page).getByRole('button', { name: 'Salir' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await page.goto('/mi-cuenta');
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('Flujos de tesorero y administrador', () => {
  test('el tesorero registra un aporte en lote', async ({ page }) => {
    await ingresar(page, 'tesorero1@natillera.local');
    await page.getByRole('link', { name: 'Aportes del mes' }).click();
    await expect(page.getByRole('heading', { name: 'Aportes del mes' })).toBeVisible();
    const filas = page.getByRole('table', { name: 'Aportes por integrante' }).getByRole('row');
    await expect(filas).toHaveCount(6);
    // solo registra el aporte atrasado de Pascuala; los demás en 0
    for (const nombre of ['John Jairo Valencia', 'Samay Moreno', 'Sindy Valencia', 'Trinidad Agualimpia']) {
      await page.getByLabel(nombre).fill('0');
    }
    await page.getByLabel('Pascuala Leyes').fill('50000');
    await page.getByRole('button', { name: 'Registrar aportes' }).click();
    await expect(page.getByText('Se registraron 1 aportes')).toBeVisible();
  });

  test('el admin crea un préstamo grande y ve la alerta de caja negativa', async ({ page }) => {
    await ingresar(page, 'admin@natillera.local');
    await expect(page).toHaveURL(/\/admin$/);
    await expect(page.getByRole('heading', { name: 'Resumen de la natillera' })).toBeVisible();

    // dinero extra en la cajita de Haddy para que el préstamo sea válido hoy
    const ingreso = await page.request.post('/api/backend/tesoreria/movimientos', {
      data: { cajitaId: 7, fecha: '2027-03-20', tipo: 'OTRO_INGRESO', descripcion: 'E2E', valor: 6000000 },
      headers: { Origin: new URL(page.url()).origin },
    });
    expect(ingreso.status()).toBe(201);

    await page.goto('/prestamos/nuevo');
    await page.getByLabel('Integrante').selectOption({ label: 'Trinidad Agualimpia' });
    await page.getByLabel('Sale de la cajita').selectOption({ index: 7 });
    await page.getByLabel('Fecha de desembolso').fill('2027-01-20');
    await page.getByLabel('Monto (COP)').fill('4000000');
    await page.getByLabel('Número de cuotas').fill('6');
    await page.getByRole('button', { name: 'Simular' }).click();
    await expect(page.getByRole('table', { name: 'Plan de pagos simulado' })).toBeVisible();
    await page.getByRole('button', { name: 'Crear préstamo' }).click();
    await expect(page).toHaveURL(/\/prestamos\/\d+$/);
    await expect(page.getByText('Trinidad Agualimpia').first()).toBeVisible();

    await page.goto('/admin');
    await expect(page.getByRole('alert').filter({ hasText: 'Se está prestando más dinero del que hay disponible' })).toBeVisible();
  });

  test('validación de préstamos: más de 12 cuotas se rechaza con mensaje en español', async ({ page }) => {
    await ingresar(page, 'tesorero1@natillera.local');
    await page.goto('/prestamos/nuevo');
    await page.getByLabel('Número de cuotas').fill('13');
    await page.getByRole('button', { name: 'Simular' }).click();
    await expect(page.getByText('Los préstamos no pueden ser a más de 12 cuotas (un año)')).toBeVisible();
  });
});
