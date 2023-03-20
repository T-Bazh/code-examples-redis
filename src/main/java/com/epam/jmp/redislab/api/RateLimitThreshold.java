package com.epam.jmp.redislab.api;

public class RateLimitThreshold {
    // time unit will be in seconds
    //timeInterval
    private long duration;

    //allowedNumberOfRequests
    private Integer threshold;

    private String key;


    public RateLimitThreshold(Integer threshold, long duration) {
        this.duration = duration;
        this.threshold = threshold;
    }

    public void setKey(String accountId, String clientIp, String requestType) {
        if (key == null) {
            key = getClusterKey(accountId,clientIp,requestType);
        }
    }
    public long getDuration() {
        return duration;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public String getKey() {
        return key;
    }

   private String getClusterKey(String accountId, String clientIp, String requestType) {
        return "{" + accountId + ":" + clientIp + ":" + requestType + "}";
    }
}
