import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {forkJoin} from 'rxjs';

interface Produit {
  id: number;
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number | null;
  etat: string;
  expertiseApproved?: boolean;
  vendeurNom: string;
  vendeurPhone?: string; // ← Ajoutez si disponible
  vendeurEmail?: string; // ← Ajoutez si disponible
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
  aExpertise?: boolean;
  categorieId?: number;
  domaineId?: number;
  nombreFavoris?: number; // ✅ AJOUTER CETTE PROPRIÉTÉ
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

interface Domaine {
  idDomaine: number;
  nomDomaine: string;
}

interface Categorie {
  idCategorie: number;
  nomCategorie: string;
  domaine: Domaine;
}

@Component({
  selector: 'app-user-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  imports: [CommonModule, FormsModule],
  standalone: true,
})
export default class UserProfileComponent implements OnInit {
  activeTab: string = 'bids';
  user: User | null = null;
  userProfileImage: string = '';
  showProfileImage: boolean = false;
  joinDate: string = '';
  isLoading: boolean = true;
  inAppEnabled: boolean = true;
  emailEnabled: boolean = false;

  showContactModal = false;

  selectedProductForDecision: Produit | null = null;

  selectedSellerName = '';
  selectedSellerPhone = '';
  selectedSellerEmail = '';
  selectedSellerPhoto = '';


  // Données pour l'édition
  isEditingProfile = false;
  isFullEditProduct = false;
  editedUser: any = {};
  editedProduct: Produit | null = null;
  isEditingProfilePhoto = false;
  profilePhotoPreview = "";
  profilePhotoFile: File | null = null;
  isUploadingProfilePhoto = false;
  isSavingProfile = false;
  originalImages: string[] = []; // Pour stocker les noms des images originales

  // Données pour l'édition complète
  domaines: Domaine[] = [];
  filteredCategories: Categorie[] = [];
  imagePreviews: string[] = [];
  newImages: File[] = [];
  imagesToDelete: string[] = [];
  isDragOver = false;
// Ajouter après les autres propriétés de produits
  produitsGagnes: Produit[] = [];
  // ✅ AJOUTER ICI (après les autres variables)
  userStats = {
    encheresActives: 0,
    produitsPubliés: 0,
    produitsVendus: 0,
    produitsFavoris: 0,
    produitsGagnes:0
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

  showSaleValidationModal: boolean = false;
  selectedProductForValidation: Produit | null = null;
  saleValidationMessage: string = '';
  isProcessingSaleValidation: boolean = false;

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
      produitsFavoris: this.produitsFavoris.length,
      produitsGagnes: this.produitsGagnes.length // ← AJOUTER CETTE LIGNE
    };
    console.log('📊 Statistiques utilisateur mises à jour:', this.userStats);
  }

  ngOnInit() {
    this.loadUserDataFromAPI();
    if (this.user?.id) {
      this.http.get<any>(`${this.API_BASE_URL}/api/user-notification-preference/${this.user.id}`).subscribe({
        next: pref => {
          this.inAppEnabled = pref?.inAppEnabled ?? true;
          this.emailEnabled = pref?.emailEnabled ?? false;
        }
      });
    }
    this.checkForTabParameter();
    this.loadDomaines();
  }


  openEditProfilePhoto(): void {
    this.isEditingProfilePhoto = true;
    this.profilePhotoPreview = this.userProfileImage;
    this.profilePhotoFile = null;
  }

  onProfilePhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];

      if (!file.type.startsWith("image/")) {
        alert("Veuillez sélectionner une image valide");
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        alert("L'image est trop volumineuse (max 5MB)");
        return;
      }

      this.profilePhotoFile = file;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.profilePhotoPreview = e.target?.result as string;
      }
      reader.readAsDataURL(file);
    }
  }

  saveProfilePhoto(): void {
    if (!this.profilePhotoFile || !this.user?.id) return;

    this.isUploadingProfilePhoto = true;
    const formData = new FormData();
    formData.append("photoProfil", this.profilePhotoFile, this.profilePhotoFile.name);

    const url = `${this.API_BASE_URL}/api/clients/${this.user.id}/photo`;

    console.log("📤 Upload de photo vers:", url);

    this.http.post(url, formData).subscribe({
      next: (response: any) => {
        console.log("✅ Réponse de l'upload:", response);

        // ✅ CORRECTION: Utiliser le bon champ de réponse
        if (response.photoProfil) {
          this.userProfileImage = response.photoProfil;
        } else if (response.profileImageUrl) {
          this.userProfileImage = response.profileImageUrl;
        } else {
          // Fallback: reconstruire l'URL
          // @ts-ignore
          this.userProfileImage = `${this.API_BASE_URL}/api/clients/images/${this.user.id}.jpg`;
        }

        this.showProfileImage = true;
        if (this.user) {
          this.user.photoProfil = this.userProfileImage;
        }

        this.isUploadingProfilePhoto = false;
        this.isEditingProfilePhoto = false;
        alert("✅ Photo de profil mise à jour avec succès!");
      },
      error: (error) => {
        console.error("❌ Erreur lors de l'upload de la photo:", error);
        this.isUploadingProfilePhoto = false;

        let errorMessage = "Erreur lors de l'upload de la photo. Veuillez réessayer.";
        if (error.status === 403) {
          errorMessage = "Accès refusé. Vérifiez que l'endpoint existe sur le serveur.";
        } else if (error.status === 404) {
          errorMessage = "Endpoint non trouvé. Vérifiez l'URL.";
        } else if (error.status === 413) {
          errorMessage = "Fichier trop volumineux (max 5MB).";
        } else if (error.error?.message) {
          errorMessage = error.error.message;
        }

        alert(errorMessage);
      },
    });
  }

  cancelEditProfilePhoto(): void {
    this.isEditingProfilePhoto = false;
    this.profilePhotoPreview = "";
    this.profilePhotoFile = null;
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

  savePreferences() {
    if (!this.user?.id) return;
    this.http.post(`${this.API_BASE_URL}/api/user-notification-preference`, {
      userId: this.user.id,
      inAppEnabled: this.inAppEnabled,
      emailEnabled: this.emailEnabled
    }).subscribe({
      next: () => {},
      error: err => console.error('Failed to save notification preferences', err)
    });
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

  private loadDomaines() {
    const url = `${this.API_BASE_URL}/api/domaines`;
    this.http.get<Domaine[]>(url).subscribe({
      next: (domaines) => {
        this.domaines = domaines;
        console.log("Domaines chargés:", this.domaines);
      },
      error: (error) => {
        console.error("Erreur lors du chargement des domaines:", error);
      },
    });
  }

  onDomainChange(domainId: string) {
    console.log("🔄 Changement de domaine:", domainId);

    if (domainId && domainId !== "") {
      const url = `${this.API_BASE_URL}/api/categories/domaine/${domainId}`;
      console.log("[API] Appel catégories ->", url);

      this.http.get<any[]>(url).subscribe({
        next: (data) => {
          console.log("[API] Réponse catégories:", data);
          this.filteredCategories = data || [];

          // ✅ S'assurer que la catégorie sélectionnée est dans la liste
          if (this.editedProduct?.categorieId && this.filteredCategories.length > 0) {
            const categoryExists = this.filteredCategories.some(
              cat => cat.idCategorie === this.editedProduct!.categorieId
            );

            if (!categoryExists) {
              console.warn("⚠️ La catégorie sélectionnée n'existe pas dans ce domaine, réinitialisation...");
              this.editedProduct.categorieId = undefined;
            }
          }
        },
        error: (err) => {
          console.error("[API] Erreur lors de la récupération des catégories:", err);
          this.filteredCategories = [];
        },
      });
    } else {
      this.filteredCategories = [];
    }
  }

  // ✅ NOUVELLE MÉTHODE : Charger tous les likes pour les produits du vendeur
  private loadAllLikesForVendeur(vendeurId: number): Promise<Map<number, number>> {
    return new Promise((resolve) => {
      const url = `${this.API_BASE_URL}/api/interactions/vendeur/${vendeurId}/likes-count`;

      console.log('🔄 Chargement de tous les likes pour vendeur:', url);

      this.http.get<{ [key: string]: number }>(url).subscribe({
        next: (likesData) => {
          console.log('✅ Tous les likes pour vendeur reçus:', likesData);

          // Convertir l'objet en Map
          const likesMap = new Map<number, number>();
          Object.entries(likesData).forEach(([produitId, count]) => {
            likesMap.set(Number(produitId), count);
          });

          console.log('📊 Map des likes créée:', likesMap);
          resolve(likesMap);
        },
        error: (error) => {
          console.error('❌ Erreur lors du chargement des likes vendeur:', error);
          // En cas d'erreur, retourner une Map vide
          resolve(new Map<number, number>());
        }
      });
    });
  }

  private loadUserProducts(userId: number): Promise<void> {
    return new Promise((resolve) => {
      const url = `${this.API_BASE_URL}/api/produits/vendeur/${userId}`;

      console.log('🔄 Chargement des produits du vendeur:', url);

      this.http.get<Produit[]>(url).subscribe({
        next: (produits) => {
          console.log('✅ Produits du vendeur reçus:', produits);

          this.loadAllLikesForVendeur(userId).then(likesMap => {
            const produitsAvecLikes = produits.map(produit => {
              const likeCount = likesMap.get(produit.id) || 0;
              return {
                ...produit,
                nombreInteractions: likeCount
              };
            });

            this.produitsEncheres = produitsAvecLikes
              .filter(p => p.etat === 'en enchére' || p.etat === 'en_enchere' || p.etat === 'relance')
              .map(p => this.ajouterImagePrincipale(p));

            this.produitsVendus = produitsAvecLikes
              .filter(p => p.etat === 'vendu' || p.etat === 'enchere_termine' || p.etat === 'vendeur_accepte_vente')
              .map(p => this.ajouterImagePrincipale(p));

            this.produitsPublies = produitsAvecLikes
              .filter((p) => ["accepter", "accepte", "expertise_validee", "transaction_annulee","enchere_termine_sans_gagnant","vendeur_refuse_vente"].includes(p.etat))
              .map((p) => this.ajouterImagePrincipale(p))

            this.produitsEnAttente = produitsAvecLikes
              .filter(p => p.etat === 'en_attente')
              .map(p => this.ajouterImagePrincipale(p));

            console.log('📊 Produits triés avec likes:');
            console.log('   - En enchère:', this.produitsEncheres.length);
            console.log('   - Vendus:', this.produitsVendus.length);
            console.log('   - Publiés:', this.produitsPublies.length);
            console.log('   - En attente:', this.produitsEnAttente.length);

            this.updateUserStats();
            resolve(); // Résoudre la promesse une fois le chargement terminé
          });

          this.loadProduitsFavoris(userId);
          this.loadProduitsGagnes(userId);
        },
        error: (error) => {
          console.error('❌ Erreur lors du chargement des produits:', error);
          resolve(); // Résoudre même en cas d'erreur
        }
      });
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

  formatCurrency(amount: number | undefined | null): string {
    if (amount === undefined || amount === null || isNaN(amount)) {
      return '0 DH';
    }
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


  manageAuction(produit: Produit) {
    // Implémentation de la gestion de l'enchère ici
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

        // Afficher un message de succès
        alert(`L'enchère a été démarrée avec succès pour ${this.durationDays} jours! Paiement de ${this.formatCurrency(this.calculatedPrice)} effectué.`);

        // ✅ NOUVEAU : Recharger les données et rediriger vers l'onglet "Mes Enchères"
        if (this.user?.id) {
          this.loadUserProducts(this.user.id).then(() => {
            // Rediriger vers l'onglet "Mes Enchères"
            this.setActiveTab('bids');

            // Optionnel : faire défiler vers le haut de la section
            setTimeout(() => {
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }, 100);
          });
        }
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
      'accepte': 'Publié',
      'accepter': 'Publié',
      'vendeur_refuse_vente': 'Republié',
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

  private loadProductCategoryAndDomain(product: Produit) {
    console.log("🔍 Chargement catégorie et domaine pour le produit:", product.id);
    console.log("   - categorieId:", product.categorieId);
    console.log("   - categorieNom:", product.categorieNom);
    console.log("   - domaineId:", product.domaineId);

    // ✅ APPROCHE 1: Si on a déjà le domaineId, l'utiliser directement
    if (product.domaineId) {
      console.log("✅ Utilisation du domaineId existant:", product.domaineId);
      this.editedProduct!.domaineId = product.domaineId;

      // Charger les catégories de ce domaine
      this.onDomainChange(product.domaineId.toString());

      // Définir la catégorie après un court délai
      setTimeout(() => {
        if (this.editedProduct) {
          this.editedProduct.categorieId = product.categorieId;
          console.log("✅ Catégorie définie:", this.editedProduct.categorieId);
        }
      }, 200);

      return;
    }

    // ✅ APPROCHE 2: Si on n'a pas le domaineId mais on a le categorieId
    if (product.categorieId) {
      console.log("🔄 Recherche du domaine via la catégorie:", product.categorieId);

      const categoryUrl = `${this.API_BASE_URL}/api/categories/${product.categorieId}`;
      console.log("📡 Appel API catégorie:", categoryUrl);

      this.http.get<any>(categoryUrl).subscribe({
        next: (category) => {
          console.log("✅ Catégorie récupérée:", category);

          if (category && category.domaine) {
            console.log("   - Domaine trouvé:", category.domaine.nomDomaine, "(ID:", category.domaine.idDomaine, ")");

            // ✅ Définir le domaine
            this.editedProduct!.domaineId = category.domaine.idDomaine;

            // ✅ Charger les catégories du domaine
            this.onDomainChange(category.domaine.idDomaine.toString());

            // ✅ Attendre que les catégories soient chargées avant de définir la catégorie
            setTimeout(() => {
              if (this.editedProduct) {
                this.editedProduct.categorieId = product.categorieId;
                console.log("✅ Catégorie définie:", this.editedProduct.categorieId);
              }
            }, 300);
          } else {
            console.warn("⚠️ Domaine non trouvé dans la catégorie");
          }
        },
        error: (err) => {
          console.error("❌ Erreur lors du chargement de la catégorie:", err);
          this.tryAlternativeCategoryLoad(product);
        },
      });
    } else {
      console.warn("⚠️ Aucun categorieId trouvé pour ce produit");
    }
  }

  private tryAlternativeCategoryLoad(product: Produit) {
    const allCategoriesUrl = `${this.API_BASE_URL}/api/categories`;
    this.http.get<any[]>(allCategoriesUrl).subscribe({
      next: (categories) => {
        const productCategory = categories.find(
          (cat) => cat.idCategorie === product.categorieId || cat.nomCategorie === product.categorieNom,
        );
        if (productCategory && productCategory.domaine) {
          this.editedProduct!.domaineId = productCategory.domaine.idDomaine;
          this.onDomainChange(productCategory.domaine.idDomaine.toString());

          setTimeout(() => {
            this.editedProduct!.categorieId = product.categorieId;
          }, 100);
        }
      },
      error: (err) => {
        console.error("❌ Erreur alternative également:", err);
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(input.files);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    if (event.dataTransfer?.files) {
      this.handleFiles(event.dataTransfer.files);
    }
  }

  // Remplacer les méthodes existantes par celles-ci :
  openFullEditProduct(produit: Produit): void {
    console.log("🖊️ Ouverture édition complète du produit:", produit);

    this.isFullEditProduct = true;

    // Copier le produit
    this.editedProduct = {
      ...produit,
      prixFin: produit.prixFin || null,
      aExpertise: produit.aExpertise || false,
      categorieId: produit.categorieId,
      domaineId: produit.domaineId,
      vendeurId: produit.vendeurId || this.user?.id,
      etat: produit.etat,
    };

    console.log("📝 Produit à éditer:", {
      nom: this.editedProduct.nom,
      domaineId: this.editedProduct.domaineId,
      categorieId: this.editedProduct.categorieId
    });

    // ✅ CORRECTION : ÉLIMINER LES DOUBLONS des images
    this.originalImages = this.removeDuplicateImages(produit.images || []);
    this.imagePreviews = [];
    this.newImages = [];
    this.imagesToDelete = [];

    console.log("🖼️ Images après suppression des doublons:", this.originalImages);

    // Charger les aperçus des images UNIQUES
    this.originalImages.forEach((img, index) => {
      const imageUrl = this.getProduitImageUrl(produit.id, img);
      this.imagePreviews.push(imageUrl);
      console.log(`🖼️ Image unique [${index}]: ${img}`);
    });

    // ✅ CORRECTION : Charger immédiatement le domaine et la catégorie
    this.loadProductCategoryAndDomain(produit);
  }

  // ✅ NOUVELLE MÉTHODE : Supprimer les doublons d'images
  private removeDuplicateImages(images: string[]): string[] {
    const uniqueImages: string[] = [];
    const seen = new Set<string>();

    images.forEach(img => {
      if (!seen.has(img)) {
        seen.add(img);
        uniqueImages.push(img);
      }
    });

    console.log(`🧹 Nettoyage doublons: ${images.length} → ${uniqueImages.length} images`);
    return uniqueImages;
  }

  private handleFiles(files: FileList): void {
    console.log("📁 Fichiers reçus:", files.length);

    const maxTotalImages = 10;
    const remainingSlots = maxTotalImages - this.imagePreviews.length;

    if (remainingSlots <= 0) {
      alert(`Maximum ${maxTotalImages} images autorisées. Supprimez une image existante pour en ajouter une nouvelle.`);
      return;
    }

    const filesToAdd = Math.min(files.length, remainingSlots);

    for (let i = 0; i < filesToAdd; i++) {
      const file = files[i];

      if (!file.type.startsWith("image/")) {
        alert("Veuillez sélectionner uniquement des images");
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        alert("L'image est trop volumineuse (max 10MB)");
        continue;
      }

      this.newImages.push(file);
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreviews.push(e.target?.result as string);
        console.log("🖼️ Nouvel aperçu ajouté, total:", this.imagePreviews.length);
      };
      reader.readAsDataURL(file);
    }

    if (files.length > filesToAdd) {
      alert(`Seulement ${filesToAdd} image(s) ajoutée(s) sur ${files.length}. Maximum ${maxTotalImages} images autorisées.`);
    }
  }

  removeImage(index: number): void {
    console.log("🗑️ Suppression de l'image à l'index:", index);
    console.log("📊 Avant suppression:", {
      previews: this.imagePreviews.length,
      original: this.originalImages.length,
      nouvelles: this.newImages.length,
      àSupprimer: this.imagesToDelete.length
    });

    // Déterminer si c'est une image existante ou nouvelle
    const isExistingImage = index < this.originalImages.length;

    if (isExistingImage) {
      // ✅ CORRECTION : Image existante - la marquer pour suppression
      const imageName = this.originalImages[index];
      if (imageName && !this.imagesToDelete.includes(imageName)) {
        this.imagesToDelete.push(imageName);
        console.log("🗑️ Image existante marquée pour suppression:", imageName);
      }

      // Retirer de la liste des originales ET des previews
      this.originalImages.splice(index, 1);
      this.imagePreviews.splice(index, 1);

    } else {
      // ✅ CORRECTION : Nouvelle image - calculer l'index correct
      const newImageIndex = index - this.originalImages.length;
      if (newImageIndex >= 0 && newImageIndex < this.newImages.length) {
        this.newImages.splice(newImageIndex, 1);
        this.imagePreviews.splice(index, 1);
        console.log("📝 Nouvelle image retirée");
      }
    }

    console.log("📊 Après suppression:", {
      previews: this.imagePreviews.length,
      original: this.originalImages.length,
      nouvelles: this.newImages.length,
      àSupprimer: this.imagesToDelete.length
    });
  }

  saveFullProduct(): void {
    if (!this.editedProduct) return;

    // Validation
    if (this.imagePreviews.length < 3) {
      alert("Veuillez ajouter au moins 3 images");
      return;
    }

    const formData = new FormData();

    // Données du produit
    const produitData = {
      nom: this.editedProduct.nom,
      description: this.editedProduct.description,
      prixDebut: this.editedProduct.prixDebut,
      prixFin: this.editedProduct.prixFin || null,
      aExpertise: this.editedProduct.aExpertise || false,
      etat: this.editedProduct.etat || "en_attente",
      categorieId: Number(this.editedProduct.categorieId),
      vendeurId: this.editedProduct.vendeurId || this.user?.id,
    };

    formData.append("produit", new Blob([JSON.stringify(produitData)], {
      type: "application/json"
    }));

    // ✅ CORRECTION : Ajouter les NOUVELLES images
    this.newImages.forEach((file, index) => {
      formData.append("images", file, file.name);
      console.log(`📸 Nouvelle image ajoutée: ${file.name}`);
    });

    // ✅ CORRECTION : Envoyer les images à supprimer
    if (this.imagesToDelete.length > 0) {
      formData.append("imagesToDelete", JSON.stringify(this.imagesToDelete));
      console.log("🗑️ Images à supprimer envoyées:", this.imagesToDelete);
    } else {
      formData.append("imagesToDelete", "[]");
    }

    const url = `${this.API_BASE_URL}/api/produits/${this.editedProduct.id}`;

    console.log("📤 Envoi modification produit:", {
      produitId: this.editedProduct.id,
      nouvellesImages: this.newImages.length,
      imagesASupprimer: this.imagesToDelete.length,
      imagesRestantes: this.originalImages.length - this.imagesToDelete.length,
      totalFinal: this.imagePreviews.length
    });

    this.http.put(url, formData).subscribe({
      next: (response: any) => {
        console.log("✅ Produit mis à jour avec succès:", response);
        alert("🎉 Produit mis à jour avec succès !");

        if (this.user?.id) {
          this.loadUserProducts(this.user.id);
        }

        this.cancelFullEdit();
      },
      error: (error) => {
        console.error("❌ Erreur lors de la mise à jour:", error);
        let errorMessage = "Erreur lors de la mise à jour du produit";

        if (error.status === 400) {
          errorMessage = "Données invalides. Vérifiez les champs.";
        } else if (error.status === 404) {
          errorMessage = "Produit non trouvé";
        } else if (error.error?.message) {
          errorMessage = error.error.message;
        }

        alert(`❌ ${errorMessage}`);
      }
    });
  }

  cancelFullEdit(): void {
    this.isFullEditProduct = false;
    this.editedProduct = null;
    this.imagePreviews = [];
    this.newImages = [];
    this.imagesToDelete = [];
    this.filteredCategories = [];
  }

  openEditProfile(): void {
    this.isEditingProfile = true;
    this.editedUser = {
      prenom: this.user?.prenom || "",
      nom: this.user?.nom || "",
      email: this.user?.email || "",
    };
    console.log("📝 Ouverture édition profil:", this.editedUser);
  }

  saveProfile(): void {
    if (!this.user?.id) {
      alert("Erreur: ID utilisateur non trouvé");
      return;
    }

    // Validation basique
    if (!this.editedUser.prenom || !this.editedUser.nom || !this.editedUser.email) {
      alert("Veuillez remplir tous les champs");
      return;
    }

    this.isSavingProfile = true;
    console.log("💾 Sauvegarde profil vers backend:", this.editedUser);

    const userData = {
      prenom: this.editedUser.prenom,
      nom: this.editedUser.nom,
      email: this.editedUser.email,
    };

    const url = `${this.API_BASE_URL}/api/clients/${this.user.id}`;

    this.http.put<any>(url, userData).subscribe({
      next: (response) => {
        console.log("✅ Profil mis à jour au backend:", response);

        // Mettre à jour l'objet user local
        if (this.user) {
          this.user.prenom = this.editedUser.prenom;
          this.user.nom = this.editedUser.nom;
          this.user.email = this.editedUser.email;
          this.user.name = `${this.editedUser.prenom} ${this.editedUser.nom}`.trim();
        }

        // Sauvegarder dans localStorage
        if (this.user) {
          const updatedUserData = {
            ...this.user,
            id: this.user.id,
            prenom: this.user.prenom,
            nom: this.user.nom,
            email: this.user.email,
          };
          localStorage.setItem("userData", JSON.stringify(updatedUserData));
          console.log("💾 Données utilisateur mises à jour dans localStorage");
        }

        this.isEditingProfile = false;
        this.isSavingProfile = false;
        alert("✅ Profil mis à jour avec succès!");
      },
      error: (error) => {
        console.error("❌ Erreur lors de la mise à jour du profil:", error);
        this.isSavingProfile = false;
        const errorMessage = error.error?.message || error.message || "Erreur serveur";
        alert("❌ Erreur lors de la mise à jour: " + errorMessage);
      },
    });
  }

  cancelEditProfile(): void {
    this.isEditingProfile = false;
    this.editedUser = {};
  }


  getFavoriteCount(produit: Produit): number {
    // ✅ CORRECTION : Utiliser nombreInteractions au lieu de nombreFavoris
    return produit.nombreInteractions || 0;
  }

  getPerformanceScore(produit: any): number {
    // Calcule un score de performance basé sur les interactions
    const views = produit.nombreInteractions || 0;
    const favorites = this.getFavoriteCount(produit);
    const comments = produit.nombreCommentaires || 0;

    const score = Math.min((views * 0.4 + favorites * 0.4 + comments * 0.2) * 10, 100);
    return Math.round(score);
  }

  // Méthode pour contacter l'acheteur
  contactBuyer(produit: Produit): void {
    if (!produit.acheteurNom) {
      alert('Aucun acheteur spécifié pour ce produit');
      return;
    }

    console.log('📧 Contact de l\'acheteur:', produit.acheteurNom);
    // Implémentez la logique de contact ici (ouverture de chat, email, etc.)
    alert(`Fonctionnalité de contact avec ${produit.acheteurNom} bientôt disponible!`);
  }

  openSaleValidationModal(produit: Produit): void {
    console.log('📝 Ouverture modal validation vente pour:', produit);

    if (produit.etat !== 'vendu' && produit.etat !== 'enchere_termine' && produit.etat !== 'vendeur_accepte_vente') {
      console.warn('⚠️ Ce produit n\'est pas en état de vente:', produit.etat);
      alert('Ce produit n\'est pas encore vendu ou l\'enchère n\'est pas terminée.');
      return;
    }

    // ✅ CORRECTION: Initialiser selectedProductForDecision aussi
    this.selectedProductForValidation = produit;
    this.selectedProductForDecision = produit; // ← AJOUTER CETTE LIGNE

    this.saleValidationMessage = '';
    this.isProcessingSaleValidation = false;

    if (produit.acheteurNom) {
      this.saleValidationMessage = `Voulez-vous confirmer la vente à ${produit.acheteurNom} pour ${this.formatCurrency(produit.prixFin || produit.prixDebut)} ?`;
    } else {
      this.saleValidationMessage = `Voulez-vous confirmer la vente pour ${this.formatCurrency(produit.prixFin || produit.prixDebut)} ?`;
    }

    this.showSaleValidationModal = true;
  }

// CORRECTION 2: Modifier closeSaleValidationModal
  closeSaleValidationModal(): void {
    this.showSaleValidationModal = false;
    this.selectedProductForValidation = null;
    this.selectedProductForDecision = null; // ← AJOUTER CETTE LIGNE
    this.saleValidationMessage = '';
    this.isProcessingSaleValidation = false;
  }
// CORRECTION 3: Modifier confirmSale() pour utiliser le bon produit
  confirmSale(): void {
    if (!this.selectedProductForValidation) return;

    this.isProcessingSaleValidation = true;
    console.log('✅ Vendeur confirme la vente:', this.selectedProductForValidation.id);

    // Utiliser selectedProductForValidation au lieu de selectedProductForDecision
    const newStatus = 'vendeur_accepte_vente';

    // ✅ CORRECTION: Passer l'ID du produit correct
    this.updateProductStatus(this.selectedProductForValidation.id, newStatus)
      .then((response) => {
        console.log('✅ État du produit mis à jour:', response);

        // Mettre à jour localement le produit
        this.selectedProductForValidation!.etat = newStatus;

        // Recharger les produits
        if (this.user?.id) {
          this.loadUserProducts(this.user.id);
        }

        this.closeSaleValidationModal();
        alert('✅ Vente confirmée avec succès! Le produit est maintenant marqué comme vendu.');
      })
      .catch((error) => {
        console.error('❌ Erreur lors de la confirmation de la vente:', error);
        this.isProcessingSaleValidation = false;
        alert('❌ Erreur lors de la confirmation de la vente. Veuillez réessayer.');
      });
  }

// CORRECTION 4: Modifier rejectSale() de la même manière
  // Dans profile.component.ts, modifiez la méthode rejectSale()

  rejectSale(): void {
    if (!this.selectedProductForValidation) return;

    this.isProcessingSaleValidation = true;
    console.log('❌ Vendeur refuse la vente:', this.selectedProductForValidation.id);

    const newStatus = 'vendeur_refuse_vente';

    // ✅ CORRECTION: Passer l'ID du produit correct
    this.updateProductStatus(this.selectedProductForValidation.id, newStatus)
      .then((response) => {
        console.log('✅ État du produit mis à jour:', response);

        // Mettre à jour localement le produit
        this.selectedProductForValidation!.etat = newStatus;

        // Recharger les produits
        if (this.user?.id) {
          this.loadUserProducts(this.user.id).then(() => {
            // ✅ NOUVEAU: Rediriger vers l'onglet des produits publiés
            this.setActiveTab('published');

          });
        }

        this.closeSaleValidationModal();
        alert('❌ Vente refusée. Le produit a été déplacé dans vos produits publiés où vous pouvez le relancer en enchère.');
      })
      .catch((error) => {
        console.error('❌ Erreur lors du refus de la vente:', error);
        this.isProcessingSaleValidation = false;
        alert('❌ Erreur lors du refus de la vente. Veuillez réessayer.');
      });
  }

// CORRECTION 5: Modifier updateProductStatus pour accepter l'ID en paramètre
  private updateProductStatus(produitId: number, newStatus: string): Promise<any> {
    return new Promise((resolve, reject) => {
      console.log('🔔 updateProductStatus appelé avec:', {
        produitId: produitId,
        newStatus: newStatus,
        longueur: newStatus.length,
        codeChaqueCaractere: Array.from(newStatus).map(c => c.charCodeAt(0))
      });

      const url = `${this.API_BASE_URL}/api/produits/${produitId}/etat`;
      const request = {
        etat: newStatus
      };

      this.http.put<any>(url, request).subscribe({
        next: (response) => {
          console.log('✅ Statut du produit mis à jour:', response);
          resolve(response);
        },
        error: (error) => {
          console.error('❌ Erreur mise à jour statut produit:', error);
          reject(error);
        }
      });
    });
  }

// ✅ MODIFIER la méthode pour les produits gagnés
  private loadProduitsGagnes(userId: number): void {
    const url = `${this.API_BASE_URL}/api/produits/acheteur/${userId}`;

    console.log('🔄 Chargement des produits gagnés:', url);

    this.http.get<Produit[]>(url).subscribe({
      next: (produits) => {
        console.log('✅ Produits gagnés reçus:', produits);

        // Charger les likes pour les produits gagnés
        this.loadLikesForAcheteur(userId).then(likesMap => {
          // Filtrer pour ne garder que les produits avec état "enchere_termine" ou "vendu"
          this.produitsGagnes = produits
            .filter(p => p.etat === 'enchere_termine' || p.etat === 'vendu' || p.etat === 'vendeur_accepte_vente')
            .map(p => {
              const likeCount = likesMap.get(p.id) || 0;
              return this.ajouterImagePrincipale({
                ...p,
                nombreInteractions: likeCount
              });
            });

          console.log('🏆 Produits gagnés chargés:', this.produitsGagnes.length);

          // Mettre à jour les statistiques
          this.updateUserStats();
        });
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement des produits gagnés:', error);
        this.produitsGagnes = [];
      }
    });
  }

// ✅ NOUVELLE MÉTHODE : Charger les likes pour les produits gagnés
  private loadLikesForAcheteur(acheteurId: number): Promise<Map<number, number>> {
    return new Promise((resolve) => {
      const url = `${this.API_BASE_URL}/api/interactions/acheteur/${acheteurId}/likes-count`;

      console.log('🔄 Chargement des likes pour acheteur:', url);

      this.http.get<{ [key: string]: number }>(url).subscribe({
        next: (likesData) => {
          console.log('✅ Likes pour acheteur reçus:', likesData);

          const likesMap = new Map<number, number>();
          Object.entries(likesData).forEach(([produitId, count]) => {
            likesMap.set(Number(produitId), count);
          });

          resolve(likesMap);
        },
        error: (error) => {
          console.error('❌ Erreur lors du chargement des likes acheteur:', error);
          resolve(new Map<number, number>());
        }
      });
    });
  }


// Fermer le modal de contact
  closeContactModal(): void {
    this.showContactModal = false;
    this.selectedSellerName = '';
    this.selectedSellerPhone = '';
    this.selectedSellerEmail = '';
    this.selectedSellerPhoto = '';
  }



  // Ajouter ces méthodes dans la classe UserProfileComponent

  getSoldStatusClass(etat: string): string {
    const statusClasses: { [key: string]: string } = {
      'vendu': 'sold-badge-completed',
      'enchere_termine': 'sold-badge-pending',
      'vendeur_accepte_vente': 'sold-badge-pending',
      'transaction_annulee': 'sold-badge-cancelled'
    };
    return statusClasses[etat] || 'sold-badge-default';
  }

  getSoldStatusText(etat: string): string {
    const statusTexts: { [key: string]: string } = {
      'vendu': 'Vendu',
      'enchere_termine': 'Terminé',
      'vendeur_accepte_vente': 'Terminé',
      'vendeur_refuse_vente': 'Annulé',
      'transaction_annulee': 'Annulé',
    };
    return statusTexts[etat] || 'Vendu';
  }

  getSaleStatusClass(etat: string): string {
    const statusClasses: { [key: string]: string } = {
      'vendu': 'sale-status-completed',
      'vendeur_accepte_vente': 'sale-status-completed', // Validé
      'vendeur_refuse_vente': 'sale-status-cancelled', // Refusé
      'enchere_termine': 'sale-status-pending',
      'transaction_annulee': 'sale-status-cancelled'
    };
    return statusClasses[etat] || 'sale-status-default';
  }

  getSaleStatusText(etat: string): string {
    const statusTexts: { [key: string]: string } = {
      'vendu': 'Vendu',
      'vendeur_accepte_vente': 'Vente confirmée par vendeur', // Nouveau
      'vendeur_refuse_vente': 'Vente refusée par vendeur',    // Nouveau
      'enchere_termine': 'Transaction en cours',
      'transaction_annulee': 'Transaction annulée'
    };
    return statusTexts[etat] || 'Transaction terminée';
  }

  getAuctionButtonText(produit: Produit): string {
    if (produit.etat === 'transaction_annulee' ||
      produit.etat === 'enchere_termine_sans_gagnant' ||
      produit.etat === 'vendeur_refuse_vente') { // Ajouter ce cas
      return 'Remettre en enchère';
    }
    return 'Mettre en enchère';
  }


  hasBeenInAuction(produit: Produit): boolean {
    // Vérifie si le produit a déjà été en enchère (transaction annulée)
    return produit.etat === 'transaction_annulee' || produit.etat === 'enchere_termine_sans_gagnant' || !!produit.dateenchere;
  }

  getProductStatusIndicator(produit: Produit): string {
    switch (produit.etat) {
      case 'transaction_annulee':
        return 'status-cancelled';
      case 'enchere_termine_sans_gagnant': // ← AJOUTER CE CAS
        return 'status-republished';
      case 'accepter':
      case 'accepte':
      case 'expertise_validee':
        return 'status-approved';
      default:
        return 'status-default';
    }
  }

  getProductStatusText1(produit: Produit): string {
    switch (produit.etat) {
      case 'transaction_annulee':
        return 'Transaction annulée';
      case 'vendeur_refuse_vente':
        return 'Transaction annulée';
      case 'accepter':
      case 'accepte':
        return 'Publié';
      case 'expertise_validee':
        return 'Expertisé';
      case 'enchere_termine_sans_gagnant' : // ← AJOUTER CE CAS
        return 'Republié';
      default:
        return 'Publié';
    }
  }

  // Méthode pour contacter le vendeur (version simplifiée - seulement afficher les coordonnées)
  contactSeller(produit: Produit): void {
    console.log('📧 Contact du vendeur:', produit.vendeurNom);

    // Initialiser les informations du vendeur
    this.selectedSellerName = produit.vendeurNom || 'Vendeur';
    this.selectedSellerPhone = '';
    this.selectedSellerEmail = '';
    this.selectedSellerPhoto = 'assets/images/default-avatar.jpg';

    // Récupérer les informations détaillées du vendeur si disponible
    if (produit.vendeurId) {
      this.loadSellerDetails(produit.vendeurId);
    } else {
      // Si pas de vendeurId, utiliser les informations de base du produit
      if (produit.vendeurPhone) {
        this.selectedSellerPhone = produit.vendeurPhone;
      }
      if (produit.vendeurEmail) {
        this.selectedSellerEmail = produit.vendeurEmail;
      }
    }

    // Afficher le modal de contact
    this.showContactModal = true;
  }

// Méthode pour charger les détails du vendeur (garder cette méthode)
  private loadSellerDetails(vendeurId: number): void {
    const url = `${this.API_BASE_URL}/api/clients/${vendeurId}`;

    this.http.get<any>(url).subscribe({
      next: (seller) => {
        console.log('✅ Détails du vendeur:', seller);
        this.selectedSellerPhone = seller.telephone || seller.phone || '';
        this.selectedSellerEmail = seller.email || '';

        // Photo de profil
        if (seller.photoProfil) {
          this.selectedSellerPhoto = seller.photoProfil.startsWith('http')
            ? seller.photoProfil
            : `${this.API_BASE_URL}/api/clients/images/${seller.photoProfil}`;
        }
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement des détails du vendeur:', error);
      }
    });
  }

}
