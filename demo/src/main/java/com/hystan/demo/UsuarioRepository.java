ackage com.hystan.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByFirebaseUid(String firebaseUid);
    Optional<Usuario> findByStripeCustomerId(String stripeCustomerId);
    Optional<Usuario> findByStripeSubscriptionId(String stripeSubscriptionId);
    List<Usuario> findByPlanoStatusAndTrialFimBefore(String planoStatus, LocalDateTime data);
}
