import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import {FormsModule} from '@angular/forms';

interface Produit {
  id: number;
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number | null;
  etat: string;
  vendeurNom: string;
  acheteurNom: string | null;
  categorieNom: string;
  nombreInteractions: number;
  nombreCommentaires: number;
  datepublication: string;
  dateenchere?: string;
  images: string[];
  imagePrincipale?: string;
  vendeurId?: number;
  dureeEnchereJours?: number;
  dateLike?: string;
}

interface User {
  name: string;
  email: string;
  id?: number;
  photoProfil?: string;
  prenom?: string;
  nom?: string;
  dateInscription?: string;
}

@Component({
  selector: 'app-user-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  imports: [CommonModule, FormsModule]
})
export class UserProfileComponent implements OnInit {
  activeTab: string = 'bids';
  user: User | null = null;
  userProfileImage: string = '';
  showProfileImage: boolean = false;
  joinDate: string = '';
  isLoading: boolean = true;

  // ✅ AJOUTER ICI (après les autres variables)
  userStats = {
    encheresActives: 0,
    produitsPubliés: 0,
    produitsVendus: 0,
    produitsFavoris: 0
  };

  showPaymentModal: boolean = false;
  selectedProduct: Produit | null = null;
  paymentAmount: number = 200; // Montant fixe ou configurable
  isProcessingPayment: boolean = false;
  walletBalance: number = 0;
  paymentError: string = '';
  showDurationModal: boolean = false;
  selectedProductForDuration: Produit | null = null;
  durationDays: number = 1; // Valeur par défaut
  maxDurationDays: number = 30; // Durée maximum
  pricePerDay: number = 100; // Prix par jour (fixe)
  calculatedPrice: number = 0;
  produitsFavoris: Produit[] = [];

  // Remplacez les données mockées par les vraies données
  produitsEncheres: Produit[] = [];
  produitsVendus: Produit[] = [];
  produitsPublies: Produit[] = [];
  produitsEnAttente: Produit[] = [];

