package com.hystan.demo;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        // Valida assinatura — sem isso qualquer um pode forjar eventos
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook com assinatura inválida rejeitado");
            return ResponseEntity.status(400).body("Assinatura inválida");
        }

        log.info("Webhook recebido: {}", event.getType());

        try {
            switch (event.getType()) {

                case "checkout.session.completed" -> {
                    var deserializer = event.getDataObjectDeserializer();
                    if (deserializer.getObject().isEmpty()) {
                        log.warn("Evento {} sem objeto deserializável, ignorando", event.getType());
                        break;
                    }
                    Session session = (Session) deserializer.getObject().get();
                    handleCheckoutCompleted(session);
                }

                case "customer.subscription.updated" -> {
                    var deserializer = event.getDataObjectDeserializer();
                    if (deserializer.getObject().isEmpty()) {
                        log.warn("Evento {} sem objeto deserializável, ignorando", event.getType());
                        break;
                    }
                    Subscription sub = (Subscription) deserializer.getObject().get();
                    handleSubscriptionUpdated(sub);
                }

                case "customer.subscription.deleted" -> {
                    var deserializer = event.getDataObjectDeserializer();
                    if (deserializer.getObject().isEmpty()) {
                        log.warn("Evento {} sem objeto deserializável, ignorando", event.getType());
                        break;
                    }
                    Subscription sub = (Subscription) deserializer.getObject().get();
                    handleSubscriptionDeleted(sub);
                }

                case "invoice.payment_failed" -> {
                    var deserializer = event.getDataObjectDeserializer();
                    if (deserializer.getObject().isEmpty()) {
                        log.warn("Evento {} sem objeto deserializável, ignorando", event.getType());
                        break;
                    }
                    Invoice invoice = (Invoice) deserializer.getObject().get();
                    handlePaymentFailed(invoice);
                }

                default -> log.info("Evento ignorado: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erro ao processar webhook {}: ", event.getType(), e);
            // Retorna 200 mesmo com erro interno para o Stripe não reenviar infinitamente
            // Erros de processamento devem ser tratados internamente
        }

        return ResponseEntity.ok("ok");
    }

    // Checkout concluído — assinatura criada ou trial iniciado
    private void handleCheckoutCompleted(Session session) {
        String customerEmail = session.getCustomerEmail();
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        if (customerEmail == null && customerId != null) {
            try {
                Customer customer = Customer.retrieve(customerId);
                customerEmail = customer.getEmail();
            } catch (Exception e) {
                log.error("Erro ao buscar customer {}: ", customerId, e);
                return;
            }
        }

        Optional<Usuario> opt = usuarioRepository.findByEmail(customerEmail);
        if (opt.isEmpty()) {
            log.warn("Checkout concluído mas usuário não encontrado: {}", customerEmail);
            return;
        }

        Usuario usuario = opt.get();
        usuario.setStripeCustomerId(customerId);
        usuario.setStripeSubscriptionId(subscriptionId);

        // Detecta trial consultando a subscription criada no checkout
        if (subscriptionId != null) {
            try {
                Subscription sub = Subscription.retrieve(subscriptionId);
                if (sub.getTrialEnd() != null) {
                    usuario.setPlanoStatus("trial");
                    usuario.setTrialFim(toLocalDateTime(sub.getTrialEnd()));
                } else {
                    usuario.setPlanoStatus("ativo");
                }
            } catch (Exception e) {
                log.warn("Não foi possível verificar trial: {}", e.getMessage());
                usuario.setPlanoStatus("ativo");
            }
        } else {
            usuario.setPlanoStatus("ativo");
        }

        // Detecta o plano pelo price ID da line item
        try {
            com.stripe.model.checkout.Session fullSession = com.stripe.model.checkout.Session.retrieve(
                session.getId(),
                com.stripe.param.checkout.SessionRetrieveParams.builder()
                    .addExpand("line_items")
                    .build(),
                null
            );
            if (fullSession.getLineItems() != null && !fullSession.getLineItems().getData().isEmpty()) {
                String priceId = fullSession.getLineItems().getData().get(0).getPrice().getId();
                usuario.setPlano(resolverPlano(priceId));
            }
        } catch (Exception e) {
            log.warn("Não foi possível detectar plano pelo price: {}", e.getMessage());
        }

        usuarioRepository.save(usuario);
        log.info("Assinatura ativada para: {}", customerEmail);
    }

    // Assinatura atualizada — upgrade, downgrade, trial ativado
    private void handleSubscriptionUpdated(Subscription sub) {
        Optional<Usuario> opt = usuarioRepository.findByStripeCustomerId(sub.getCustomer());
        if (opt.isEmpty()) {
            log.warn("Subscription updated mas customer não encontrado: {}", sub.getCustomer());
            return;
        }

        Usuario usuario = opt.orElseThrow();

        // Atualiza status
        switch (sub.getStatus()) {
            case "active" -> {
                usuario.setPlanoStatus("ativo");
                if (sub.getCurrentPeriodEnd() != null) {
                    usuario.setAssinaturaFim(toLocalDateTime(sub.getCurrentPeriodEnd()));
                }
            }
            case "trialing" -> {
                usuario.setPlanoStatus("trial");
                if (sub.getTrialEnd() != null) {
                    usuario.setTrialFim(toLocalDateTime(sub.getTrialEnd()));
                }
            }
            case "past_due" -> usuario.setPlanoStatus("inadimplente");
            case "canceled" -> {
                usuario.setPlanoStatus("cancelado");
                usuario.setPlano("gratuito");
            }
        }

        // Atualiza plano se mudou
        if (!sub.getItems().getData().isEmpty()) {
            String priceId = sub.getItems().getData().get(0).getPrice().getId();
            usuario.setPlano(resolverPlano(priceId));
        }

        usuarioRepository.save(usuario);
        log.info("Assinatura atualizada para {}: status={}, plano={}",
            usuario.getEmail(), usuario.getPlanoStatus(), usuario.getPlano());
    }

    // Assinatura cancelada — revoga acesso
    private void handleSubscriptionDeleted(Subscription sub) {
        Optional<Usuario> opt = usuarioRepository.findByStripeCustomerId(sub.getCustomer());
        if (opt.isEmpty()) {
            log.warn("Subscription deleted mas customer não encontrado: {}", sub.getCustomer());
            return;
        }

        Usuario usuario = opt.get();
        usuario.setPlano("gratuito");
        usuario.setPlanoStatus("cancelado");
        usuario.setStripeSubscriptionId(null);

        usuarioRepository.save(usuario);
        log.info("Assinatura cancelada para: {}", usuario.getEmail());
    }

    // Pagamento falhou — marca como inadimplente
    private void handlePaymentFailed(Invoice invoice) {
        Optional<Usuario> opt = usuarioRepository.findByStripeCustomerId(invoice.getCustomer());
        if (opt.isEmpty()) {
            log.warn("Payment failed mas customer não encontrado: {}", invoice.getCustomer());
            return;
        }

        Usuario usuario = opt.get();
        usuario.setPlanoStatus("inadimplente");

        usuarioRepository.save(usuario);
        log.warn("Pagamento falhou para: {}", usuario.getEmail());
    }

    // Converte timestamp Unix para LocalDateTime
    private LocalDateTime toLocalDateTime(Long timestamp) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp),
            ZoneId.systemDefault()
        );
    }

    // Mapeia price ID para nome do plano interno
    private String resolverPlano(String priceId) {
        // Injetando os valores via System.getenv para não precisar de @Value em método estático
        String priceBasico = System.getenv("STRIPE_PRICE_BASICO");
        String pricePro = System.getenv("STRIPE_PRICE_PRO");
        String priceBasicoAnual = System.getenv("STRIPE_PRICE_BASICO_ANUAL");
        String priceProAnual = System.getenv("STRIPE_PRICE_PRO_ANUAL");

        if (priceId.equals(priceBasico) || priceId.equals(priceBasicoAnual)) return "basico";
        if (priceId.equals(pricePro) || priceId.equals(priceProAnual)) return "pro";
        return "gratuito";
    }
}