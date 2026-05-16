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

    // Planos mensais
    @Value("${STRIPE_PRICE_BASICO}")
    private String priceEssencial;

    @Value("${STRIPE_PRICE_PRO}")
    private String priceBusiness;

    // Planos anuais — adicione estas variáveis no Railway também
    @Value("${STRIPE_PRICE_BASICO_ANUAL:#{null}}")
    private String priceEssencialAnual;

    @Value("${STRIPE_PRICE_PRO_ANUAL:#{null}}")
    private String priceBusinessAnual;

    @PostMapping("/criar-checkout")
    public ResponseEntity<Map<String, String>> criarCheckout(
            @RequestParam("plano") String plano,
            HttpServletRequest request) {

        // Rate limiting
        if (!rateLimiter.isAllowed(request.getRemoteAddr() + ":checkout", RATE_LIMIT, WINDOW_MS)) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Muitas requisições. Tente novamente em instantes.");
            return ResponseEntity.status(429).body(err);
        }

        try {
            Stripe.apiKey = stripeSecretKey.trim().replaceAll("[\\r\\n\\t]", "");

            // Mapeia nome do plano para price ID
            String priceId = resolverPriceId(plano.trim().toLowerCase());

            if (priceId == null || priceId.isBlank()) {
                Map<String, String> err = new HashMap<>();
                err.put("error", "Plano inválido ou não configurado: " + plano);
                return ResponseEntity.status(400).body(err);
            }

            // Trial de 7 dias só nos planos mensais
            boolean temTrial = !plano.contains("anual");

            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://gerador-pdf-production-76f7.up.railway.app/dashboard.html?pago=true")
                .setCancelUrl("https://gerador-pdf-production-76f7.up.railway.app/planos.html")
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                );

            // Adiciona trial de 7 dias se for plano mensal
            if (temTrial) {
                builder.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                        .setTrialPeriodDays(7L)
                        .build()
                );
            }

            SessionCreateParams params = builder.build();
            Session sessionObj = Session.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("url", sessionObj.getUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Erro ao criar sessão de pagamento. Tente novamente.");
            return ResponseEntity.status(500).body(err);
        }
    }

    private String resolverPriceId(String plano) {
        return switch (plano) {
            case "essencial"       -> priceEssencial;
            case "pro"             -> priceBusiness;
            case "essencial_anual" -> priceEssencialAnual;
            case "pro_anual"       -> priceBusinessAnual;
            // compatibilidade com nomes antigos
            case "basico"          -> priceEssencial;
            case "basico_anual"    -> priceEssencialAnual;
            default                -> null;
        };
    }
}