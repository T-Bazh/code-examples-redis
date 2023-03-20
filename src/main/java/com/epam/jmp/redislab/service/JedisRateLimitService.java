package com.epam.jmp.redislab.service;

import com.epam.jmp.redislab.api.RateLimitThreshold;
import com.epam.jmp.redislab.api.RequestDescriptor;
import com.epam.jmp.redislab.configuration.ratelimit.RateLimitRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class JedisRateLimitService implements RateLimitService {

    private final Set<RateLimitRule> rateLimitRules;

    @Autowired
    private JedisCluster jedis;

    public JedisRateLimitService(Set<RateLimitRule> rateLimitRules) {
        this.rateLimitRules = rateLimitRules;
    }

    public boolean shouldLimit(Set<RequestDescriptor> requestDescriptors) {
        boolean allowed = true;
        for (RequestDescriptor descriptor : requestDescriptors) {
            RateLimitRule ruleToApply = foundRLRuleApply(descriptor);
            if (Objects.nonNull(ruleToApply)) {
                RateLimitThreshold rlThreshold =
                        new RateLimitThreshold(ruleToApply.getAllowedNumberOfRequests(), ruleToApply.getTimeIntervalInSecs());
                rlThreshold.setKey(descriptor.getAccountId().orElse(""),
                        descriptor.getClientIp().orElse(""),
                        descriptor.getRequestType().orElse(""));
                if (jedis.exists(rlThreshold.getKey())) {
                    long newValue = jedis.decr(rlThreshold.getKey());
                    allowed = newValue > 0;
                } else {
                    jedis.set(rlThreshold.getKey(), rlThreshold.getThreshold().toString());
                    jedis.expire(rlThreshold.getKey(), rlThreshold.getDuration());
                }
            }
        }
        return !allowed;
    }

    private boolean genericRuleShouldBeApplied(RequestDescriptor descriptor, RateLimitRule rule) {
        return rule.getAccountId().isPresent() && rule.getAccountId().get().equals("") && !rule.getRequestType().isPresent();
    }

    private boolean specificRuleShouldBeApplied(RequestDescriptor descriptor, RateLimitRule rule) {
        return rule.getClientIp().isPresent() && rule.getClientIp().equals(descriptor.getClientIp()) ||
                rule.getAccountId().isPresent() && rule.getAccountId().equals(descriptor.getAccountId()) ||
                rule.getRequestType().isPresent() && rule.getRequestType().equals(descriptor.getRequestType());
    }

    private RateLimitRule foundRLRuleApply(RequestDescriptor descriptor) {
        RateLimitRule genericRule = null;
        for (RateLimitRule rule : rateLimitRules) {
            if (specificRuleShouldBeApplied(descriptor, rule)) {
                return rule;
            }
            if (genericRuleShouldBeApplied(descriptor, rule)) {
                genericRule = rule;
            }
        }
        return genericRule;
    }
}
