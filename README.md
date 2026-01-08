# Service Chat

Passit 프로젝트의 실시간 채팅 및 메시징 마이크로서비스입니다.

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [API 문서](#api-문서)
- [환경 변수](#환경-변수)
- [개발 규칙](#개발-규칙)

## 프로젝트 개요

Service Chat은 Passit 플랫폼의 실시간 채팅 및 거래 메시징을 담당하는 독립적인 마이크로서비스입니다.

### 주요 책임

- 실시간 1:1 채팅 (판매자-구매자)
- 채팅방 생성 및 관리
- 메시지 전송 및 저장
- 거래 관련 시스템 메시지
- 채팅방 상태 관리
- 읽음/안읽음 처리

## 주요 기능

### 채팅방 관리

- **채팅방 생성**: 티켓 ID 기반 자동 채팅방 생성
- **채팅방 목록**: 사용자별 참여 채팅방 조회
- **채팅방 상태**: 활성/비활성/종료 상태 관리
- **멤버 관리**: 채팅방 참여자 관리

### 메시지 관리

- **메시지 전송**: 텍스트 메시지 실시간 전송
- **메시지 조회**: 채팅방별 메시지 히스토리
- **시스템 메시지**: 거래 상태 변경 알림 메시지
- **읽음 처리**: 메시지 읽음 상태 업데이트

### 실시간 통신

- **WebSocket 연결**: STOMP 프로토콜 기반 실시간 통신
- **메시지 브로드캐스트**: 채팅방 내 실시간 메시지 전달
- **연결 상태 관리**: 사용자 온라인/오프라인 상태

## 기술 스택

### Backend

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Data JPA**: 데이터베이스 접근
- **Spring WebSocket**: 실시간 통신
- **STOMP**: 메시지 프로토콜
- **Spring WebFlux**: 비동기 HTTP 클라이언트
- **Lombok**: 보일러플레이트 코드 감소

### Database

- **MySQL**: 8.0+
- **Redis**: 7.x (메시지 캐싱)

### Testing

- **JUnit 5**: 단위 테스트
- **Mockito**: Mock 객체 생성

### Build Tool

- **Gradle**: 8.5

## 시작하기

### 사전 요구사항

다음 도구들이 설치되어 있어야 합니다:

| 도구           | 버전                      | 설치 링크                                                |
| -------------- | ------------------------- | -------------------------------------------------------- |
| IntelliJ IDEA  | 최신 (Community/Ultimate) | [JetBrains](https://www.jetbrains.com/idea/)             |
| JDK            | 17                        | [Adoptium](https://adoptium.net)                         |
| Docker Desktop | 최신                      | [Docker](https://www.docker.com/products/docker-desktop) |
| Git            | 최신                      | [Git](https://git-scm.com)                               |

### 로컬 개발 환경 설정

#### 1. 리포지토리 클론

```bash
git clone https://github.com/your-org/service-chat.git
cd service-chat
```

#### 2. 데이터베이스 시작 (Docker Compose)

```bash
docker-compose up -d
```

MySQL과 Redis 컨테이너가 시작됩니다.

#### 3. 환경 변수 설정

필요한 경우 환경 변수를 설정하세요:

```bash
# 데이터베이스
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=passit_db
export DB_USER=passit_user
export DB_PASSWORD=passit_password

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# 서비스 통신
export TICKET_SERVICE_URL=http://localhost:8082
```

#### 4. 애플리케이션 실행

**방법 1: Gradle로 실행**

```bash
./gradlew bootRun
```

**방법 2: IntelliJ IDEA**

1. 프로젝트를 IntelliJ IDEA로 열기
2. `src/main/java/com/company/service_chat/Service_chatApplication.java` 실행
3. Run Configuration에서 환경 변수 설정 가능

애플리케이션은 `http://localhost:8083`에서 실행됩니다.

#### 5. Health Check 확인

```bash
curl http://localhost:8083/api/health
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "service-chat"
  },
  "error": null
}
```

### 빌드 및 테스트

#### 전체 빌드

```bash
./gradlew clean build
```

#### 단위 테스트 실행

```bash
./gradlew test
```

#### 테스트 리포트 확인

```bash
# 리포트 확인: build/reports/tests/test/index.html
open build/reports/tests/test/index.html
```

## API 문서

### 공통 응답 형식

모든 API는 다음 형식으로 응답합니다:

#### 성공 응답

```json
{
  "success": true,
  "data": {
    /* 실제 데이터 */
  },
  "message": "성공 메시지",
  "error": null
}
```

#### 오류 응답

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": "에러 메시지"
}
```

### 주요 엔드포인트

#### 채팅방 API (`/api/rooms`)

| Method | Endpoint                | 설명                  | 인증 필요 |
| ------ | ----------------------- | --------------------- | --------- |
| POST   | `/`                     | 채팅방 생성           | ✅        |
| GET    | `/my`                   | 내 채팅방 목록        | ✅        |
| GET    | `/{roomId}`             | 채팅방 상세 조회      | ✅        |
| GET    | `/{roomId}/messages`    | 채팅방 메시지 조회    | ✅        |
| POST   | `/{roomId}/members`     | 채팅방 멤버 추가      | ✅        |
| DELETE | `/{roomId}/members`     | 채팅방 나가기         | ✅        |

#### 메시지 API (WebSocket)

| Destination              | 설명              | 인증 필요 |
| ------------------------ | ----------------- | --------- |
| `/app/chat.send`         | 메시지 전송       | ✅        |
| `/topic/room/{roomId}`   | 채팅방 구독       | ✅        |

#### 거래 관련 API (`/api/deals`)

| Method | Endpoint                | 설명                  | 인증 필요 |
| ------ | ----------------------- | --------------------- | --------- |
| POST   | `/{dealId}/status`      | 거래 상태 변경 알림   | ✅        |

#### Health Check

| Method | Endpoint           | 설명                     |
| ------ | ------------------ | ------------------------ |
| GET    | `/api/health`      | 서비스 헬스 체크         |
| GET    | `/actuator/health` | Spring Actuator 헬스 체크 |

### WebSocket 연결

#### 연결 엔드포인트

```
ws://localhost:8083/ws-chat
```

#### 메시지 전송 예시

```javascript
// STOMP 클라이언트로 연결
const socket = new SockJS('http://localhost:8083/ws-chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // 채팅방 구독
    stompClient.subscribe('/topic/room/123', function(message) {
        console.log(JSON.parse(message.body));
    });
    
    // 메시지 전송
    stompClient.send("/app/chat.send", {}, JSON.stringify({
        'roomId': 123,
        'message': 'Hello!'
    }));
});
```

## 환경 변수

### 데이터베이스

| 변수          | 설명              | 기본값          |
| ------------- | ----------------- | --------------- |
| `DB_HOST`     | MySQL 호스트      | localhost       |
| `DB_PORT`     | MySQL 포트        | 3306            |
| `DB_NAME`     | 데이터베이스 이름 | passit_db       |
| `DB_USER`     | DB 사용자         | passit_user     |
| `DB_PASSWORD` | DB 비밀번호       | passit_password |

### Redis

| 변수            | 설명          | 기본값     |
| --------------- | ------------- | ---------- |
| `REDIS_HOST`    | Redis 호스트  | localhost  |
| `REDIS_PORT`    | Redis 포트    | 6379       |
| `REDIS_PASSWORD`| Redis 비밀번호| (없음)     |

### 서비스 통신

| 변수                  | 설명                 | 기본값                 |
| --------------------- | -------------------- | ---------------------- |
| `TICKET_SERVICE_URL`  | Ticket 서비스 URL    | http://localhost:8082  |

## 프로젝트 구조

```
service-chat/
├── src/
│   ├── main/
│   │   ├── java/com/company/service_chat/
│   │   │   ├── config/              # 설정 클래스 (WebSocket, Redis 등)
│   │   │   ├── controller/          # REST API 컨트롤러
│   │   │   ├── dto/                 # 요청/응답 DTO
│   │   │   ├── entity/              # JPA 엔티티
│   │   │   ├── exception/           # 커스텀 예외 및 핸들러
│   │   │   ├── repository/          # JPA Repository
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   └── Service_chatApplication.java
│   │   └── resources/
│   │       ├── application.yml      # 애플리케이션 설정
│   │       ├── application-dev.yml  # Dev 환경 설정
│   │       └── application-prod.yml # Prod 환경 설정
│   └── test/
│       ├── java/                    # 테스트 코드
│       └── resources/
├── build.gradle                     # Gradle 빌드 설정
├── docker-compose.yml               # Docker Compose 설정
├── helm/                            # Helm 차트
├── k8s/                             # Kubernetes 매니페스트
├── scripts/                         # 유틸리티 스크립트
└── README.md
```

## 개발 규칙

### 코드 스타일

- Java Code Convention 준수
- Lombok 적극 활용
- Layer 간 의존성 방향: Controller → Service → Repository
- DTO 사용으로 Entity 노출 방지
- 모든 API 응답은 `ApiResponse` 래퍼 사용

### 브랜치 전략

- `main`: 운영 환경 배포 브랜치
- `develop`: 개발 환경 통합 브랜치
- `feature/{기능명}`: 기능 개발 브랜치
- `bugfix/{버그명}`: 버그 수정 브랜치

### 커밋 메시지 규칙

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드, 설정 파일 수정
```

### Pull Request 규칙

1. PR 생성 시 템플릿 작성
2. 최소 1명 이상 리뷰어 지정
3. CI 성공 필수
4. 승인 후 Squash Merge

## 트러블슈팅

### 빌드 실패

```bash
# Gradle 캐시 삭제
./gradlew clean

# Gradle Wrapper 재설치
./gradlew wrapper
```

### 데이터베이스 연결 실패

```bash
# Docker 컨테이너 상태 확인
docker-compose ps

# MySQL 로그 확인
docker-compose logs mysql

# 컨테이너 재시작
docker-compose restart mysql
```

### WebSocket 연결 실패

- CORS 설정 확인
- 방화벽 설정 확인
- 프록시 서버 WebSocket 지원 여부 확인

### Redis 연결 실패

```bash
# Redis 컨테이너 상태 확인
docker-compose ps redis

# Redis 로그 확인
docker-compose logs redis

# Redis 연결 테스트
redis-cli -h localhost -p 6379 ping
```

## 배포

### Docker 빌드

```bash
docker build -t chat-service:latest .
```

### Kubernetes 배포

```bash
kubectl apply -f k8s/
```

### Helm 배포

```bash
helm install chat-service ./helm
```

## 라이선스

MIT License

## 문의

프로젝트 관련 문의사항은 팀 PM에게 연락하세요.
