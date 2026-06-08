# 🛒 OSS Mall - 결제 및 할인 엔진 (Payment & Discount Engine)

본 프로젝트는 이커머스의 본질인 **'상품 데이터 관리'**와 **'지능형 정책 엔진'**에 집중한 오픈소스 몰 엔진 패키지입니다.  
대형 플랫폼의 폐쇄적인 정책 엔진과 기존 오픈소스의 과도한 복잡성을 해결하기 위해 개발되었으며, **단일 실행 가능한 JAR 패키지 안에 백엔드 엔진과 모던 웹 프론트엔드 시뮬레이터가 통합**되어 제공됩니다.

---

## 🛠️ 기술 스택 (Tech Stack)

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Data JPA, Spring Web
- **Frontend**: HTML5, Vanilla CSS, JavaScript (내장 리소스 패키징)
- **Database**: H2 Database (In-Memory 환경으로 즉시 구동)
- **Build Tool**: Maven 3.9+
- **Infrastructure**: Docker, VM 호환 단일 아카이브 구조

---

## 💡 핵심 기능 및 기술적 도전 과제 해결

### 1. 부동 소수점 오차 0% 전략 (`BigDecimal` 정밀 연산)
컴퓨터의 이진 부동 소수점 방식(IEEE 754 표준)으로 인해 금융 수치 연산 시 발생할 수 있는 미세한 오차를 차단하고자, 금액 및 할인율 연산 전체에 `double` 또는 `float` 대신 `BigDecimal`을 필수 채택하여 안정성을 확보하였습니다.

### 2. 우선순위 기반 룰 해석기 (`PolicyResolver`)
- **RateDiscountPolicy** (우선순위 1, VIP 이상 10% 비율 할인, Exclusive = false)
- **FixDiscountPolicy** (우선순위 2, 1,000원 정액 할인, Exclusive = true)
- `PolicyResolver`가 주입된 모든 할인 정책을 `priority` 순서대로 정렬하여 적용합니다.
- `isExclusive()` 속성이 참인 정책이 적용되면 이후 정책 계산은 중단(break)됩니다.
- 할인 합산액이 상품 가격을 초과하여 음수가 될 경우 최종가를 `0원`으로 강제 보정(floor)하는 정밀 필터가 동작합니다.

### 3. 비관적 락을 통한 동시성 제어 (`PESSIMISTIC_WRITE`)
동시 다발적인 한정판 상품 결제 요청 시 발생하는 초과 차감(Oversell)을 원천적으로 막기 위해, `ProductRepository`에 `@Lock(LockModeType.PESSIMISTIC_WRITE)`을 설정하여 데이터베이스 트랜잭션 진입 시점부터 쓰기 잠금을 획득하도록 보장합니다.

### 4. 멱등성 검증 (`IdempotencyKey`)
네트워크 타임아웃 등의 장애로 클라이언트가 결제를 중복 요청하더라도, 주문에 부여된 고유의 `idempotencyKey`를 검증하여 이미 동일 키로 기록된 주문이 존재할 경우 실제 처리를 수행하지 않고 이전의 성공/실패 주문 결과를 그대로 반환합니다.

### 5. 보상 트랜잭션 및 예외 복구
외부 PG사 API 결제 승인 요청 중 오류(한도 초과, 통신 에러 등)가 발생하면, 선차감되었던 상품 재고를 `addStock()` 메서드를 통해 원상복구합니다. 이와 동시에, 실패한 주문의 이력은 분석 및 고객 CS를 위해 데이터베이스에 `FAILED` 상태로 정상 기록(영속화)됩니다.

---

## 📂 패키지 구조 (Project Structure)

```
src/main/java/com/shopping/engine
├── ShoppingEngineApplication.java (메인 부트스트랩)
├── controller
│   ├── OrderController.java (결제 요청 및 조회, 초기화 API)
│   ├── ProductController.java (상품 CRUD REST API - [UC1])
│   ├── PolicyController.java (할인 정책 런타임 수정 API - [UC3])
│   └── dto
│       ├── ItemDto.java
│       ├── OrderRequestDto.java
│       ├── OrderResponseDto.java
│       ├── ProductRequestDto.java
│       └── PolicyUpdateRequestDto.java
├── service
│   ├── OrderService.java (트랜잭션 지휘 및 동시성/보상 제어)
│   └── ProductService.java (상품 정보 CRUD 및 벨리데이션 서비스)
├── domain
│   ├── Customer.java / Grade.java (BASIC, VIP, VVIP)
│   ├── Product.java (재고 비즈니스 로직 및 낙관적 락 버전 포함)
│   ├── Order.java / OrderItem.java / OrderStatus.java (주문 라이프사이클)
│   ├── DiscountPolicySetting.java (정책 설정 영속 엔티티)
│   └── DiscountInfo.java (적용 할인 이력 엔티티)
├── repository
│   ├── CustomerRepository.java
│   ├── ProductRepository.java (비관적 락 선언)
│   ├── OrderRepository.java
│   └── DiscountPolicySettingRepository.java
├── policy
│   ├── DiscountPolicy.java (인터페이스)
│   ├── RateDiscountPolicy.java (등급별 다형 할인 연산)
│   ├── FixDiscountPolicy.java (정액 할인 연산)
│   ├── PolicyResolver.java (우선순위/단독적용 런타임 해석기)
│   └── DemoDataInitializer.java (시뮬레이션 데이터 자동 Seed)
└── infrastructure
    ├── PaymentGateway.java
    └── MockPaymentGateway.java (PG 결제 승인 모의 객체)

src/main/resources/static (웹 프론트엔드 내장 리소스)
├── index.html
├── style.css
├── app.js (캐시 우회 탑재)
└── admin.js (캐시 우회 탑재)
```

