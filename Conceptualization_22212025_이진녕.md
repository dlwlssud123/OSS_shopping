# [Conceptualization] 🛒 결제 및 할인 엔진 (Payment & Discount Engine)

| 항목 | 내용 |
| :--- | :--- |
| **Student No** | 22212025 |
| **Name** | 이진녕 |
| **E-mail** | vbnm9247@naver.com |

**Project Title: OOP 원칙을 적용한 유연한 결제 및 할인 엔진 설계**

---

## [ Revision history ]

| Revision date | Version # | Description | Author |
| :--- | :--- | :--- | :--- |
| 2026/03/17 | 1.0.0 | 초안 작성 및 비즈니스 목적 정의 | 진녕 |
| 2026/03/18 | 1.0.1 | 시스템 컨텍스트 다이어그램 및 유즈케이스 구체화 | 진녕 |
| 2026/03/18 | 1.0.2 | 운영 개념(Concept of Operation) 및 문제 정의 보완 | 진녕 |

---

## Contents
1. Business purpose
2. System context diagram
3. Use case list
4. Concept of operation
5. Problem statement
6. Core classes
7. Glossary
8. References

---

## 1. Business purpose

### 1.1 Project Background & Motivation
현대의 이커머스 플랫폼에서 결제 시스템은 단순한 거래 처리를 넘어, 급변하는 마케팅 전략(시즌별 할인, 등급별 혜택 등)을 실시간으로 반영해야 하는 핵심 엔진입니다. 그러나 전통적인 개발 방식에서는 새로운 할인 정책이 추가될 때마다 핵심 결제 로직을 수정해야 하며, 이는 시스템의 안정성을 저해하고 배포 비용을 증가시킵니다. [cite_start]본 프로젝트는 이러한 비즈니스 가변성에 대응하기 위해 **객체지향 설계 원칙(SOLID)**을 실전 비즈니스 로직에 투영하여, 소스 코드 수정 없이 설정을 통해 정책을 교체할 수 있는 엔진의 표준 모델을 제시하고자 합니다. [cite: 1463]

### 1.2 Project Goal
* [cite_start]**유연한 정책 전환**: 비즈니스 요구사항 변경 시 서비스 로직의 수정 없이 구성을 통해 할인 정책을 즉시 교체함. [cite: 1463]
* [cite_start]**결합도 최소화**: `PaymentService`가 구체적인 할인 구현체에 의존하지 않도록 설계하여 정책 확장성을 극대화함. [cite: 1463]
* [cite_start]**테스트 신뢰성 확보**: 인터페이스 기반 설계를 통해 각 할인 로직의 독립적인 단위 테스트(Unit Test) 환경을 구축함. [cite: 1463]

### 1.3 Target Market
* [cite_start]고도화된 마케팅 전략이 필요한 이커머스 스타트업 개발팀. [cite: 1463]
* [cite_start]객체지향 설계 원칙을 실무 코드에 적용하고자 하는 백엔드 아키텍트. [cite: 1463]

---

## 2. System context diagram

[cite_start]본 시스템은 고객의 주문 요청을 처리하며, 마케터(관리자)의 정책 변경을 설정 파일(`AppConfig`)을 통해 반영합니다. [cite: 1464]

```mermaid
flowchart TD
    classDef main fill:#E3F2FD,stroke:#1565C0,stroke-width:4px,font-size:18px,font-weight:bold;
    classDef actor fill:#FFFDE7,stroke:#FBC02D,stroke-width:2px;

    Customer["👤 고객<br/>(Customer)"]:::actor
    Admin["👤 관리자/마케터<br/>(Admin/Marketer)"]:::actor
    System["⚙️ 결제 & 할인 엔진<br/>(System)"]:::main
    PG["🏦 외부 PG사<br/>(Payment Gateway)"]

    Customer ==>|"1. 결제 요청"| System
    Admin -.->|"2. 할인 정책 변경 및 설정"| System
    System ==>|"3. 최종 금액 승인 요청"| PG
    PG ==>|"4. 승인 결과 반환"| System
    System ==>|"5. 결제 완료 알림"| Customer
    ```
---

## 3. Use case list

### 3.1 회원 관리 (Member Management)
* **Actor**: 고객 (Customer)
* **Description**: 회원은 서비스에 가입할 수 있으며, 등급(VIP, BASIC)에 따라 다른 할인 혜택을 부여받습니다.

