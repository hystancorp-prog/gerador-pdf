package com.hystan.demo;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
public class StripeController {

    @Value("${STRIPE_SECRET_KEY}")
    private String stripeSecretKey;

    @Value("${STRIPE_PRICE_BASICO}")
    private String priceBasico;

    @Value("${STRIPE_PRICE_PRO}")
    private String pricePro;

    @PostMapping("/criar-checkout")
    public ResponseEntity<Map<String, String>> criarCheckout(
            @RequestParam("plano") String plano) {
        try {
            Stripe.apiKey = stripeSecretKey;

            String priceId = plano.equals("pro") ? pricePro : priceBasico;

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://gerador-pdf-production-76f7.up.railway.app/dashboard.html?sucesso=true")
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
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}