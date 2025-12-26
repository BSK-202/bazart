import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WalletService, WalletBalanceResponse, RechargeResponse, HistoryResponse } from '../../../services/wallet.service';
import { AuthService } from '../../../services/auth.service';
import { EnchereService } from '../../../services/enchere.service';
import { StripeService } from '../../../services/stripe.service';
import { Router } from '@angular/router';
import { StripePaymentComponent } from '../stripe-payment/stripe-payment.component';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, DatePipe, StripePaymentComponent],
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.css']
})
export class WalletComponent implements OnInit {
  balance: number = 0;
  amountToAdd: number = 0;
  message: string = '';
  currency: string = 'MAD';
  isLoading: boolean = false;
  showConfirmation: boolean = false;
  pendingAmount: number = 0;

  // Propriétés pour l'historique
  history: any[] = [];
  historyLoading: boolean = false;
  selectedOperationType: string = 'ALL';

  // Propriétés pour les montants bloqués
  blockedEncheres: any[] = [];
  totalBlocked: number = 0;
  isLoadingBlocked: boolean = false;

  // ✅ NOUVELLES PROPRIÉTÉS STRIPE
  showStripePayment: boolean = false;
  stripeClientSecret: string = '';
  stripePendingAmount: number = 0;

  constructor(
    private walletService: WalletService,
    private authService: AuthService,
    private enchereService: EnchereService,
    private stripeService: StripeService,
    private router: Router
  ) {}

  ngOnInit() {
    console.log('💰 Initialisation du composant Wallet');
    this.checkAuthentication();
    this.getBalance();
    this.loadHistory();
    this.loadBlockedAmounts();
  }

  checkAuthentication() {
    const isLoggedIn = this.authService.isLoggedIn();
    const token = this.authService.getToken();
    const user = this.authService.getUser();

    console.log('🔐 État authentification Wallet:');
    console.log('   - Connecté:', isLoggedIn);
    console.log('   - Token présent:', !!token);
    console.log('   - Utilisateur:', user);

    if (!isLoggedIn) {
      this.message = '❌ Veuillez vous connecter pour accéder à votre portefeuille';
      setTimeout(() => {
        this.authService.redirectToLogin('Veuillez vous connecter pour accéder au portefeuille');
      }, 3000);
    }
  }

  getBalance() {
    if (!this.authService.isLoggedIn()) {
      this.message = 'Non authentifié. Veuillez vous reconnecter.';
      return;
    }

    console.log('🔄 Chargement du solde...');
    this.walletService.getBalance().subscribe({
      next: (response: WalletBalanceResponse) => {
        console.log('✅ Réponse solde:', response);
        if (response.success) {
          this.balance = response.balance;
          this.message = '';
          console.log(`💰 Solde chargé: ${this.balance} MAD`);
        } else {
          this.message = 'Erreur lors du chargement du solde';
        }
      },
      error: (err: any) => {
        console.error('❌ Erreur lors du chargement du solde:', err);
        if (err.status === 401) {
          this.message = 'Session expirée. Veuillez vous reconnecter.';
          setTimeout(() => {
            this.authService.redirectToLogin('Session expirée');
          }, 3000);
        } else {
          this.message = err.error?.error || 'Impossible de charger le solde.';
        }
      }
    });
  }

