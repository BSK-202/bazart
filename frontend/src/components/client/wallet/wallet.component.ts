import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WalletService, WalletBalanceResponse, RechargeResponse, HistoryResponse } from '../../../services/wallet.service';
import { AuthService } from '../../../services/auth.service';
import {EnchereService} from '../../../services/enchere.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, DatePipe],
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

  // ✅ AJOUTER CES PROPRIÉTES POUR LES MONTANTS BLOQUÉS
  blockedEncheres: any[] = [];
  totalBlocked: number = 0;
  isLoadingBlocked: boolean = false;

  constructor(
    private walletService: WalletService,
    private authService: AuthService,
    private enchereService: EnchereService, // Nouveau
    private router: Router // Nouveau
  ) {}

  ngOnInit() {
    console.log('💰 Initialisation du composant Wallet');

    // Vérification de l'authentification
    this.checkAuthentication();

    this.getBalance();
    this.loadHistory();
    this.loadBlockedAmounts(); // ✅ AJOUTER CET APPEL
  }
  checkAuthentication() {
    const isLoggedIn = this.authService.isLoggedIn();
    const token = this.authService.getToken();
    const user = this.authService.getUser();

    console.log('🔐 État authentification Wallet:');
    console.log('   - Connecté:', isLoggedIn);
    console.log('   - Token présent:', !!token);
    console.log('   - Utilisateur:', user);
    console.log('   - Contenu localStorage:');

    // Debug: afficher tout le localStorage
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      console.log(`     ${key}: ${localStorage.getItem(key!)}`);
    }

    if (!isLoggedIn) {
      this.message = '❌ Veuillez vous connecter pour accéder à votre portefeuille';
      console.error('Utilisateur non connecté - redirection nécessaire');

      // Optionnel : redirection automatique après délai
      setTimeout(() => {
        this.authService.redirectToLogin('Veuillez vous connecter pour accéder au portefeuille');
      }, 3000);
    }
  }

  // Dans wallet.component.ts - amélioration de getBalance()
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
          console.error('❌ Erreur dans la réponse:', response);
        }
      },
      error: (err: any) => {
        console.error('❌ Erreur lors du chargement du solde:', err);

        // ✅ CORRECTION : Gestion spécifique des erreurs wallet
        if (err.status === 401) {
          // Erreur d'authentification spécifique au wallet
          if (err.error?.message?.includes('token') || err.error?.error?.includes('JWT')) {
            this.message = 'Session expirée. Veuillez vous reconnecter.';
            setTimeout(() => {
              this.authService.redirectToLogin('Session expirée');
            }, 3000);
          } else {
            this.message = 'Accès non autorisé au portefeuille.';
          }
        } else if (err.status === 403) {
          this.message = 'Accès refusé.';
        } else {
          this.message = err.error?.error || 'Impossible de charger le solde.';
        }
      }
    });
  }
  confirmRecharge() {
    if (!this.authService.isLoggedIn()) {
      this.message = 'Veuillez vous connecter pour recharger votre portefeuille';
      return;
    }

    if (this.amountToAdd <= 0) {
      this.message = "Veuillez saisir un montant valide.";
      return;
    }
    this.pendingAmount = this.amountToAdd;
    this.showConfirmation = true;
    this.message = '';
    console.log(`💳 Confirmation recharge: ${this.pendingAmount} MAD`);
  }

  executeRecharge() {
    if (!this.authService.isLoggedIn()) {
      this.message = 'Session expirée. Veuillez vous reconnecter.';
      return;
    }

    this.isLoading = true;
    this.message = 'Recharge en cours...';
    console.log(`🔄 Exécution recharge: ${this.pendingAmount} MAD`);

    this.walletService.recharge(this.pendingAmount).subscribe({
      next: (response: RechargeResponse) => {
        this.isLoading = false;
        this.showConfirmation = false;
        console.log('✅ Réponse recharge:', response);

        if (response.success) {
          this.balance = response.newBalance;
          this.amountToAdd = 0;
          this.message = `✅ ${response.message}`;
          console.log(`💰 Nouveau solde: ${this.balance} MAD`);
          this.loadHistory();
        } else {
          this.message = '❌ Erreur lors de la recharge.';
          console.error('❌ Erreur recharge:', response);
        }
      },
      error: (err: any) => {
        this.isLoading = false;
        this.showConfirmation = false;
        console.error('❌ Erreur lors de la recharge:', err);
        if (err.status === 401) {
          this.message = 'Session expirée. Veuillez vous reconnecter.';
          this.authService.redirectToLogin('Session expirée');
        } else {
          this.message = err.error?.error || "Erreur lors de la recharge du portefeuille.";
        }
      }
    });
  }

  cancelRecharge() {
    this.showConfirmation = false;
    this.pendingAmount = 0;
    this.message = 'Recharge annulée.';
    console.log('❌ Recharge annulée');
  }

  setQuickAmount(amount: number) {
    this.amountToAdd = amount;
    this.message = '';
    console.log(`⚡ Montant rapide: ${amount} MAD`);
  }

  loadHistory() {
    if (!this.authService.isLoggedIn()) {
      return;
    }

    this.historyLoading = true;
    console.log(`📊 Chargement historique - Type: ${this.selectedOperationType}`);

    const historyCall = this.selectedOperationType === 'ALL'
      ? this.walletService.getHistory()
      : this.walletService.getHistoryByType(this.selectedOperationType);

    historyCall.subscribe({
      next: (response: HistoryResponse) => {
        console.log('✅ Réponse historique:', response);
        if (response.success) {
          this.history = response.history;
          console.log(`📋 ${this.history.length} opérations chargées`);
        } else {
          this.message = 'Erreur lors du chargement de l\'historique';
          console.error('❌ Erreur historique:', response);
        }
        this.historyLoading = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur lors du chargement de l\'historique:', err);
        if (err.status === 401) {
          this.message = 'Session expirée. Veuillez vous reconnecter.';
        } else {
          this.message = err.error?.error || 'Impossible de charger l\'historique.';
        }
        this.historyLoading = false;
      }
    });
  }

  onOperationTypeChange() {
    console.log(`🔍 Filtre historique changé: ${this.selectedOperationType}`);
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


  // Méthode pour charger les montants bloqués
  loadBlockedAmounts() {
    if (!this.authService.isLoggedIn()) {
      return;
    }

    this.isLoadingBlocked = true;
    console.log('🔒 Chargement des enchères en tête...');

    this.enchereService.getLeadingEncheres().subscribe({
      next: (response: any) => {
        console.log('✅ Réponse enchères en tête:', response);
        if (response.success) {
          this.blockedEncheres = response.leadingEncheres;
          this.totalBlocked = response.totalBlocked;
          console.log(`👑 ${this.blockedEncheres.length} enchères où vous êtes en tête`);
        } else {
          console.error('❌ Erreur enchères en tête:', response);
          this.blockedEncheres = [];
          this.totalBlocked = 0;
        }
        this.isLoadingBlocked = false;
      },
      error: (err: any) => {
        console.error('❌ Erreur chargement enchères en tête:', err);
        this.blockedEncheres = [];
        this.totalBlocked = 0;
        this.isLoadingBlocked = false;
      }
    });
  }



// Méthode pour formater la date des enchères bloquées
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

// Méthode pour naviguer vers la page du produit
  viewProduct(produitId: number) {
    this.router.navigate(['/produit', produitId]);
  }

// Méthode pour rafraîchir toutes les données
  refreshAll() {
    console.log('🔄 Rafraîchissement complet');
    this.checkAuthentication();
    this.getBalance();
    this.loadHistory();
    this.loadBlockedAmounts();
  }

}
