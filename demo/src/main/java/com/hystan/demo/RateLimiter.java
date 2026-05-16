package com.hystan.demo;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, long[]> cache = new ConcurrentHashMap<>();

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
}