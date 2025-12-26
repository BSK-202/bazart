package com.marketplace.wallet.controller;

import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import com.marketplace.wallet.dto.PaymentIntentRequest;
import com.marketplace.wallet.dto.PaymentIntentResponse;
import com.marketplace.wallet.dto.ConfirmPaymentRequest;
import com.marketplace.wallet.dto.WalletResponse;
import com.marketplace.wallet.entity.Wallet;
import com.marketplace.wallet.service.Walletservice;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "http://localhost:4200")
public class StripePaymentController {

    private final Walletservice walletService;
    private final ClientService clientService;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public StripePaymentController(Walletservice walletService, ClientService clientService) {
        this.walletService = walletService;
        this.clientService = clientService;
        log.info("🎯 StripePaymentController initialisé");
    }

    /**
     * Récupérer la clé publique Stripe
     */
    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        log.info("📢 GET /api/stripe/public-key appelé");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("publicKey", stripePublicKey);

        return ResponseEntity.ok(response);
    }

    /**
     * Vérifier la configuration Stripe
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        log.info("📢 GET /api/stripe/config appelé");

        Map<String, Object> config = new HashMap<>();
        config.put("stripeConfigured", stripeSecretKey != null && !stripeSecretKey.isEmpty());
        config.put("publicKey", stripePublicKey);
        config.put("message", stripeSecretKey != null && !stripeSecretKey.isEmpty()
                ? "Stripe est configuré ✅"
                : "Stripe n'est PAS configuré ❌");

        return ResponseEntity.ok(config);
    }

    /**
     * Créer un Payment Intent
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(
            @RequestBody PaymentIntentRequest request,
            Authentication authentication) {

        log.info("🎯 POST /api/stripe/create-payment-intent appelé");
        log.info("📦 Requête reçue: amount={}", request.getAmount());
        log.info("👤 Utilisateur authentifié: {}", authentication != null ? authentication.getName() : "AUCUN");

        try {
            // Vérification de l'authentification
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("❌ Utilisateur non authentifié");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(PaymentIntentResponse.error("Utilisateur non authentifié"));
            }

            // Vérification de la clé Stripe
            if (stripeSecretKey == null || stripeSecretKey.isEmpty()) {
                log.error("❌ Clé Stripe non configurée");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(PaymentIntentResponse.error("Configuration Stripe manquante"));
            }

            // Récupérer l'utilisateur connecté
            String email = authentication.getName();
            log.info("🔍 Recherche du client avec email: {}", email);

            Client user = clientService.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("❌ Utilisateur non trouvé: {}", email);
                        return new RuntimeException("Utilisateur non trouvé");
                    });

            log.info("✅ Client trouvé: ID={}, Email={}", user.getIdclient(), user.getEmail());

            // Valider le montant
            double amount = request.getAmount();
            log.info("💰 Montant demandé: {}€", amount);

            if (amount <= 0) {
                log.error("❌ Montant invalide: {}", amount);
                return ResponseEntity.badRequest()
                        .body(PaymentIntentResponse.error("Le montant doit être positif"));
            }

            if (amount < 0.50) {
                log.error("❌ Montant trop petit: {}€ (minimum 0.50€)", amount);
                return ResponseEntity.badRequest()
                        .body(PaymentIntentResponse.error("Le montant minimum est de 0.50€"));
            }

            // Créer le Payment Intent avec Stripe
            log.info("🔄 Création du Payment Intent avec Stripe...");
            PaymentIntent paymentIntent = walletService.createStripePaymentIntent(user, amount);

            log.info("✅ Payment Intent créé avec succès!");
            log.info("   - Payment Intent ID: {}", paymentIntent.getId());
            log.info("   - Montant: {} centimes ({}€)", paymentIntent.getAmount(), paymentIntent.getAmount() / 100.0);
            log.info("   - Devise: {}", paymentIntent.getCurrency());
            log.info("   - Statut: {}", paymentIntent.getStatus());

            // Retourner le client secret
            PaymentIntentResponse response = PaymentIntentResponse.success(
                    paymentIntent.getClientSecret(),
                    paymentIntent.getId()
            );

            log.info("📤 Réponse envoyée au client avec clientSecret");
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("❌ Erreur Stripe: Code={}, Message={}", e.getCode(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentIntentResponse.error("Erreur Stripe: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la création du Payment Intent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentIntentResponse.error("Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * Confirmer le paiement et recharger le wallet
     */
    @PostMapping("/confirm-payment")
    public ResponseEntity<?> confirmPayment(
            @RequestBody ConfirmPaymentRequest request,
            Authentication authentication) {

        log.info("🎯 POST /api/stripe/confirm-payment appelé");
        log.info("📦 Payment Intent ID: {}, Montant: {}€", request.getPaymentIntentId(), request.getAmount());

        try {
            // Vérification de l'authentification
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("❌ Utilisateur non authentifié");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(WalletResponse.error("Utilisateur non authentifié"));
            }

            // Récupérer l'utilisateur connecté
            String email = authentication.getName();
            Client user = clientService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            log.info("✅ Client trouvé: ID={}, Email={}", user.getIdclient(), user.getEmail());

            // Confirmer le paiement et recharger le wallet
            log.info("🔄 Confirmation du paiement et recharge du wallet...");
            Wallet updatedWallet = walletService.confirmStripePaymentAndRecharge(
                    user,
                    request.getPaymentIntentId(),
                    request.getAmount()
            );

            log.info("✅ Wallet rechargé avec succès!");
            log.info("   - Nouveau solde: {}€", updatedWallet.getBalance());
            log.info("   - Montant ajouté: {}€", request.getAmount());

            return ResponseEntity.ok(WalletResponse.success("Recharge effectuée avec succès")
                    .addData("newBalance", updatedWallet.getBalance())
                    .addData("amount", request.getAmount()));

        } catch (StripeException e) {
            log.error("❌ Erreur Stripe lors de la confirmation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WalletResponse.error("Paiement non validé: " + e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la confirmation du paiement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WalletResponse.error("Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * Webhook Stripe (pour les notifications de paiement)
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("🔔 Webhook Stripe reçu");
        log.info("   - Signature: {}", sigHeader);
        log.info("   - Payload length: {} bytes", payload.length());

        // TODO: Implémenter la vérification du webhook
        return ResponseEntity.ok("Webhook reçu");
    }
}