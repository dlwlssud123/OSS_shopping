package com.shopping.engine.policy;

import com.shopping.engine.domain.Customer;
import com.shopping.engine.domain.DiscountPolicySetting;
import com.shopping.engine.domain.Grade;
import com.shopping.engine.domain.OrderItem;
import com.shopping.engine.domain.Product;
import com.shopping.engine.repository.DiscountPolicySettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyResolverTest {

    private PolicyResolver policyResolver;

    @BeforeEach
    void setUp() {
        DiscountPolicySettingRepository mockRepo = org.mockito.Mockito.mock(DiscountPolicySettingRepository.class);
        DiscountPolicySetting rateSetting = new DiscountPolicySetting(
                "RATE", "VIP Grade Rate Discount", true, 1, false, new BigDecimal("0.10"), BigDecimal.ZERO);
        DiscountPolicySetting fixSetting = new DiscountPolicySetting(
                "FIX", "Fix Discount", true, 2, true, BigDecimal.ZERO, new BigDecimal("1000.00"));
        
        org.mockito.Mockito.when(mockRepo.findById("RATE")).thenReturn(java.util.Optional.of(rateSetting));
        org.mockito.Mockito.when(mockRepo.findById("FIX")).thenReturn(java.util.Optional.of(fixSetting));
        
        DiscountPolicySettings settings = new DiscountPolicySettings(mockRepo);
        DiscountPolicy ratePolicy = new RateDiscountPolicy(settings);
        DiscountPolicy fixPolicy = new FixDiscountPolicy(settings);
        policyResolver = new PolicyResolver(Arrays.asList(fixPolicy, ratePolicy));
    }

    @Test
    @DisplayName("BASIC 등급 고객은 VIP 10% 할인을 받지 못하고 1000원 정액 할인만 적용된다")
    void basicCustomerDiscount() {
        // given
        Customer customer = new Customer("BasicUser", Grade.BASIC);
        Product product = new Product("TestProduct", new BigDecimal("10000.00"), 10);
        OrderItem orderItem = new OrderItem(product, 1, product.getPrice());
        List<OrderItem> items = Collections.singletonList(orderItem);

        // when
        BigDecimal totalDiscount = policyResolver.applyDiscountRules(customer, items);

        // then
        // VIP 할인은 적용 안 됨 -> 1000원 정액 할인만 적용
        assertThat(totalDiscount).isEqualByComparingTo("1000.00");
        assertThat(orderItem.getAppliedDiscounts()).hasSize(1);
        assertThat(orderItem.getAppliedDiscounts().get(0).getPolicyName()).contains("Fix Discount");
    }

    @Test
    @DisplayName("VIP 등급 고객은 10% 비율 할인과 1000원 정액 할인이 순차 적용된다 (FixDiscountPolicy가 Exclusive이므로 2단계에서 중단)")
    void vipCustomerDiscount() {
        // given
        Customer customer = new Customer("VipUser", Grade.VIP);
        Product product = new Product("MacBook", new BigDecimal("10000.00"), 10);
        OrderItem orderItem = new OrderItem(product, 1, product.getPrice());
        List<OrderItem> items = Collections.singletonList(orderItem);

        // when
        BigDecimal totalDiscount = policyResolver.applyDiscountRules(customer, items);

        // then
        // 10000원 의 10% = 1000원 할인 (RateDiscount)
        // 남은 금액 9000원 에서 1000원 추가 정액 할인 (FixDiscount - Exclusive true)
        // 총 할인액 = 2000원
        assertThat(totalDiscount).isEqualByComparingTo("2000.00");
        assertThat(orderItem.getAppliedDiscounts()).hasSize(2);
    }

    @Test
    @DisplayName("할인 금액이 상품 가격보다 큰 경우, 총 할인액은 상품 원가로 보정(floor)되어 최종가는 0원이 된다")
    void discountFloorCorrection() {
        // given
        Customer customer = new Customer("BasicUser", Grade.BASIC);
        // 500원 짜리 양말 구매
        Product product = new Product("Socks", new BigDecimal("500.00"), 10);
        OrderItem orderItem = new OrderItem(product, 1, product.getPrice());
        List<OrderItem> items = Collections.singletonList(orderItem);

        // when
        // 1000원 정액할인 적용 시도
        BigDecimal totalDiscount = policyResolver.applyDiscountRules(customer, items);

        // then
        // 할인액은 원가인 500원을 초과할 수 없음
        assertThat(totalDiscount).isEqualByComparingTo("500.00");
        assertThat(orderItem.getAppliedDiscounts().get(0).getDiscountAmount()).isEqualByComparingTo("500.00");
        
        // 최종가 계산 검증
        BigDecimal finalPrice = orderItem.getOriginalPrice().subtract(totalDiscount);
        assertThat(finalPrice).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("정책 설정에서 RATE 할인을 비활성화하면 VIP도 정액 할인만 적용된다")
    void dynamicPolicySettingDisablesRatePolicy() {
        // given
        DiscountPolicySettingRepository mockRepo = org.mockito.Mockito.mock(DiscountPolicySettingRepository.class);
        DiscountPolicySetting rateSetting = new DiscountPolicySetting(
                "RATE", "VIP Grade Rate Discount", true, 1, false, new BigDecimal("0.10"), BigDecimal.ZERO);
        DiscountPolicySetting fixSetting = new DiscountPolicySetting(
                "FIX", "Fix Discount", true, 2, true, BigDecimal.ZERO, new BigDecimal("1000.00"));
        
        org.mockito.Mockito.when(mockRepo.findById("RATE")).thenReturn(java.util.Optional.of(rateSetting));
        org.mockito.Mockito.when(mockRepo.findById("FIX")).thenReturn(java.util.Optional.of(fixSetting));
        org.mockito.Mockito.when(mockRepo.save(org.mockito.Mockito.any(DiscountPolicySetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscountPolicySettings settings = new DiscountPolicySettings(mockRepo);
        settings.update("RATE", false, null, null, null, null);
        
        PolicyResolver resolver = new PolicyResolver(Arrays.asList(
                new FixDiscountPolicy(settings),
                new RateDiscountPolicy(settings)
        ));

        Customer customer = new Customer("VipUser", Grade.VIP);
        Product product = new Product("MacBook", new BigDecimal("10000.00"), 10);
        OrderItem orderItem = new OrderItem(product, 1, product.getPrice());

        // when
        BigDecimal totalDiscount = resolver.applyDiscountRules(customer, Collections.singletonList(orderItem));

        // then
        assertThat(totalDiscount).isEqualByComparingTo("1000.00");
        assertThat(orderItem.getAppliedDiscounts()).hasSize(1);
        assertThat(orderItem.getAppliedDiscounts().get(0).getPolicyName()).contains("Fix Discount");
    }
}
