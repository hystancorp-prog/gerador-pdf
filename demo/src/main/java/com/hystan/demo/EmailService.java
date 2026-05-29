package com.hystan.demo;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    public void enviarBoasVindas(String email, String nome) {
        try {
            Resend resend = new Resend(resendApiKey);

            String nomeExibir = (nome != null && !nome.isBlank()) ? nome : "por aí";

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: 'DM Sans', Arial, sans-serif; background: #f5f5f3; margin: 0; padding: 40px 20px;">
                  <div style="max-width: 560px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; border: 1px solid #e0e0dd;">
                    <div style="background: #1a1a1a; padding: 32px 40px;">
                      <h1 style="color: white; font-size: 22px; margin: 0; font-weight: 800;">HYSTAN</h1>
                    </div>
                    <div style="padding: 40px;">
                      <h2 style="font-size: 22px; color: #1a1a1a; margin: 0 0 16px;">Bem-vindo, %s!</h2>
                      <p style="color: #666; font-size: 15px; line-height: 1.7; margin: 0 0 24px;">
                        Sua conta está pronta. Agora você pode transformar planilhas Excel em
                        relatórios PDF profissionais, gerar orçamentos e emitir recibos em segundos.
                      </p>
                      <a href="https://www.hystan.com.br/dashboard.html"
                         style="display: inline-block; background: #1a1a1a; color: white;
                                padding: 14px 28px; border-radius: 10px; text-decoration: none;
                                font-weight: 600; font-size: 15px;">
                        Acessar o dashboard →
                      </a>
                      <p style="color: #aaa; font-size: 13px; margin: 32px 0 0; line-height: 1.6;">
                        Qualquer dúvida é só responder este email.<br>
                        Equipe Hystan · <a href="https://www.hystan.com.br" style="color: #1a1a1a;">www.hystan.com.br</a>
                      </p>
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(nomeExibir);

            CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                .from("Hystan <onboarding@resend.dev>")
                .to(email)
                .subject("Bem-vindo à Hystan!")
                .html(html)
                .build();

            resend.emails().send(sendEmailRequest);

        } catch (Exception e) {
            System.out.println("Erro ao enviar email: " + e.getMessage());
        }
    }

    public void enviarTrialExpirado(String email, String nome, int docsUsados) {
        try {
            Resend resend = new Resend(resendApiKey);

            String nomeExibir = (nome != null && !nome.isBlank()) ? nome : "você";
            String plural = docsUsados == 1 ? "" : "s";

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: DM Sans, Arial, sans-serif; background: #f5f5f3; margin: 0; padding: 40px 20px;">
                  <div style="max-width: 560px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; border: 1px solid #e0e0dd;">
                    <div style="background: #1a1a1a; padding: 32px 40px;">
                      <h1 style="color: white; font-size: 22px; margin: 0; font-weight: 800;">HYSTAN</h1>
                    </div>
                    <div style="padding: 40px;">
                      <h2 style="font-size: 22px; color: #1a1a1a; margin: 0 0 16px;">Seu trial da Hystan expirou</h2>
                      <p style="color: #666; font-size: 15px; line-height: 1.7; margin: 0 0 8px;">
                        Olá, %s!
                      </p>
                      <p style="color: #666; font-size: 15px; line-height: 1.7; margin: 0 0 24px;">
                        Você usou <strong>%d documento%s</strong> durante o trial.
                        Para continuar gerando documentos profissionais, escolha um plano.
                      </p>
                      <a href="https://www.hystan.com.br/planos.html"
                         style="display: inline-block; background: #1a1a1a; color: white;
                                padding: 14px 28px; border-radius: 10px; text-decoration: none;
                                font-weight: 600; font-size: 15px;">
                        Assinar agora →
                      </a>
                      <p style="color: #aaa; font-size: 13px; margin: 32px 0 0; line-height: 1.6;">
                        Qualquer dúvida é só responder este email.<br>
                        Equipe Hystan · <a href="https://www.hystan.com.br" style="color: #1a1a1a;">www.hystan.com.br</a>
                      </p>
                    </div>
                  </div>
                </body>
                </html>
            """.formatted(nomeExibir, docsUsados, plural);

            CreateEmailOptions req = CreateEmailOptions.builder()
                .from("Hystan <onboarding@resend.dev>")
                .to(email)
                .subject("Seu trial da Hystan expirou")
                .html(html)
                .build();

            resend.emails().send(req);

        } catch (Exception e) {
            System.out.println("Erro ao enviar email trial expirado: " + e.getMessage());
        }
    }
}
