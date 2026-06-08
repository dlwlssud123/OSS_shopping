package com.shopping.engine.repository;

import com.shopping.engine.domain.DiscountPolicySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountPolicySettingRepository extends JpaRepository<DiscountPolicySetting, String> {
}
