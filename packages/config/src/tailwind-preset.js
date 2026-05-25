/** @type {import('tailwindcss').Config} */
module.exports = {
  theme: {
    extend: {
      colors: {
        brand: {
          blue: {
            50:  '#eef1f8',
            100: '#d5deee',
            200: '#aabddd',
            300: '#7a97c8',
            400: '#4f74b0',
            500: '#2d5499',
            600: '#1f4080',
            700: '#1B3566',  // primary brand navy
            800: '#142850',
            900: '#0d1b38',
            950: '#070e1e',
          },
          green: {
            50:  '#f5fae8',
            100: '#e8f4c8',
            200: '#d0e895',
            300: '#b2d85b',
            400: '#9CCA44',
            500: '#8CC63F',  // primary brand lime green
            600: '#72a030',
            700: '#567923',
            800: '#3a5218',
            900: '#243310',
          },
        },
      },
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        display: ['Montserrat', 'Arial Black', 'Arial', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