---

## 🚀 실행 및 VM 배포 가이드

프론트엔드 리소스가 백엔드 서버(JAR) 내부에 내장되어 있어, 빌드된 JAR 파일 하나 혹은 Docker 컨테이너 하나만으로 웹 사이트와 서버가 즉시 동작합니다.

### 1. 로컬에서 구동 및 테스트하기
```bash
# 1. 빌드 및 패키징 수행 (target/ 밑에 JAR 파일 생성됨)
mvn clean package

# 2. 애플리케이션 실행
java -jar target/engine-0.0.1-SNAPSHOT.jar

# (대안) 빌드 도구를 통한 직접 실행
mvn spring-boot:run
```
실행이 완료되면 브라우저에서 **`http://localhost:8080`**으로 접속하여 웹 시뮬레이터에 진입할 수 있습니다.

### 2. 가상 머신(VM) 및 Docker 배포하기
VM 인스턴스에 접속하여 프로젝트를 다운로드한 후, Docker를 통해 원격 배포를 한 번에 완료할 수 있습니다.
```bash
# 1. 로컬 또는 빌드 서버에서 JAR 파일 빌드 후 도커 이미지 빌드
docker build -t shopping-engine .

# 2. 도커 컨테이너 기동 (8080 포트 오픈)
docker run -d -p 8080:8080 --name order-system shopping-engine
```
VM 서버의 방화벽(인바운드 규칙)에서 `8080` 포트를 개방한 후, 브라우저에서 `http://<VM_외부IP>:8080`으로 접속하면 로컬과 동일하게 결제 및 할인 로직을 테스트해 보실 수 있습니다.

---

## ⚡ 주요 API 명세 요약

### 상품 관리 및 검색

- `GET /api/products`: 전체 상품 조회
- `GET /api/products?keyword=맥북`: 상품명 검색
- `GET /api/products/{productId}`: 상품 상세 조회
- `POST /api/products`: 상품 등록
- `PUT /api/products/{productId}`: 상품 수정
- `DELETE /api/products/{productId}`: 상품 삭제

상품 수정에는 `Product.version`의 `@Version` 기반 optimistic locking을 사용하고, 결제/취소 재고 변경에는 `PESSIMISTIC_WRITE`를 사용한다.

### 할인 정책 설정

- `GET /api/policies`: 현재 할인 정책 설정 조회
- `PUT /api/policies/RATE`: VIP/VVIP 비율 할인 정책 수정
- `PUT /api/policies/FIX`: 정액 할인 정책 수정

예시 요청:

```json
{
  "enabled": true,
  "priority": 1,
  "exclusive": false,
  "discountRate": 0.15,
  "discountAmount": null
}
```

`PolicyResolver`는 매 계산 시 최신 priority와 enabled 설정을 반영한다.

### 주문 취소

- `POST /api/orders/{orderId}/cancel`: `COMPLETE` 주문을 `CANCELED`로 전이하고 차감된 재고를 복구한다.

### 1. 데모 데이터 초기화 (POST)
- **Endpoint**: `/api/init`
- **Description**: 테스트에 필요한 기본 회원(BASIC, VIP, VVIP 등급) 및 상품 데이터(재고 상태)를 H2 DB에 초기 생성합니다.

### 2. 결제 승인 요청 (POST)
- **Endpoint**: `/api/orders`
- **Request Body**:
  ```json
  {
    "customerId": 2,
    "items": [
      { "productId": 1, "quantity": 1 }
    ],
    "idempotencyKey": "KEY-A1B2-C3D4-E5F6"
  }
  ```
- **Response Body (성공 시)**:
  ```json
  {
    "orderId": 1,
    "idempotencyKey": "KEY-A1B2-C3D4-E5F6",
    "status": "COMPLETE",
    "originalAmount": 3000000.00,
    "discountAmount": 301000.00,
    "finalAmount": 2699000.00,
    "receiptId": "REC-EF84C7B2",
    "errorMessage": null
  }
  ```

---

## 🧪 테스트 코드 구동 및 검증
- **`PolicyResolverTest.java`**: VIP 비율 할인 적용, 중복 정책 적용 우선순위, 정액 할인 Exclusive 동작 및 0원 하한 보정 기능 검증.
- **`OrderServiceConcurrencyTest.java`**: 멀티스레드 환경에서 30개의 스레드가 동시에 재고 10개인 한정 상품 결제를 시도할 때, 정확히 10건만 성공하고 20건이 재고 부족 예외를 받으며, FAILED 주문으로 DB에 이력이 적재되고 재고는 0으로 안전하게 잠금 처리됨을 검증.

```bash
# 테스트 명령어 실행
mvn test
```
