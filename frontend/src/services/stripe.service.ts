import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { loadStripe, Stripe, StripeElements, PaymentIntent } from '@stripe/stripe-js';

@Injectable({
  providedIn: 'root'
})
export class StripeService {
  private apiUrl = 'http://localhost:8080/api/stripe';
  private stripe: Stripe | null = null;
  private stripePublicKey: string = '';

  constructor(private http: HttpClient) {
    console.log('🔧 StripeService initialisé');
  }

  /**
   * Initialiser Stripe avec la clé publique
   */
  async initializeStripe(): Promise<void> {
    // Si Stripe est déjà initialisé, ne rien faire
    if (this.stripe) {
      console.log('✅ Stripe déjà initialisé');
      return;
    }

    try {
      console.log('🔄 Récupération de la clé publique Stripe...');

      // ✅ FIX: Utiliser firstValueFrom au lieu de toPromise()
      const response: any = await firstValueFrom(
        this.http.get(`${this.apiUrl}/public-key`)
      );

      console.log('📦 Réponse clé publique:', response);

      // La clé peut être dans response.publicKey ou response.data.publicKey
      this.stripePublicKey = response.publicKey || response.data?.publicKey;

      if (!this.stripePublicKey) {
        throw new Error('Clé publique Stripe non trouvée dans la réponse');
      }

      console.log('🔑 Clé publique Stripe:', this.stripePublicKey.substring(0, 20) + '...');
      console.log('🚀 Chargement de Stripe.js...');

      // Charger Stripe
      this.stripe = await loadStripe(this.stripePublicKey);

      if (!this.stripe) {
        throw new Error('Impossible de charger Stripe');
      }

      console.log('✅ Stripe initialisé avec succès!');
    } catch (error) {
      console.error('❌ Erreur initialisation Stripe:', error);
      throw error;
    }
  }

  /**
   * Créer un Payment Intent
   */
  createPaymentIntent(amount: number, currency: string = 'mad'): Observable<any> {
    console.log(`💳 Création Payment Intent: ${amount} ${currency}`);
    return this.http.post(`${this.apiUrl}/create-payment-intent`, {
      amount,
      currency
    });
  }

  /**
   * Confirmer le paiement côté serveur
   */
  confirmPayment(paymentIntentId: string, amount: number): Observable<any> {
    console.log(`✅ Confirmation paiement: ${paymentIntentId}, ${amount}`);
    return this.http.post(`${this.apiUrl}/confirm-payment`, {
      paymentIntentId,
      amount
    });
  }

  /**
   * Créer les éléments de paiement Stripe
   */
  createElements(clientSecret: string): StripeElements | null {
    console.log('🎨 Création des éléments Stripe...');

    if (!this.stripe) {
      console.error('❌ Stripe n\'est pas initialisé');
      return null;
    }

    try {
      const elements = this.stripe.elements({
        clientSecret,
        appearance: {
          theme: 'stripe',
          variables: {
            colorPrimary: '#667eea',
            colorBackground: '#ffffff',
            colorText: '#1a1a1a',
            colorDanger: '#df1b41',
            fontFamily: 'system-ui, sans-serif',
            spacingUnit: '4px',
            borderRadius: '8px'
          }
        }
      });

      console.log('✅ Éléments Stripe créés');
      return elements;
    } catch (error) {
      console.error('❌ Erreur création éléments:', error);
      return null;
    }
  }

  /**
   * Confirmer le paiement avec Stripe
   */
  async confirmCardPayment(
    clientSecret: string,
    elements: StripeElements
  ): Promise<PaymentIntent | null> {
    console.log('💳 Confirmation du paiement...');

    if (!this.stripe) {
      throw new Error('Stripe non initialisé');
    }

    if (!elements) {
      throw new Error('Éléments Stripe non fournis');
    }

    try {
      const { error, paymentIntent } = await this.stripe.confirmPayment({
        elements: elements,
        confirmParams: {
          return_url: window.location.origin + '/wallet',
        },
        redirect: 'if_required'
      });

      if (error) {
        console.error('❌ Erreur Stripe:', error);
        throw new Error(error.message || 'Erreur lors du paiement');
      }

      console.log('✅ Paiement confirmé:', paymentIntent);
      return paymentIntent || null;

    } catch (error: any) {
      console.error('❌ Erreur confirmation paiement:', error);
      throw error;
    }
  }

  /**
   * Obtenir l'instance Stripe
   */
  getStripe(): Stripe | null {
    return this.stripe;
  }

  /**
   * Vérifier si Stripe est initialisé
   */
  isStripeReady(): boolean {
    return this.stripe !== null;
  }
}
