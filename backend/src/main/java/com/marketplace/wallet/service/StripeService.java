package com.marketplace.wallet.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StripeService {

    /**
     * Créer un Payment Intent pour initier un paiement
     *
     * IMPORTANT: On utilise SOIT automatic_payment_methods SOIT payment_method_types
     * Jamais les deux en même temps !
     */
    public PaymentIntent createPaymentIntent(double amount, String currency) throws StripeException {
        log.info("🔄 StripeService.createPaymentIntent appelé");
        log.info("   - Montant: {}€", amount);
        log.info("   - Devise: {}", currency);

        // Convertir le montant en centimes (Stripe utilise les plus petites unités)
        long amountInCents = (long) (amount * 100);
        log.info("   - Montant en centimes: {}", amountInCents);

        // ✅ SOLUTION: Utiliser UNIQUEMENT automatic_payment_methods
        // On ne spécifie PAS payment_method_types
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        log.info("🚀 Envoi de la requête à Stripe...");
        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("✅ Payment Intent créé avec succès par Stripe");
        log.info("   - ID: {}", paymentIntent.getId());
        log.info("   - Statut: {}", paymentIntent.getStatus());
        log.info("   - Montant: {} centimes", paymentIntent.getAmount());

        return paymentIntent;
    }

    /**
     * Récupérer un Payment Intent par son ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        log.info("🔍 Récupération du Payment Intent: {}", paymentIntentId);
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        log.info("✅ Payment Intent récupéré - Statut: {}", paymentIntent.getStatus());
        return paymentIntent;
    }

    /**
     * Vérifier si un paiement est réussi
     */
    public boolean isPaymentSucceeded(String paymentIntentId) throws StripeException {
        log.info("🔍 Vérification du statut du paiement: {}", paymentIntentId);
        PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId);
        boolean succeeded = "succeeded".equals(paymentIntent.getStatus());

        if (succeeded) {
            log.info("✅ Paiement réussi !");
        } else {
            log.warn("⚠️ Paiement non réussi - Statut: {}", paymentIntent.getStatus());
        }

        return succeeded;
    }

    /**
     * Annuler un Payment Intent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("❌ Annulation du Payment Intent: {}", paymentIntentId);
        PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId);
        PaymentIntent cancelled = paymentIntent.cancel();
        log.info("✅ Payment Intent annulé");
        return cancelled;
    }
}