  private readonly API_BASE_URL = 'http://localhost:8080';
  protected isAuctionStarting: boolean | undefined;

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute
  ) {}


  private updateUserStats(): void {
    this.userStats = {
      encheresActives: this.produitsEncheres.length,
      produitsPubliés: this.produitsPublies.length,
      produitsVendus: this.produitsVendus.length,
      produitsFavoris: this.produitsFavoris.length
    };
    console.log('📊 Statistiques utilisateur mises à jour:', this.userStats);
  }

  ngOnInit() {
    this.loadUserDataFromAPI();
    this.checkForTabParameter();
  }

  private checkForTabParameter(): void {
    // Vérifier les paramètres de requête
    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab = params['tab'];
        console.log('📌 Onglet activé via paramètre URL:', this.activeTab);
      }
    });

    // Vérifier l'état de navigation (state)
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state?.['activeTab']) {
      this.activeTab = navigation.extras.state['activeTab'];
      console.log('📌 Onglet activé via state:', this.activeTab);
    }
  }




  private loadUserDataFromAPI() {
    const userData = localStorage.getItem('userData');
    console.log("User data from localStorage:", userData);

    if (userData) {
      try {
        const parsedUser = JSON.parse(userData);
        const userId = parsedUser.id || parsedUser.userId || parsedUser.idclient;

        if (userId) {
          this.fetchUserFromAPI(userId);
          this.loadUserProducts(userId); // Charger les produits de l'utilisateur
        } else {
          console.error('User ID not found in localStorage');
          this.loadUserDataFromLocalStorage();
        }
      } catch (e) {
        console.error('Error parsing user data:', e);
        this.loadUserDataFromLocalStorage();
      }
    } else {
      this.isLoading = false;
      console.log('No user data found in localStorage');
    }
  }

  private loadUserProducts(userId: number) {
    const url = `${this.API_BASE_URL}/api/produits/vendeur/${userId}`;

    console.log('🔄 Chargement des produits du vendeur:', url);

    this.http.get<Produit[]>(url).subscribe({
      next: (produits) => {
        console.log('✅ Produits du vendeur reçus:', produits);

        // Filtrer les produits par état
        this.produitsEncheres = produits
          .filter(p => p.etat === 'en enchére' || p.etat === 'en_enchere')
          .map(p => this.ajouterImagePrincipale(p));

        this.produitsVendus = produits
          .filter(p => p.etat === 'vendu')
          .map(p => this.ajouterImagePrincipale(p));

        this.produitsPublies = produits
          .filter(p => p.etat === 'accepter' || p.etat === 'accepte')
          .map(p => this.ajouterImagePrincipale(p));

        this.produitsEnAttente = produits
          .filter(p => p.etat === 'en_attente')
          .map(p => this.ajouterImagePrincipale(p));

        console.log('📊 Produits triés:');
        console.log('   - En enchère:', this.produitsEncheres.length);
        console.log('   - Vendus:', this.produitsVendus.length);
        console.log('   - Publiés:', this.produitsPublies.length);
        console.log('   - En attente:', this.produitsEnAttente.length);
        this.loadProduitsFavoris(userId);
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement des produits:', error);
      }
    });
  }

  private ajouterImagePrincipale(produit: Produit): Produit {
    if (produit.images && produit.images.length > 0) {
      produit.imagePrincipale = this.getProduitImageUrl(produit.id, produit.images[0]);
    } else {
      produit.imagePrincipale = 'assets/images/placeholder.jpg';
    }
    return produit;
  }

  private getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === '') {
      return 'assets/images/placeholder.jpg';
    }

    if (imageName.startsWith('http') || imageName.startsWith('/api/')) {
      return `${this.API_BASE_URL}${imageName}`;
    }

    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  private fetchUserFromAPI(userId: number) {
    const url = `http://localhost:8080/api/clients/${userId}`;

    console.log('🔄 Fetching user data from API:', url);

    this.http.get<any>(url).subscribe({
      next: (response) => {
        console.log('✅ User data from API:', response);

        this.user = {
          name: response.nom && response.prenom
            ? `${response.prenom} ${response.nom}`.trim()
            : 'Utilisateur',
          email: response.email,
          id: response.idClient,
          photoProfil: response.photoProfil,
          prenom: response.prenom,
          nom: response.nom,
          dateInscription: response.dateInscription
        };

        if (this.user.photoProfil) {
          this.userProfileImage = this.user.photoProfil;
          this.showProfileImage = true;
        } else {
          this.showProfileImage = false;
        }

        this.calculateJoinDate();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error fetching user from API:', error);
        this.loadUserDataFromLocalStorage();
      }
    });
  }

  private loadUserDataFromLocalStorage() {
    const userData = localStorage.getItem('userData');

    if (userData) {
      try {
        const parsedUser = JSON.parse(userData);

        this.user = {
          name: parsedUser.nom
            ? `${parsedUser.prenom} ${parsedUser.nom}`.trim()
            : parsedUser.name || 'Utilisateur',
          email: parsedUser.email,
          id: parsedUser.id || parsedUser.userId || parsedUser.idclient,
          photoProfil: parsedUser.photoProfil,
          prenom: parsedUser.prenom,
          nom: parsedUser.nom,
          dateInscription: parsedUser.dateInscription || parsedUser.createdAt
        };

        if (this.user.photoProfil) {
          this.userProfileImage = this.user.photoProfil;
          this.showProfileImage = true;
        } else {
          this.showProfileImage = false;
        }

        this.calculateJoinDate();

      } catch (e) {
        console.error('Error parsing user data from localStorage:', e);
        this.user = null;
        this.joinDate = '';
        this.showProfileImage = false;
      }
    } else {
      this.user = null;
      this.joinDate = '';
      this.showProfileImage = false;
    }

    this.isLoading = false;
  }

  private calculateJoinDate() {
    if (this.user?.dateInscription) {
      try {
        const joinDate = new Date(this.user.dateInscription);
        this.joinDate = this.formatJoinDate(joinDate);
        console.log('📅 Join date calculated:', this.joinDate);
      } catch (e) {
        console.error('Error parsing dateInscription:', e);
        this.joinDate = '';
      }
    } else {
      this.joinDate = '';
      console.log('No dateInscription found');
    }
  }

  private formatJoinDate(date: Date): string {
    const months = [
      'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
      'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
    ];

    const month = months[date.getMonth()];
    const year = date.getFullYear();

    return `${month} ${year}`;
  }

  // Gérer l'erreur de chargement d'image
  onImageError() {
    console.log('Profile image not found, using default avatar');
    this.showProfileImage = false;
  }

  getInitials(): string {
    if (this.user?.prenom && this.user?.nom) {
      return (this.user.prenom[0] + this.user.nom[0]).toUpperCase();
    } else if (this.user?.name) {
      const names = this.user.name.split(' ');
      if (names.length >= 2) {
        return (names[0][0] + names[1][0]).toUpperCase();
      }
      return this.user.name.substring(0, 2).toUpperCase();
    }
    return ''; // Retourne vide si pas d'utilisateur
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    // Optionnel: mettre à jour l'URL sans recharger la page
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tab },
      queryParamsHandling: 'merge'
    });
  }

  formatCurrency(amount: number): string {
    return amount.toLocaleString('fr-MA') + ' DH';
  }

  // Méthode pour naviguer vers le détail du produit
  goToProductDetail(produitId: number): void {
    console.log('🎯 Navigation vers le produit:', produitId);
    this.router.navigate(['/produit', produitId]);
  }

  // Méthode pour formater la date
  formatDate(dateString: string): string {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  }

  // Méthode pour démarrer une enchère
  startAuction(produit: Produit): void {
    console.log('🚀 Démarrage de l\'enchère pour le produit:', produit.id);
    this.isAuctionStarting = true;

    // Ici vous pouvez appeler votre API pour démarrer l'enchère
    const url = `${this.API_BASE_URL}/api/produits/${produit.id}/start-auction`;

    this.http.post(url, {}).subscribe({
      next: (response) => {
        console.log('✅ Enchère démarrée avec succès:', response);
        this.isAuctionStarting = false;

        // Recharger les données pour mettre à jour l'affichage
        if (this.user?.id) {
          this.loadUserProducts(this.user.id);
        }

        // Optionnel: Afficher un message de succès
        alert('L\'enchère a été démarrée avec succès!');
      },
      error: (error) => {
        console.error('❌ Erreur lors du démarrage de l\'enchère:', error);
        this.isAuctionStarting = false;

        // Optionnel: Afficher un message d'erreur
        alert('Erreur lors du démarrage de l\'enchère. Veuillez réessayer.');
      }
    });
  }

  manageAuction(produit: Produit) {

  }

  // Méthode pour calculer la durée de l'enchère
  calculateAuctionDuration(dateEnchereString: string): string {
    if (!dateEnchereString) return 'Nouvelle';

    try {
      const dateEnchere = new Date(dateEnchereString);
      const now = new Date();
      const diffMs = now.getTime() - dateEnchere.getTime();
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
      const diffDays = Math.floor(diffHours / 24);

      if (diffDays > 0) {
        return `${diffDays} jour${diffDays > 1 ? 's' : ''}`;
      } else if (diffHours > 0) {
        return `${diffHours} heure${diffHours > 1 ? 's' : ''}`;
      } else {
        return 'Moins d\'1 heure';
      }
    } catch (e) {
      console.error('Error calculating auction duration:', e);
      return 'N/A';
    }
  }

  // Méthode pour démarrer l'enchère avec paiement
  startAuctionWithPayment(produit: Produit): void {
    console.log('💳 Démarrage avec paiement pour le produit:', produit.id);
    this.selectedProduct = produit;
    this.showPaymentModal = true;
    this.paymentError = '';

    // Charger le solde du wallet
    this.loadWalletBalance();
  }

