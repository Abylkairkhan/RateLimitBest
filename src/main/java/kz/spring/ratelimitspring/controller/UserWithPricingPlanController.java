package kz.spring.ratelimitspring.controller;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import kz.spring.ratelimitspring.model.User;
import kz.spring.ratelimitspring.repository.UserRepository;
import kz.spring.ratelimitspring.service.PricingPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api2/users")
public class UserWithPricingPlanController {

    @Autowired
    private UserRepository userRepository;

    private final PricingPlanService pricingPlanService = new PricingPlanService();

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestHeader(value = "X-api-key") String apiKey
            ) {
        System.out.println(apiKey);
        Bucket bucket = pricingPlanService.resolveBucket(apiKey);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok()
                    .header("X-Rate-Limit-Remaining", Long.toString(probe.getRemainingTokens()))
                    .body(users);
        }
        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill))
                .build();
    }
}
