import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        marca: {
          50: '#fff8eb',
          100: '#feebc8',
          200: '#fcd38d',
          500: '#e8891b',
          600: '#c96f10',
          700: '#a05410',
          900: '#5a2f0c',
        },
        hoja: {
          50: '#effaf3',
          600: '#1f8a4c',
          700: '#176b3b',
        },
      },
      fontFamily: {
        sans: ['system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};

export default config;
