package com.marketplace.wallet.config;

import com.stripe.Stripe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        log.info("🔧 Configuration de Stripe...");

        // 1. Configurer la clé API
        Stripe.apiKey = secretKey;

        // 2. ✅ AUGMENTER LES TIMEOUTS (c'est ça qui manquait!)
        Stripe.setConnectTimeout(30000);  // 30 secondes au lieu de 10
        Stripe.setReadTimeout(80000);     // 80 secondes au lieu de 30
        Stripe.setMaxNetworkRetries(3);   // 3 essais au lieu de 0

        // 3. ✅ FORCER TLS 1.2/1.3 (important!)
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");

        log.info("✅ Stripe configuré avec timeouts étendus");
        log.info("   - Connect timeout: 30s");
        log.info("   - Read timeout: 80s");
        log.info("   - Max retries: 3");
    }
}