### 3.2 주문 생성 (Order Creation)
* **Actor**: 고객 (Customer)
* **Description**: 상품을 선택하여 주문을 생성합니다. 시스템은 회원의 등급을 확인하여 적절한 할인 금액을 산출합니다.

### 3.3 할인 정책 변경 (Policy Management)
* **Actor**: 관리자/마케터 (Admin)
* **Description**: 시스템 중단 없이 설정 파일(`AppConfig`)을 통해 할인 정책(고정 금액 할인 vs 정률 할인)을 실시간으로 교체합니다.

---

## 4. Concept of operation

1. **초기화 단계**: `AppConfig`가 실제 서비스에서 사용할 구현체(예: `RateDiscountPolicy`, `MemoryMemberRepository`)를 결정하고 객체를 생성하여 주입(DI)합니다.
2. **요청 처리 단계**: `OrderService`는 고객의 주문 요청이 들어오면 `MemberRepository`를 통해 고객 정보를 조회합니다.
3. **정책 적용 단계**: 조회된 회원 등급 정보를 `DiscountPolicy` 인터페이스에 전달합니다. 이때 주입된 실제 구현체가 할인 금액을 계산합니다.
4. **결과 반환 단계**: 할인 금액이 차감된 최종 주문 객체(`Order`)를 생성하여 외부 시스템(PG)에 승인을 요청하거나 고객에게 반환합니다.

---

## 5. Problem statement

1. **가변적인 비즈니스 요구사항**: 마케팅 정책이 빈번하게 변경됨에 따라 기존 결제 코드를 계속 수정해야 하는 **Open-Closed Principle (OCP)** 위반 문제 발생.
2. **높은 결합도**: 주문 로직이 특정 할인 정책(예: 고정 할인)에 직접 의존할 경우, 새로운 정책(예: 정률 할인) 도입 시 대규모 코드 수정 불가피.
3. **의존관계 역전의 필요성**: 고수준 모듈(`OrderService`)이 저수준 모듈(`FixDiscountPolicy`)에 의존하지 않고, 추상화(`DiscountPolicy`)에 의존하도록 설계하여 **Dependency Inversion Principle (DIP)**을 준수해야 함.

---

## 6. Core classes

| Class / Interface | Role | Description |
| :--- | :--- | :--- |
| **Member** | Domain Entity | 회원 ID, 이름, 등급(Grade) 정보를 보유한 엔티티 |
| **MemberRepository** | Interface | 회원 데이터를 저장하고 조회하는 표준 인터페이스 |
| **MemoryMemberRepository** | Implementation | DB 없이 메모리상에서 회원 정보를 관리하는 가상 저장소 |
| **MemberService** | Service | 회원 가입 및 정보 조회 비즈니스 로직 담당 |
| **DiscountPolicy** | Interface | 할인 정책을 추상화한 인터페이스 (할인액 계산 기능) |
| **FixDiscountPolicy** | Implementation | 정액 할인 정책 (예: VIP는 무조건 1,000원 할인) |
| **RateDiscountPolicy** | Implementation | 정률 할인 정책 (예: VIP는 결제 금액의 10% 할인) |
| **Order** | Domain Entity | 주문 정보를 담는 객체 (회원ID, 상품명, 가격, 할인금액 등) |
| **OrderService** | Service | 주문 생성 시 할인 정책을 호출하여 최종 주문서 작성 |
| **AppConfig** | Configurator | 애플리케이션의 실제 의존 관계를 연결하는 **DI 컨테이너** |

---

## 7. Glossary

* **DI (Dependency Injection)**: 객체 간의 의존 관계를 외부에서 주입하여 결합도를 낮추는 기법.
* **SOLID**: 객체지향 설계의 5가지 핵심 원칙 (단일 책임, 개방-폐쇄, 리스코프 치환, 인터페이스 분리, 의존관계 역전).
* **Grade (VIP/BASIC)**: 할인 혜택 적용의 기준이 되는 사용자 등급.

---

## 8. References

* 김영한, "스프링 핵심 원리 - 기본편", 인프런 강의.
* Robert C. Martin, "Clean Architecture: A Craftsman's Guide to Software Structure and Design".