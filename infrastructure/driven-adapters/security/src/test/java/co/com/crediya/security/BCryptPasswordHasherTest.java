package co.com.crediya.security;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void cifraLaClaveSinGuardarlaEnTextoPlano() {
        String hash = hasher.hash("ClaveSegura1").block();

        assertThat(hash).isNotBlank().isNotEqualTo("ClaveSegura1").startsWith("$2a$");
    }

    @Test
    void verificaLaClaveContraElHash() {
        String hash = hasher.hash("ClaveSegura1").block();

        StepVerifier.create(hasher.matches("ClaveSegura1", hash)).expectNext(true).verifyComplete();
        StepVerifier.create(hasher.matches("OtraClave99", hash)).expectNext(false).verifyComplete();
    }
}
