package com.shopping.engine.controller;

import com.shopping.engine.controller.dto.PolicyUpdateRequestDto;
import com.shopping.engine.policy.DiscountPolicySettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PolicyController {

    private final DiscountPolicySettings policySettings;

    public PolicyController(DiscountPolicySettings policySettings) {
        this.policySettings = policySettings;
    }

    @GetMapping
    public ResponseEntity<List<DiscountPolicySettings.PolicySetting>> getPolicies() {
        return ResponseEntity.ok(policySettings.findAll());
    }

    @PutMapping("/{type}")
    public ResponseEntity<DiscountPolicySettings.PolicySetting> updatePolicy(
            @PathVariable String type,
            @RequestBody PolicyUpdateRequestDto request
    ) {
        return ResponseEntity.ok(policySettings.updateExtended(
                type,
                request.enabled(),
                request.priority(),
                request.exclusive(),
                request.discountRate(),
                request.discountAmount(),
                request.basicDiscountRate(),
                request.vipDiscountRate(),
                request.vvipDiscountRate()
        ));
    }
}
