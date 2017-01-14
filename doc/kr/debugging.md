# Debugging

Scala.js는 소스 맵을 지원하므로 브라우저에서 코드를 디버깅하는 것이 매우 쉽습니다. 실제 소스 코드에서 중단 점을 설정하고 실제 IDE 디버거 에서처럼 로컬 변수를 검사 할 수 있습니다. 자세한 내용은 브라우저 개발자 도구에 대한 설명서를 참조하십시오.

## Setting source maps

Play Scala.js 플러그인은 소스 맵을 올바른 위치에 자동으로 복사합니다.

## Actual debugging

애플리케이션을 실행할 때 아래와 같이 개발자 도구 창을 통해 소스에 액세스 할 수 있습니다.

![debug sources](images/debug1.png?raw=true)

중단 점을 설정하고 변수를 조사하여 코드에서 어떤 일이 벌어지고 있는지 확인할 수 있습니다. 변수 이름 중 일부는 '$ 1 완료'와 같은 재미있는 확장이 있지만 이는 스칼라가 맹 글링 한 이름 때문일 수 있습니다. 아래에서 디버거가 중단 점에 어떻게 도달했으며 로컬 변수가 자동으로 표시되는지 확인할 수 있습니다.

![breakpoints](images/debug2.png?raw=true)

DevTools 창의 활성 반응 구성 요소를 시각화하는 데 도움이되도록 Facebook [React DevTools](https://chrome.google.com/webstore/detail/react-developer-tools/fmkadmapgofadopljbjfkapdkoienihi)을 Chrome 브라우저에 설치해야 할 수도 있습니다.