  // ✅ NOUVELLE MÉTHODE : Initier le paiement Stripe
  confirmRecharge() {
    if (!this.authService.isLoggedIn()) {
      this.message = 'Veuillez vous connecter pour recharger votre portefeuille';
      return;
    }

    if (this.amountToAdd <= 0) {
      this.message = "Veuillez saisir un montant valide.";
      return;
    }

    this.isLoading = true;
    this.message = 'Initialisation du paiement...';
    console.log(`💳 Création Payment Intent: ${this.amountToAdd} MAD`);

    // Créer un Payment Intent Stripe
    this.stripeService.createPaymentIntent(this.amountToAdd).subscribe({
      next: (response: any) => {
        console.log('✅ Payment Intent créé:', response);
        if (response.success) {
          this.stripeClientSecret = response.clientSecret;
          this.stripePendingAmount = this.amountToAdd;
          this.showStripePayment = true;
          this.message = '';
        } else {
          this.message = '❌ Erreur lors de l\'initialisation du paiement';
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur création Payment Intent:', err);
        this.message = err.error?.message || 'Erreur lors de l\'initialisation du paiement';
        this.isLoading = false;
      }
    });
  }

  // ✅ NOUVELLE MÉTHODE : Gérer le succès du paiement Stripe
  onStripePaymentSuccess(event: any) {
    console.log('✅ Paiement Stripe réussi:', event);
    this.isLoading = true;
    this.message = 'Confirmation du paiement...';

    // Confirmer le paiement côté serveur et recharger le wallet
    this.stripeService.confirmPayment(event.paymentIntentId, event.amount).subscribe({
      next: (response: any) => {
        console.log('✅ Wallet rechargé:', response);
        if (response.success) {
          this.balance = response.data.newBalance;
          this.amountToAdd = 0;
          this.showStripePayment = false;
          this.message = `✅ Recharge de ${event.amount} MAD effectuée avec succès!`;
          this.loadHistory();
        } else {
          this.message = '❌ Erreur lors de la confirmation du paiement';
        }
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur confirmation paiement:', err);
        this.message = err.error?.message || 'Erreur lors de la confirmation du paiement';
        this.isLoading = false;
      }
    });
  }

  // ✅ NOUVELLE MÉTHODE : Annuler le paiement Stripe
  onStripePaymentCancel() {
    console.log('❌ Paiement Stripe annulé');
    this.showStripePayment = false;
    this.stripeClientSecret = '';
    this.stripePendingAmount = 0;
    this.message = 'Paiement annulé.';
  }

  // ✅ NOUVELLE MÉTHODE : Gérer les erreurs Stripe
  onStripePaymentError(error: string) {
    console.error('❌ Erreur paiement Stripe:', error);
    this.message = `❌ ${error}`;
  }

  cancelRecharge() {
    this.showConfirmation = false;
    this.pendingAmount = 0;
    this.message = 'Recharge annulée.';
  }

  setQuickAmount(amount: number) {
    this.amountToAdd = amount;
    this.message = '';
  }

  loadHistory() {
    if (!this.authService.isLoggedIn()) return;

    this.historyLoading = true;
    const historyCall = this.selectedOperationType === 'ALL'
      ? this.walletService.getHistory()
      : this.walletService.getHistoryByType(this.selectedOperationType);

    historyCall.subscribe({
      next: (response: HistoryResponse) => {
        if (response.success) {
          this.history = response.history;
        }
        this.historyLoading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur historique:', err);
        this.historyLoading = false;
      }
    });
  }

  onOperationTypeChange() {
    this.loadHistory();
  }

  getOperationClass(operationType: string): string {
    switch (operationType) {
      case 'CREDIT': return 'credit-operation';
      case 'DEBIT': return 'debit-operation';
      default: return 'default-operation';
    }
  }

  formatDescription(description: string): string {
    return description || 'Opération sans description';
  }

  loadBlockedAmounts() {
    if (!this.authService.isLoggedIn()) return;

    this.isLoadingBlocked = true;
    this.enchereService.getLeadingEncheres().subscribe({
      next: (response: any) => {
        if (response.success) {
          this.blockedEncheres = response.leadingEncheres;
          this.totalBlocked = response.totalBlocked;
        }
        this.isLoadingBlocked = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur enchères:', err);
        this.isLoadingBlocked = false;
      }
    });
  }

  formatBlockedDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  }

  viewProduct(produitId: number) {
    this.router.navigate(['/produit', produitId]);
  }

  refreshAll() {
    this.checkAuthentication();
    this.getBalance();
    this.loadHistory();
    this.loadBlockedAmounts();
  }
}
