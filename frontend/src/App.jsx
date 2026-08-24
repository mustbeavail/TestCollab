/**
 * 애플리케이션의 최상위 컴포넌트.
 *
 * 지금은 화면을 붙이기 전의 자리표시자다. 백엔드가 우선이라 프론트는
 * 뼈대까지만 만들어 두고, 화면은 API가 준비된 뒤 이 자리에서 시작한다.
 *
 * 라우터를 두지 않은 이유: 화면이 하나뿐인 지금은 경로를 나눌 대상이 없다.
 * 화면이 늘어나 주소로 구분할 필요가 생기면 그때 react-router를 넣는다.
 */
function App() {
  return (
    <main>
      <h1>Project Collab</h1>
      <p>프로젝트와 작업을 함께 관리하는 협업 서비스입니다.</p>
      <p>
        백엔드 API 문서는 <a href="http://localhost:8080/swagger-ui.html">Swagger UI</a>에서 볼 수 있습니다.
      </p>
    </main>
  )
}

export default App