// Méthode pour charger le solde du wallet
  private loadWalletBalance(): void {
    const url = 'http://localhost:8080/api/wallet/balance';

    this.http.get<any>(url).subscribe({
      next: (response) => {
        if (response.success) {
          this.walletBalance = response.balance;
          console.log('💰 Solde du wallet:', this.walletBalance);
        } else {
          console.error('❌ Erreur lors du chargement du solde:', response.error);
          this.paymentError = 'Erreur lors du chargement du solde';
        }
      },
      error: (error) => {
        console.error('❌ Erreur API wallet:', error);
        this.paymentError = 'Erreur de connexion au wallet';
      }
    });
  }

// Méthode pour confirmer le paiement
  confirmPayment(): void {
    if (!this.selectedProduct) return;

    this.isProcessingPayment = true;
    this.paymentError = '';

    // Vérifier si le solde est suffisant
    if (this.walletBalance < this.paymentAmount) {
      this.paymentError = `Solde insuffisant. Votre solde: ${this.formatCurrency(this.walletBalance)}, Montant requis: ${this.formatCurrency(this.paymentAmount)}`;
      this.isProcessingPayment = false;
      return;
    }

    // Effectuer le débit du wallet
    this.debitWalletForAuction();
  }

// Méthode pour débiter le wallet
  private debitWalletForAuction(): void {
    const debitRequest = {
      amount: this.paymentAmount,
      description: `Paiement pour démarrage enchère - Produit: ${this.selectedProduct?.nom}`
    };

    const url = 'http://localhost:8080/api/wallet/debit';

    this.http.post<any>(url, debitRequest).subscribe({
      next: (response) => {
        if (response.success) {
          console.log('✅ Paiement effectué avec succès:', response);
          this.startAuctionAfterPayment();
        } else {
          this.paymentError = response.error || 'Erreur lors du paiement';
          this.isProcessingPayment = false;
        }
      },
      error: (error) => {
        console.error('❌ Erreur lors du débit:', error);
        this.paymentError = 'Erreur lors du traitement du paiement';
        this.isProcessingPayment = false;
      }
    });
  }
  // Dans profile.component.ts
  getProductDescription(): string {
    if (!this.selectedProduct?.description) {
      return 'Aucune description disponible';
    }

    const description = this.selectedProduct.description;
    return description.length > 100
      ? description.slice(0, 100) + '...'
      : description;
  }

  private startAuctionAfterPayment(): void {
    if (!this.selectedProduct) {
      console.error('❌ Aucun produit sélectionné');
      this.isProcessingPayment = false;
      this.paymentError = 'Erreur: aucun produit sélectionné';
      return;
    }

    console.log('🚀 Démarrage de l\'enchère après paiement pour le produit:', this.selectedProduct.id);
    console.log(`📅 Durée configurée: ${this.durationDays} jours`);

    // ✅ ENVOYER SEULEMENT LA DURÉE (le prix est calculé côté backend si besoin)
    const requestBody = {
      dureeEnchereJours: this.durationDays
    };

    const url = `${this.API_BASE_URL}/api/produits/${this.selectedProduct.id}/start-auction`;

    this.http.post(url, requestBody).subscribe({
      next: (response) => {
        console.log('✅ Enchère démarrée avec succès:', response);
        this.isProcessingPayment = false;
        this.showPaymentModal = false;
        this.selectedProduct = null;
        this.selectedProductForDuration = null;

        // Recharger les données pour mettre à jour l'affichage
        if (this.user?.id) {
          this.loadUserProducts(this.user.id);
        }

        alert(`L'enchère a été démarrée avec succès pour ${this.durationDays} jours! Paiement de ${this.formatCurrency(this.calculatedPrice)} effectué.`);
      },
      error: (error) => {
        console.error('❌ Erreur lors du démarrage de l\'enchère:', error);
        this.isProcessingPayment = false;
        this.paymentError = 'Erreur lors du démarrage de l\'enchère après paiement';
        this.refundPayment();
      }
    });
  }


