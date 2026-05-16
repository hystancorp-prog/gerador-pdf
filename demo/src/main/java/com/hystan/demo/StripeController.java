package com.hystan.demo;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
public class StripeController {

    private static final int  RATE_LIMIT = 5;
    private static final long WINDOW_MS  = 60_000L;

    @Autowired
    private RateLimiter rateLimiter;

    @Value("${STRIPE_SECRET_KEY}")
    private String stripeSecretKey;

    @Value("${STRIPE_PRICE_BASICO}")
    private String priceBasico;

    @Value("${STRIPE_PRICE_PRO}")
    private String pricePro;

    @PostMapping("/criar-checkout")
    public ResponseEntity<Map<String, String>> criarCheckout(
            @RequestParam("plano") String plano,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(request.getRemoteAddr() + ":checkout", RATE_LIMIT, WINDOW_MS)) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Muitas requisições. Tente novamente em instantes.");
            return ResponseEntity.status(429).body(err);
        }
        try {
            String key = stripeSecretKey.trim().replaceAll("[\\r\\n\\t]", "");
            Stripe.apiKey = key;

            String priceId = plano.equals("pro") ? pricePro.trim() : priceBasico.trim();

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://gerador-pdf-production-76f7.up.railway.app/dashboard.html?pago=true")
                .setCancelUrl("https://gerador-pdf-production-76f7.up.railway.app/index.html")
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                )
                .build();

            Session session = Session.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("url", session.getUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Erro ao criar sessão de pagamento. Tente novamente.");
            return ResponseEntity.status(500).body(err);
        }
    }
}