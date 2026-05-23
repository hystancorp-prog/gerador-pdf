package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {

    private static final long CLEANUP_THRESHOLD_MS = 5 * 60_000L;

    private final ConcurrentHashMap<String, long[]> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limiter-cleanup");
        t.setDaemon(true);
        return t;
    });

    public RateLimiter() {
        scheduler.scheduleAtFixedRate(this::evictStaleEntries, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Verifica se a chave está dentro do limite.
     * @param key       identificador único (ex: IP + ":checkout")
     * @param maxRequests número máximo de requisições permitidas na janela
     * @param windowMs  tamanho da janela em milissegundos
     * @return true se permitido, false se limite atingido
     */
    public boolean isAllowed(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();

        cache.compute(key, (k, val) -> {
            if (val == null || now - val[1] > windowMs) {
                // Nova janela — reseta contador
                return new long[]{1, now};
            }
            val[0]++;
            return val;
        });

        long[] entry = cache.get(key);
        return entry[0] <= maxRequests;
    }

    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take the RIGHTMOST IP — that is the one appended by the trusted
            // upstream proxy (Railway). The leftmost IPs are client-supplied and
            // can be forged to trivially bypass rate limiting.
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private void evictStaleEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> now - e.getValue()[1] > CLEANUP_THRESHOLD_MS);
    }
}
