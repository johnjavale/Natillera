import { formatoCOP, formatoFecha, formatoPorcentaje, nombreMesCiclo } from './formato';

describe('formatoCOP', () => {
  it('muestra pesos sin decimales con separador de miles', () => {
    expect(formatoCOP(1250000)).toBe('$1.250.000');
    expect(formatoCOP('196666.67')).toBe('$196.667');
    expect(formatoCOP(0)).toBe('$0');
  });

  it('muestra negativos y valores vacíos', () => {
    expect(formatoCOP(-720)).toMatch(/^-\$720$|^\$-720$/);
    expect(formatoCOP(null)).toBe('—');
    expect(formatoCOP(undefined)).toBe('—');
    expect(formatoCOP('abc')).toBe('—');
  });
});

describe('otros formatos', () => {
  it('porcentaje con una cifra decimal', () => {
    expect(formatoPorcentaje(0.08)).toBe('8,0 %');
    expect(formatoPorcentaje(0.24)).toBe('24,0 %');
  });

  it('fecha de negocio sin corrimiento de zona horaria', () => {
    expect(formatoFecha('2027-01-01')).toBe('1 ene 2027');
    expect(formatoFecha('2027-12-31')).toBe('31 dic 2027');
  });

  it('nombre del mes del ciclo cruza de año', () => {
    expect(nombreMesCiclo('2027-01-01', 3)).toBe('mar 2027');
    expect(nombreMesCiclo('2027-06-01', 8)).toBe('ene 2028');
  });
});
