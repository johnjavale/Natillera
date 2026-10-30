import { render, screen } from '@testing-library/react';
import { ApiError } from '@/lib/api';
import { Dinero, Estado, MensajeError } from './ui';

describe('componentes de UI', () => {
  it('Dinero en rojo cuando hay alerta (atraso, diferencia, caja negativa)', () => {
    render(<><Dinero valor={100000} /><Dinero valor={-720} alerta /></>);
    expect(screen.getByText('$100.000')).not.toHaveClass('text-red-700');
    expect(screen.getByText(/720/)).toHaveClass('text-red-700');
  });

  it('préstamo PAGADO en verde con ícono y texto (no solo color)', () => {
    render(<Estado estado="PAGADO" />);
    const etiqueta = screen.getByText('Pagado');
    expect(etiqueta).toHaveClass('text-hoja-700');
    expect(etiqueta.textContent).toContain('✓');
  });

  it('muestra el detail y los errores del ProblemDetail', () => {
    render(<MensajeError error={new ApiError({ status: 422, detail: 'Los préstamos no pueden ser a más de 12 cuotas (un año)', errores: { monto: 'El monto es obligatorio' } })} />);
    expect(screen.getByRole('alert')).toHaveTextContent('más de 12 cuotas');
    expect(screen.getByText('El monto es obligatorio')).toBeInTheDocument();
  });
});
