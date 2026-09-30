package com.growthpilot.razorpay.client;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RazorpayClientFactory {

    private final Map<String, RazorpayClient> clients = new ConcurrentHashMap<>();

    public RazorpayClient getClient(String keyId, String keySecret) throws RazorpayException {
        String cacheKey = keyId + ":" + keySecret;
        if (!clients.containsKey(cacheKey)) {
            clients.put(cacheKey, new RazorpayClient(keyId, keySecret));
        }
        return clients.get(cacheKey);
    }
}
