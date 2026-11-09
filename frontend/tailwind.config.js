/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#0B0E13',
        surface: '#151A22',
        surface2: '#1C2330',
        line: '#273042',
        ink: '#EEF2F7',
        muted: '#A3ADBD',
        faint: '#5B667A',
        mint: { DEFAULT: '#3DDC97', soft: '#123528', ink: '#06120C' },
        star: '#FFC34D',
        danger: { DEFAULT: '#FF8A8A', line: '#5A2A2E', soft: '#3A1C20' },
        amber: { soft: '#3A2E14' },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['"Space Grotesk"', 'Inter', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
