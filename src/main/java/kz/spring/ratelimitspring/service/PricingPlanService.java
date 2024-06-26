package kz.spring.ratelimitspring.service;

import io.github.bucket4j.Bucket;
import kz.spring.ratelimitspring.res.PricingPlan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PricingPlanService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String apiKey) {
        return cache.computeIfAbsent(apiKey, this::newBucket);
    }

    private Bucket newBucket(String apiKey) {
        PricingPlan pricingPlan = PricingPlan.resolvePlanFromApiKey(apiKey);
        return Bucket.builder()
                .addLimit(pricingPlan.getLimit())
                .build();
    }
}
