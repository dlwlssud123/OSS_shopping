[Conceptualization] 결제 및 할인 엔진 (Payment & Discount Engine)
Student Info
Name: 진녕
Major: 컴퓨터공학부 (3학년)
Project Title: OOP 원칙을 적용한 유연한 결제 및 할인 엔진 설계

[ Revision history ]
Revision date	Version #	Description	Author
2026/03/18	1.0.0	초안 작성 및 비즈니스 목적 정의	진녕
2026/03/18	1.0.1	시스템 컨텍스트 다이어그램 및 유즈케이스 구체화	진녕
2026/03/18	1.0.2	운영 개념(Concept of Operation) 및 문제 정의 보완	진녕

= Contents =
Business purpose
System context diagram
Use case list
Concept of operation
Problem statement
Glossary
References

1. Business purpose
1.1 Project Background & Motivation
현대의 이커머스 플랫폼에서 결제 시스템은 단순한 거래 처리를 넘어, 급변하는 마케팅 전략(시즌별 할인, 등급별 혜택 등)을 실시간으로 반영해야 하는 핵심 엔진입니다. 그러나 전통적인 개발 방식에서는 새로운 할인 정책이 추가될 때마다 핵심 결제 로직을 수정해야 하며, 이는 시스템의 안정성을 저해하고 배포 비용을 증가시킵니다. 본 프로젝트는 이러한 비즈니스 가변성에 대응하기 위해 **객체지향 설계 원칙(SOLID)**을 실전 비즈니스 로직에 투영하여, 소스 코드 수정 없이 설정을 통해 정책을 교체할 수 있는 엔진의 표준 모델을 제시하고자 합니다.

1.2 Project Goal
유연한 정책 전환: 비즈니스 요구사항 변경 시 서비스 로직의 수정 없이 구성을 통해 할인 정책을 즉시 교체함.
결합도 최소화: PaymentService가 구체적인 할인 구현체에 의존하지 않도록 설계하여 정책 확장성을 극대화함.
테스트 신뢰성 확보: 인터페이스 기반 설계를 통해 각 할인 로직의 독립적인 단위 테스트(Unit Test) 환경을 구축함.

1.3 Target Market
고도화된 마케팅 전략이 필요한 이커머스 스타트업 개발팀.
객체지향 설계 원칙을 실무 코드에 적용하고자 하는 백엔드 아키텍트.

2. System context diagram
본 시스템은 고객의 주문 요청을 처리하며, 마케터(관리자)의 정책 변경을 설정 파일(AppConfig)을 통해 반영합니다.

코드 스니펫
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

3. Use case list
Actor	Use Case	Description
Customer	등급별 할인 적용	사용자의 등급(VIP, 일반) 정보를 바탕으로 적절한 할인 금액을 계산함.
Customer	최종 금액 산출	상품 가격에서 적용된 할인 금액을 차감하여 실제 결제 금액을 도출함.
Admin	할인 정책 교체	정액 할인에서 정률 할인으로, 혹은 신규 정책으로 시스템 중단 없이 전환함.
System	외부 결제 승인	산출된 최종 금액을 외부 PG사 API를 통해 승인 처리함.

4. Concept of operation
1) 회원 등급별 할인 적용
항목	내용
Purpose	고객 등급에 따른 차등 혜택을 제공하여 고객 충성도를 제고함.
Approach	DiscountPolicy 인터페이스를 호출하면, 현재 주입된 정책(Fix/Rate)에 따라 할인액이 리턴됨.
Dynamics	결제 로직(PaymentService)이 실행되는 시점에 회원 정보를 조회할 경우.
Goals	다형성을 활용하여 동일한 메서드 호출로 서로 다른 할인 결과를 도출함.

2) 할인 정책의 동적 교체 (OCP 적용)
항목	내용
Purpose	마케팅 상황 변화에 따라 소스 코드 수정 없이 시스템 동작을 변경함.
Approach	AppConfig 클래스에서 빈(Bean) 등록 설정을 변경하여 의존관계를 주입(DI)함.
Dynamics	새로운 할인 정책 도입 혹은 기존 정책의 파라미터(할인율 등) 변경 시.
Goals	Open-Closed Principle을 준수하여 확장에는 열려 있고 수정에는 닫힌 구조 구현.

5. Problem statement
5.1 Technical Difficulties
Spring DI 컨테이너 이해: 수동으로 객체를 생성하던 방식에서 벗어나, Spring 프레임워크가 객체 생명주기를 관리하는 IoC(제어의 역전) 환경을 구축하는 데 기술적 학습 곡선이 존재함.
순환 참조 문제: 도메인 간의 의존관계를 잘못 설정할 경우 빈 생성 시 순환 참조 에러가 발생할 수 있으므로 설계 단계에서 주의가 필요함.

5.2 Non-Functional Requirements (NFRs)
성능: 할인 금액 계산 및 검증 로직은 500ms 이내에 완료되어야 함.
확장성: 새로운 DiscountPolicy 구현체 추가 시 기존 Order 및 Payment 관련 코드를 0% 수정해야 함.
정확성: 모든 금액 계산 로직은 1원 단위까지 정확해야 하며, 부동 소수점 오차를 방지하기 위한 처리를 포함함.

6. Glossary
VIP: 높은 매출 기여도로 인해 특별 할인 혜택을 받는 회원 등급.
AppConfig: 실제 객체의 생성과 연결(Dependency Injection)을 담당하는 설정 정보 모듈.
DIP (Dependency Inversion Principle): 고수준 모듈이 저수준 모듈에 의존하지 않고 추상화에 의존해야 한다는 원칙.
OCP (Open-Closed Principle): 기존 코드의 수정 없이 기능을 확장할 수 있어야 한다는 설계 원칙.

7. References
Spring Boot Reference Documentation: https://spring.io/projects/spring-boot
Clean Architecture (Robert C. Martin): SOLID 원칙 및 아키텍처 설계 지침.
JUnit 5 User Guide: https://junit.org/junit5/docs/current/user-guide/
