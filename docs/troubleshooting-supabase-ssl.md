# Render → Supabase SSL 연결 실패 트러블슈팅

2026-11-09 ~ 11-10, Render 배포에서 앱이 Supabase에 SSL로 연결하지 못해 기동에 실패한 문제를 정리한다.

- 환경: Render 무료 인스턴스(Singapore, 0.1 CPU · 512MB) + Docker(`eclipse-temurin:21-jre-alpine`) + Spring Boot 3.5 + Flyway 11 + pgjdbc 42.7.11 + Supabase Session pooler
- 결론: **CPU가 부족해 첫 TLS 핸드셰이크가 pooler의 대기 시간 안에 끝나지 못했다.** 기동 시 Flyway가 DB 연결을 재시도하도록 해서 해결했다.

## 1. 증상

배포할 때마다 Flyway가 첫 DB 연결을 맺다가 실패하고, 컨테이너가 `Exited with status 1`로 종료된다.

```
Caused by: org.postgresql.util.PSQLException: SSL error: Remote host terminated the handshake
Caused by: javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake
Caused by: java.io.EOFException: SSL peer shut down incorrectly
```

첫 배포(11-09 08:17)부터 `sslmode=require`로는 단 한 번도 성공하지 못했다.

## 2. 연결 방식 선택

Supabase 대시보드의 **Connect**에서 고르는 값이다.

| 항목 | 선택 | 이유 |
| --- | --- | --- |
| Type | JDBC | Spring Boot이고 `DB_URL`이 `jdbc:postgresql://...` 형식을 기대한다 |
| Method | **Session pooler** (포트 `5432`) | Direct connection은 IPv6 전용인데 Render는 IPv4만 된다. Transaction pooler(`6543`)는 Flyway의 세션 advisory lock, JDBC prepared statement와 충돌한다 |

Supabase가 주는 JDBC 문자열의 `user`와 `password`는 URL에서 빼서 환경변수로 따로 넣는다. `sslmode=require`는 남긴다.

```
DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<DB 비밀번호>
```

## 3. 조사 과정

### 3.1 `sslmode=disable` — 임시로 됐다가 다시 막힘

| 시각 (UTC) | 설정 | 결과 |
| --- | --- | --- |
| 11-10 04:37 | `sslmode=disable` | 성공. 평문 통신이라 임시 조치였다 |
| 11-10 05:41 | `sslmode=disable` | `FATAL: (ESSLREQUIRED) SSL connection is required` — 그사이 Supabase의 SSL 강제가 켜졌다 |
| 11-10 05:42 | `sslmode=require` | 다시 `Remote host terminated the handshake` |

`ESSLREQUIRED`는 Supabase가 SSL을 지원하고, 오히려 강제한다는 뜻이다. SSL을 지원하지 않는 서버였다면 `The server does not support SSL`이 나왔을 것이다. 서버가 SSL을 받아들이고 핸드셰이크를 시작했다가 도중에 끊은 것이다.

### 3.2 원인 범위 좁히기 — Render 밖에서는 정상

같은 pooler에 로컬 PC에서 접속해 봤다. 틀린 비밀번호로 접속해서 에러가 비밀번호 단계에서 나면, SSL은 통과한 것이다.

| 위치 | 클라이언트 | 계정 | 결과 |
| --- | --- | --- | --- |
| 로컬 | `openssl s_client -starttls postgres` | — | TLS 1.2 · 1.3 모두 정상 |
| 로컬 | Java 21 + pgjdbc 42.7.11 | 실제 계정 | `password authentication failed` → SSL 통과 |
| 로컬 Docker | `eclipse-temurin:21-jre-alpine` (운영과 같은 이미지) | 실제 계정 | `password authentication failed` → SSL 통과 |
| Render | 같은 이미지 | 실제 계정 | 핸드셰이크 끊김 |

코드, 드라이버, Docker 이미지, Supabase 계정 설정은 원인에서 빠졌다. 실패가 쌓여 차단(ban)된 것도 아니다. 첫 배포부터 같은 증상이었다.

### 3.3 Render에서 TLS 디버그 로그 켜기

Render 환경변수에 `JAVA_TOOL_OPTIONS=-Djavax.net.debug=ssl:handshake`를 넣고 배포해 핸드셰이크를 단계별로 봤다.

**첫 시도 (05:55)**

```
05:55:29  Ignore disabled cipher suite ...           ← SSL 엔진 초기화 시작
05:55:32.535  Produced ClientHello (466 bytes)       ← 약 3초 뒤에야 ClientHello 전송
05:55:32.631  Remote host terminated the handshake   ← 서버 응답 없이 끊김
```

