/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        ink: '#0f1b2d',
        ember: '#e05a47',
        parchment: '#f4efe5',
        pine: '#295245',
        steel: '#6b7a90'
      },
      boxShadow: {
        dossier: '0 24px 80px rgba(15, 27, 45, 0.14)'
      }
    }
  },
  plugins: []
};
