# Java 통합 실습: 당일배송 물류센터

각 파일은 이전 단계의 도메인 구조와 업무 규칙을 발전시킨 독립 실행 버전입니다.
`main` 메서드는 해당 챕터의 핵심 변경을 확인하기 쉽도록 실행 예시를 단순화할 수 있지만,
이전에 만든 택배 접수·조회·상태 변경·이력 관리 기능은 이후 단계에서도 유지합니다.

```text
Ch05Arrays.java          병렬 배열 기반 V1
Ch06Classes.java         메서드 없이 택배와 배송 이력을 객체로 전환
Ch06Methods.java         객체를 유지한 채 기능별 메서드로 분리
Ch06AdvancedClass.java   생성자 오버로딩, final, getter/setter와 무게 검증 적용
Ch07Inheritance.java     배송 종류별 계산을 상속으로 분리
Ch08Repository.java      저장 기능을 인터페이스와 메모리 구현체로 분리
Ch11Exceptions.java      예외 처리 적용
Ch12DateAndString.java   날짜와 문자열 처리 개선
Ch15Collections.java     표준 컬렉션으로 전환
Ch18FileStorage.java     파일 저장소 적용
Ch20JdbcStorage.java     JDBC 저장소 적용
```

원하는 파일을 지정해 아래 명령으로 실행합니다.

```bash
javac Ch05Arrays.java
java Ch05Arrays
```

`Ch18FileStorage.java`는 실행한 폴더에 `parcel_data.txt`, `parcel_history.txt`를 만듭니다.

`Ch20JdbcStorage.java`는 MySQL JDBC 드라이버와 데이터베이스 설정이 필요합니다. 실행 전 파일의 DB 접속 정보를 수정하고, `Ch20JdbcStorageSchema.sql`을 먼저 실행합니다.
