import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Vite 개발 서버 설정. https://vite.dev/config/
export default defineConfig({
  // @vitejs/plugin-react : JSX 변환과 HMR(수정한 컴포넌트만 새로고침 없이 교체)을 담당한다.
  plugins: [react()],

  server: {
    port: 5173,

    /**
     * 개발 중 /api로 시작하는 요청을 백엔드(8080)로 대신 보내준다.
     *
     * 왜 필요한가:
     * 프론트는 5173, 백엔드는 8080에서 뜨는데 브라우저는 포트가 다르면 다른 출처로 보고
     * 요청을 막는다(CORS). 백엔드에 CORS 허용 설정을 넣어 푸는 방법도 있지만,
     * 그러면 개발 편의를 위한 설정이 제출 코드에 남는다.
     * 프록시는 브라우저 입장에서 같은 출처(5173)로 요청이 나가고 Vite가 8080으로 전달하므로
     * 백엔드를 건드리지 않고 해결된다.
     *
     * rewrite를 두지 않은 이유: 백엔드 API 경로 자체가 /api로 시작하므로
     * 앞부분을 떼어낼 필요 없이 경로를 그대로 넘긴다.
     * changeOrigin은 전달할 때 Host 헤더를 대상 서버(8080) 기준으로 바꾼다.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