ClientHello 내용(SNI, cipher, key share)은 평범했다. 서버가 SSL 요청을 수락한 뒤 클라이언트가 SSL 엔진을 준비하는 데 3초가 걸렸고, 그사이 pooler가 기다리다 끊었다.

**재시도 (06:02, SSL 엔진이 이미 준비된 상태)**

```
06:02:24.973  WRITE ClientHello
06:02:24.976  READ ServerHello                ← 서버는 3ms 만에 응답
06:02:25.475  Consuming server Certificate    ← 인증서 처리에 0.4초
06:02:25.877  Produced ClientKeyExchange
06:02:26.069  Produced ChangeCipherSpec → Broken pipe
```

서버는 즉시 응답했지만 클라이언트 쪽 처리가 1초 넘게 걸려 결국 끊겼다. 앱 기동 자체도 146~163초가 걸렸다. 그만큼 CPU가 부족하다.

### 3.4 효과가 없었던 시도

| 시도 | 결과 |
| --- | --- |
| `-Djdk.tls.client.protocols=TLSv1.2` (TLS 1.2 고정) | 똑같이 끊김 |
| Transaction pooler(`6543`) + `prepareThreshold=0` | 똑같이 끊김 |

## 4. 원인

Render 무료 인스턴스는 CPU가 0.1개다. Spring이 기동하는 동안 CPU가 포화된 상태에서 Java가 첫 TLS 연결을 맺으려면 SSL 엔진 초기화(cipher 목록, 인증서 저장소 로딩)와 인증서 검증에 몇 초가 걸린다. Supabase pooler는 핸드셰이크가 늦어지면 연결을 끊고, 그 시간은 우리가 바꿀 수 없다.

로컬에서는 CPU가 충분해서 같은 이미지로도 재현되지 않았다.

## 5. 해결

Flyway가 기동 시 DB 연결을 재시도하도록 했다. 두 번째 시도부터는 SSL 엔진이 메모리에 준비돼 있어서 핸드셰이크가 제시간에 끝난다.

`application-prod.yml`:

```yaml
spring:
  flyway:
    connect-retries: 10
    connect-retries-interval: 5s
```

처음에는 코드를 바꾸지 않고 Render 환경변수로 검증했다. 지금도 같은 값이 남아 있다.

```
SPRING_FLYWAY_CONNECT_RETRIES=10           # 실패하면 최대 10번 재시도
SPRING_FLYWAY_CONNECT_RETRIES_INTERVAL=5s  # 재시도 간격 상한
```

배포 로그(06:06)에서 첫 시도가 끊기고 재시도에서 연결된 것을 확인했다.

```
06:06:23  Connection error: SSL error: Remote host terminated the handshake
06:06:23  Retrying in 1 sec...
06:06:29  Database: jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres
06:07:52  Started GdgocPknuBeApplication in 163.595 seconds
```

`GET /api/health` → `{"status":"UP","db":"UP"}`

앱이 뜬 뒤 Hikari가 새 연결을 맺을 때는 SSL 엔진이 이미 준비돼 있고 CPU에도 여유가 있어 문제가 없다.

## 6. 최종 설정

| 환경변수 | 값 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `postgres.<project-ref>` |
| `DB_PASSWORD` | DB 비밀번호 |
| `SPRING_FLYWAY_CONNECT_RETRIES` | `10` |
| `SPRING_FLYWAY_CONNECT_RETRIES_INTERVAL` | `5s` |

Supabase의 SSL 강제(Enforce SSL)는 켜 둔다.

## 7. 다시 생기면

- 재시도 설정은 `application-prod.yml`의 `spring.flyway.connect-retries`에도 있다. Render 환경변수(`SPRING_FLYWAY_CONNECT_RETRIES*`)는 같은 값이라 지워도 된다. yml의 재시도 설정은 지우지 않는다.
- `sslmode=disable`로 우회하지 않는다. Supabase가 SSL을 강제하고 있어 `ESSLREQUIRED`로 실패하고, 통과하더라도 공용 인터넷 구간이 평문이 된다.
- 원인을 다시 볼 때는 `JAVA_TOOL_OPTIONS=-Djavax.net.debug=ssl:handshake`로 핸드셰이크 로그를 켠다. 로그가 많고 CPU를 잡아먹으니 확인이 끝나면 지운다.
- 유료 인스턴스로 올리면 CPU가 늘어 첫 시도부터 붙을 가능성이 높다. 재시도 설정은 그대로 둬도 해가 없다.
