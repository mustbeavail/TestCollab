import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Vite 개발 서버 설정. https://vite.dev/config/
export default defineConfig({
  // JSX 변환과 HMR을 담당한다.
  plugins: [react()],

  server: {
    port: 5173,

    /**
     * 개발 중 /api 요청을 백엔드(8080)로 넘긴다.
     * 프론트(5173)와 포트가 달라 브라우저가 막는데(CORS), 프록시를 쓰면
     * 개발 편의용 CORS 설정을 제출 코드에 남기지 않고 해결된다.
     * 백엔드 경로가 이미 /api로 시작해 rewrite는 두지 않는다.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
