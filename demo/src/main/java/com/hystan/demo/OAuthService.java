package com.hystan.demo;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuthService extends DefaultOAuth2UserService {

    private final UsuarioRepository repo;

    public OAuthService(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        try {
            System.out.println("=== OAUTH INICIADO ===");
            OAuth2User user = super.loadUser(request);
            System.out.println("=== OAUTH OK: " + user.getAttribute("email"));
            
            String email = user.getAttribute("email");
            String nome  = user.getAttribute("name");
            String foto  = user.getAttribute("picture");

            repo.findByEmail(email).orElseGet(() -> {
                Usuario novo = new Usuario(email, nome, foto);
                return repo.save(novo);
            });

            System.out.println("=== USUARIO SALVO ===");
            return user;

        } catch (Exception e) {
            System.out.println("=== ERRO OAUTH: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}