/// Remplacer l'ancienne méthode par celle-ci :
  private refundPayment(): void {
    const rechargeRequest = {
      amount: this.paymentAmount,
      description: `Remboursement - Erreur démarrage enchère pour produit ${this.selectedProduct?.id || 'inconnu'}`
    };

    const url = 'http://localhost:8080/api/wallet/recharge';

    this.http.post<any>(url, rechargeRequest).subscribe({
      next: (response) => {
        console.log('💰 Remboursement effectué:', response);
      },
      error: (error) => {
        console.error('❌ Erreur lors du remboursement:', error);
      },
      complete: () => {
        // Réinitialiser le produit sélectionné après remboursement
        this.selectedProduct = null;
      }
    });
  }

// Méthode pour annuler le paiement
  cancelPayment(): void {
    this.showPaymentModal = false;
    this.selectedProduct = null;
    this.isProcessingPayment = false;
    this.paymentError = '';
  }


  // Méthode pour valider la durée et passer au paiement
  validateDuration(): void {
    if (!this.selectedProductForDuration) return;

    if (this.durationDays < 1 || this.durationDays > this.maxDurationDays) {
      this.paymentError = `La durée doit être entre 1 et ${this.maxDurationDays} jours`;
      return;
    }

    // Fermer le modal de durée et ouvrir le modal de paiement
    this.showDurationModal = false;

    // Mettre à jour le montant de paiement avec le prix calculé
    this.paymentAmount = this.calculatedPrice;

    // Ouvrir le modal de paiement
    this.selectedProduct = this.selectedProductForDuration;
    this.showPaymentModal = true;

    // Charger le solde du wallet
    this.loadWalletBalance();
  }
  // Méthode pour calculer le prix en fonction de la durée
  calculatePrice(): void {
    this.calculatedPrice = this.durationDays * this.pricePerDay;
    console.log(`💰 Calcul prix: ${this.durationDays} jours × ${this.pricePerDay} DH = ${this.calculatedPrice} DH`);
  }

  // Méthode pour ouvrir le modal de durée
  openDurationModal(produit: Produit): void {
    console.log('📅 Ouverture modal durée pour le produit:', produit.id);
    this.selectedProductForDuration = produit;
    this.durationDays = 1; // Réinitialiser à 1 jour
    this.calculatePrice();
    this.showDurationModal = true;
    this.paymentError = '';
  }

  // Dans profile.component.ts, ajouter cette méthode pour charger les favoris
  private loadProduitsFavoris(userId: number): void {
    const url = `${this.API_BASE_URL}/api/interactions/client/${userId}/produits-likes`;

    console.log('🔄 Chargement des produits favoris:', url);

    this.http.get<any[]>(url).subscribe({
      next: (produitsLikes) => {
        console.log('✅ Produits favoris reçus:', produitsLikes);

        // Convertir les données en format Produit
        this.produitsFavoris = produitsLikes.map(produitData => {
          const produit: Produit = {
            id: produitData.id,
            nom: produitData.nom,
            description: produitData.description,
            prixDebut: produitData.prixDebut,
            prixFin: produitData.prixFin,
            etat: produitData.etat,
            vendeurNom: produitData.vendeurNom,
            acheteurNom: null, // Pas disponible dans les données de like
            categorieNom: produitData.categorieNom,
            nombreInteractions: produitData.nombreInteractions,
            nombreCommentaires: produitData.nombreCommentaires,
            datepublication: produitData.datePublication ?
              new Date(produitData.datePublication).toISOString() : '',
            dateenchere: produitData.dateEnchere ?
              new Date(produitData.dateEnchere).toISOString() : undefined,
            images: produitData.images || [],
            imagePrincipale: '',
            vendeurId: produitData.vendeurId,
            dureeEnchereJours: produitData.dureeEnchereJours
          };

          // Ajouter l'image principale
          return this.ajouterImagePrincipale(produit);
        });

        console.log('❤️ Produits favoris chargés:', this.produitsFavoris.length);
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement des favoris:', error);
        this.produitsFavoris = [];
      }
    });
  }

  // Dans profile.component.ts, ajouter ces méthodes
  getEtatDisplay(etat: string): string {
    const etats: { [key: string]: string } = {
      'en_attente': 'En attente',
      'accepte': 'Accepté',
      'accepter': 'Accepté',
      'en_enchere': 'En enchère',
      'vendu': 'Vendu',
      'refuser': 'Refusé'
    };
    return etats[etat] || etat;
  }

  toggleFavorite(produit: Produit): void {
    if (!this.user?.id) return;

    const url = `${this.API_BASE_URL}/api/interactions/toggle/${produit.id}`;

    this.http.post(url, {}, {
      headers: { 'X-Client-Id': this.user.id.toString() }
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Like toggle:', response);

        // Recharger les favoris
        if (this.user?.id) {
          this.loadProduitsFavoris(this.user.id);
        }
      },
      error: (error) => {
        console.error('❌ Erreur toggle like:', error);
      }
    });
  }
}
