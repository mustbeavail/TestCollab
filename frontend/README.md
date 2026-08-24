# frontend

Project Collab의 프론트엔드입니다. 과제에서 프론트엔드는 선택 항목이라 현재는 뼈대만 있습니다.

## 실행

```bash
npm install
npm run dev      # http://localhost:5173
```

백엔드가 8080에서 함께 떠 있어야 API 호출이 동작합니다.

| 명령 | 하는 일 |
| :-- | :-- |
| `npm run dev` | 개발 서버 실행 (HMR) |
| `npm run build` | 프로덕션 빌드 (`dist/`) |
| `npm run lint` | oxlint 검사 |

## 구성

```
src/
├─ main.jsx     진입점
├─ App.jsx      최상위 컴포넌트 (현재 자리표시자)
└─ index.css    전역 스타일
```

- **React 19 · Vite** — 설정이 가장 가벼운 조합을 골랐습니다.
- **추가 라이브러리 없음** — 화면이 하나뿐이라 라우터도 상태관리 라이브러리도 넣지 않았습니다.
  화면이 늘어 주소로 구분할 필요가 생기면 그때 추가합니다.
- **개발 서버 프록시** — `/api` 요청을 `localhost:8080`으로 전달합니다.
  백엔드에 CORS 설정을 넣지 않아도 되도록 한 조치이며, 자세한 이유는 `vite.config.js` 주석에 적었습니다.
