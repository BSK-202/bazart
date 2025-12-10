import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';
import { Enchere, EnchereService } from '../../../services/enchere.service';
import {FundsReservationService} from '../../../services/funds-reservation.service';

// Services

interface Bid {
  bidder: string;
  amount: number;
  time: string;
  isLeading: boolean;
}

interface Seller {
  idClient: number;
  nom: string;
  prenom: string;
  email: string;
  tel: string;
  ville: string;
  pays: string;
  dateInscription: string;
  photoProfil: string;
  enabled: boolean;
}

interface Produit {
  id: number;
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number | null;
  etat: string;
  vendeurNom: string;
  acheteurNom: string | null;
  idClientAcheteur?: number; // ✅ NOUVEAU
  categorieNom: string;
  nombreInteractions: number;
  nombreCommentaires: number;
  images: string[];
  vendeurId: number;
  datePublication?: string; // camelCase
  datepublication?: string; // fallback en minuscules
  aExpertise: boolean;
  dateenchere?: string;
  dureeEnchereJours?: number;
  expertisePublicComment?: string;
  expertiseAuthenticityLevel?: string;
  expertiseProductCondition?: string;
  expertiseApproved?: boolean;
  expertiseRequestId?: number;
}

@Component({
  selector: 'app-product-detail',
  templateUrl: './Product-detail.component.html',
  styleUrls: ['./Product-detail.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule]
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  produitId: string = '';
  bidAmount: string = '';
  timeLeft: string = '';
  walletBalance: number = 0;
  selectedImage: number = 0;
  private timer: any;
  showDefaultAvatar: boolean = false;
  // Variables pour les données dynamiques
  produit: Produit | null = null;
  vendeur: Seller | null = null;
  isLoading: boolean = true;
  error: string = '';

  enchereActuelle: number = 0;
  nombreEncheres: number = 0;
  historiqueEncheres: Enchere[] = [];
  isLoadingEncheres: boolean = false;

  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private authService: AuthService,
    private enchereService: EnchereService,
    private fundsReservationService: FundsReservationService
    // Nouveau service
  ) {}

  ngOnInit(): void {
    this.produitId = this.route.snapshot.paramMap.get('id') || '';
    console.log('🆔 ID Produit:', this.produitId);

    if (this.produitId) {
      this.loadProduitDetails();
      this.loadDonneesEnchere();
    } else {
      this.error = 'ID produit non valide';
      this.isLoading = false;
    }

    this.startTimer();
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  // Charger les données d'enchère
  loadDonneesEnchere(): void {
    if (!this.produitId) return;

    const produitIdNum = parseInt(this.produitId);
    // Charger l'enchère actuelle
    this.enchereService.getEnchereActuelle(produitIdNum).subscribe({
      next: (response) => {
        this.enchereActuelle = response.montantActuel;
        console.log('💰 Enchère actuelle:', this.enchereActuelle);
      },
      error: (err) => {
        console.error('❌ Erreur chargement enchère actuelle:', err);
        this.enchereActuelle = this.produit?.prixDebut || 0;
      }
    });

    // Charger le nombre d'enchères
    this.enchereService.getNombreEncheres(produitIdNum).subscribe({
      next: (response) => {
        this.nombreEncheres = response.count;
        console.log('🔢 Nombre d\'enchères:', this.nombreEncheres);
      },
      error: (err) => {
        console.error('❌ Erreur chargement nombre enchères:', err);
      }
    });

    // Charger l'historique des enchères
    this.loadHistoriqueEncheres();
  }

  // Charger l'historique des enchères
  loadHistoriqueEncheres(): void {
    if (!this.produitId) return;

    this.isLoadingEncheres = true;
    const produitIdNum = parseInt(this.produitId);

    this.enchereService.getHistoriqueEncheres(produitIdNum).subscribe({
      next: (historique) => {
        this.historiqueEncheres = historique.map(enchere => ({
          ...enchere,
          bidder: `${enchere.encherisseurPrenom} ${enchere.encherisseurNom}`,
          amount: enchere.montant,
          time: this.formatEnchereTime(enchere.dateEnchere),
          isLeading: enchere.isLeading || false
        }));
        console.log('📊 Historique des enchères:', this.historiqueEncheres);
        this.isLoadingEncheres = false;
      },
      error: (err) => {
        console.error('❌ Erreur chargement historique enchères:', err);
        this.isLoadingEncheres = false;
      }
    });
  }

  // URL de téléchargement du rapport (ajoutez ce getter)
  get reportDownloadUrl(): string | null {
    if (!this.produit?.expertiseRequestId) return null;
    // Adapte l’endpoint si nécessaire
    return `${this.API_BASE_URL}/api/expertise/requests/${this.produit.expertiseRequestId}/report-pdf`;
  }

  // Formater le temps de l'enchère
  private formatEnchereTime(dateString: string): string {
    try {
      const dateEnchere = new Date(dateString);
      const maintenant = new Date();
      const difference = maintenant.getTime() - dateEnchere.getTime();

      const minutes = Math.floor(difference / (1000 * 60));
      const heures = Math.floor(difference / (1000 * 60 * 60));
      const jours = Math.floor(difference / (1000 * 60 * 60 * 24));

      if (minutes < 1) return 'À l\'instant';
      if (minutes < 60) return `Il y a ${minutes} minute${minutes > 1 ? 's' : ''}`;
      if (heures < 24) return `Il y a ${heures} heure${heures > 1 ? 's' : ''}`;
      return `Il y a ${jours} jour${jours > 1 ? 's' : ''}`;
    } catch (e) {
      return 'Date inconnue';
    }
  }

  loadProduitDetails(): void {
    this.isLoading = true;
    this.resetProfileImageState();
    const url = `${this.API_BASE_URL}/api/produits/${this.produitId}`;

    this.http.get<any>(url).subscribe({ // ✅ Utiliser 'any' pour debug
      next: (produitData) => {
        console.log('✅ Produit chargé COMPLET:', produitData);
        console.log('🔍 Toutes les propriétés:', Object.keys(produitData));

        // Debug: chercher la propriété date
        for (const key in produitData) {
          if (key.toLowerCase().includes('date')) {
            console.log(`📅 Propriété date trouvée: ${key} =`, produitData[key]);
          }
        }

        this.produit = produitData as Produit;

        // Debug spécifique pour la date
        console.log('📅 datePublication:', this.produit.datePublication);
        console.log('📅 datepublication:', (this.produit as any).datepublication);

        // Charger les informations du vendeur
        this.loadVendeurInfo(this.extractVendeurId(produitData));

        // Construire les URLs complètes des images
        if (this.produit.images && this.produit.images.length > 0) {
          this.produit.images = this.produit.images.map(imageName =>
            this.getProduitImageUrl(this.produit!.id, imageName)
          );
        }

        this.isLoading = false;
      },
      error: (err) => {
        console.error('❌ Erreur chargement produit:', err);
        this.error = 'Erreur lors du chargement du produit';
        this.isLoading = false;
      }
    });
  }

  // Dans ProductDetailComponent
  formatDate(dateString: string | undefined): string {
    if (!dateString) return '';

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
      console.error('Error formatting date:', e);
      return dateString;
    }
  }

  // 🔍 Extraire l'ID du vendeur du produit
  private extractVendeurId(produit: Produit): number {
    // Si vous avez directement l'ID du vendeur dans le produit
    if ((produit as any).vendeurId) {
      return (produit as any).vendeurId;
    }

    // Sinon, vous devrez peut-être modifier votre backend pour inclure l'ID du vendeur
    // Pour l'instant, on retourne un ID par défaut (à adapter selon vos besoins)
    console.warn('⚠️ ID vendeur non trouvé dans le produit, utilisation de l\'ID 1 par défaut');
    return 1;
  }

  // 🔄 Charger les informations du vendeur
  loadVendeurInfo(vendeurId: number): void {
    const url = `${this.API_BASE_URL}/api/clients/${vendeurId}`;

    this.http.get<Seller>(url).subscribe({
      next: (vendeurData) => {
        console.log('✅ Vendeur chargé:', vendeurData);
        this.vendeur = vendeurData;
      },
      error: (err) => {
        console.error('❌ Erreur chargement vendeur:', err);
        // Vendeur par défaut en cas d'erreur
        this.vendeur = this.createDefaultVendeur(vendeurId);
      }
    });
  }

  // 🛠️ Créer un vendeur par défaut en cas d'erreur
  private createDefaultVendeur(vendeurId: number): Seller {
    return {
      idClient: vendeurId,
      nom: this.produit?.vendeurNom || 'Vendeur',
      prenom: '',
      email: '',
      tel: '',
      ville: '',
      pays: '',
      dateInscription: new Date().toISOString(),
      photoProfil: '',
      enabled: true
    };
  }

  // 🖼️ Construire l'URL complète de l'image du produit
  getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === '') {
      return 'assets/images/placeholder.jpg';
    }

    if (imageName.startsWith('http') || imageName.startsWith('/api/')) {
      return `${this.API_BASE_URL}${imageName}`;
    }

    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  getProfileImageUrl(userId: number): string {
    console.log('🖼️ Chargement image profil vendeur ID:', userId);
    this.showDefaultAvatar = false; // Réinitialiser à chaque appel
    return `${this.API_BASE_URL}/api/clients/images/${userId}.jpg`;
  }

  // Gérer l'erreur de chargement de l'image de profil
  onProfileImageError(event: any) {
    console.log('❌ Profile image not found, using default avatar');
    this.showDefaultAvatar = true;

    // Optionnel: cacher l'image défectueuse
    const imgElement = event.target as HTMLImageElement;
    imgElement.style.display = 'none';
  }

  private resetProfileImageState() {
    this.showDefaultAvatar = false;
  }

  get minBid(): number {
    if (!this.isEnchereActive) {
      return this.produit?.prixDebut || 0;
    }

    // Utiliser l'enchère actuelle + incrément minimum
    return this.enchereActuelle + (this.auction.minIncrement || 50);
  }

  get isAuthenticated(): boolean {
    return this.authService.isLoggedIn();
  }

  private startTimer(): void {
    this.timer = setInterval(() => {
      this.updateTimeLeft();
    }, 1000);
  }

  // Modifier la méthode updateTimeLeft pour mettre à jour l'état
  private updateTimeLeft(): void {
    if (!this.isEnchereActive || !this.auctionEndTime) {
      this.timeLeft = 'Enchère non active';
      return;
    }

    const now = new Date().getTime();
    const distance = this.auctionEndTime.getTime() - now;

    if (distance < 0) {
      this.timeLeft = 'Enchère terminée';
      this.handleAuctionEnd();
      return;
    }

    const days = Math.floor(distance / (1000 * 60 * 60 * 24));
    const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((distance % (1000 * 60)) / 1000);

    this.timeLeft = `${days}j ${hours}h ${minutes}m ${seconds}s`;
  }

  selectImage(index: number): void {
    this.selectedImage = index;
  }

  get auctionWinner(): string | null {
    if (!this.isEnchereTerminee || this.historiqueEncheres.length === 0) {
      return null;
    }

    // Trouver l'enchère la plus élevée avec vérification de type
    const winningBid = this.historiqueEncheres.reduce((prev, current) => {
      // Vérifier que les montants existent
      const prevAmount = prev.amount || 0;
      const currentAmount = current.amount || 0;

      return prevAmount > currentAmount ? prev : current;
    });

    return winningBid.bidder || null;
  }

  private updateProductStateInDatabase(idGagnant: number | null): void {
    if (!this.produitId) return;

    const url = `${this.API_BASE_URL}/api/produits/${this.produitId}/terminer-enchere`;

    const body = idGagnant ? { idGagnant } : {};

    this.http.post(url, body).subscribe({
      next: (response: any) => {
        console.log('✅ Enchère marquée comme terminée:', response);
        if (this.produit) {
          this.produit.etat = 'enchere_termine';
          // @ts-ignore
          this.produit.idClientAcheteur = idGagnant; // Mettre à jour localement si nécessaire
        }
      },
      error: (err) => {
        console.error('❌ Erreur:', err);
      }
    });
  }

  private handleAuctionEnd(): void {
    console.log('🏁 Enchère terminée pour le produit:', this.produit?.id);

    // Déterminer le gagnant
    let idGagnant: number | null = null;

    if (this.historiqueEncheres.length > 0) {
      // Trouver l'enchère avec le montant le plus élevé
      const highestBid = this.historiqueEncheres.reduce((prev, current) => {
        const prevAmount = prev.amount || 0;
        const currentAmount = current.amount || 0;
        return (prevAmount > currentAmount) ? prev : current;
      });

      if (highestBid.encherisseurId) {
        idGagnant = highestBid.encherisseurId;
        console.log('🏆 Gagnant identifié:', idGagnant);
      }
    }

    // Mettre à jour l'état local du produit
    if (this.produit && this.produit.etat === 'en_enchere') {
      this.produit.etat = 'enchere_termine';
      console.log('✅ État du produit mis à jour: enchere_termine');
    }

    // Appeler l'API pour mettre à jour l'état en base de données avec le gagnant
    this.updateProductStateInDatabase(idGagnant);

    clearInterval(this.timer);
  }



  async handleBid(): Promise<void> {
    if (!this.isAuthenticated) {
      this.router.navigate(['/connexion']);
      return;
    }

    const amount = parseFloat(this.bidAmount);
    const produitIdNum = parseInt(this.produitId);
    const nouveauClientId = this.authService.getCurrentUserId(); // 🔄 Renommage pour clarté

    // Validations de base
    if (!this.bidAmount || isNaN(amount)) {
      alert('Veuillez entrer un montant valide');
      return;
    }

    if (amount < this.minBid) {
      alert(`L'enchère doit être d'au moins ${this.formatPrice(this.minBid)}`);
      return;
    }

    if (!nouveauClientId) {
      alert('Erreur: Utilisateur non identifié');
      return;
    }

    console.log('🎯 Début processus enchère:', {
      produitId: produitIdNum,
      nouveauClientId,
      montant: amount
    });

    try {
      // ✅ ÉTAPE 1: Identifier l'ancien leader AVANT tout débit
      const ancienLeaderInfo = await this.identifierAncienLeader(produitIdNum);

      // ✅ ÉTAPE 2: DÉBITER le nouveau client
      console.log('💰 Débit du nouveau client...');
      const debitResult = await this.debiterClient(nouveauClientId, amount, produitIdNum);

      if (!debitResult.success) {
        alert(`❌ ${debitResult.message}`);
        return;
      }

      console.log('✅ Débit effectué, nouveau solde:', debitResult.newBalance);

      // ✅ ÉTAPE 3: REMBOURSER l'ancien leader (SI IL EXISTE)
      if (ancienLeaderInfo && ancienLeaderInfo.ancienLeaderId !== nouveauClientId) {
        console.log('🔄 Remboursement ancien leader:', ancienLeaderInfo.ancienLeaderId, 'Montant:', ancienLeaderInfo.montant);

        // 🔥 CORRECTION ICI : Utiliser l'ID de l'ancien leader, pas du nouveau client
        await this.rembourserAncienLeader(
          ancienLeaderInfo.ancienLeaderId, // ✅ ID de l'ancien leader
          ancienLeaderInfo.montant,        // ✅ Montant à rembourser
          produitIdNum
        );
      }

      // ✅ ÉTAPE 4: PLACER l'enchère
      console.log('📤 Placement de l\'enchère...');
      this.enchereService.placerEnchere(produitIdNum, nouveauClientId, amount).subscribe({
        next: (enchere) => {
          console.log('✅ Enchère placée avec succès:', enchere);
          alert(`✅ Enchère de ${this.formatPrice(amount)} placée avec succès! Votre nouveau solde: ${this.formatPrice(debitResult.newBalance)}`);
          this.bidAmount = '';

          // Recharger les données
          this.loadDonneesEnchere();
        },
        error: (err) => {
          console.error('❌ Erreur placement enchère:', err);

          // ⚠️ EN CAS D'ERREUR: Rembourser le nouveau client
          this.rembourserClient(nouveauClientId, amount, produitIdNum);

          const errorMessage = err.error?.error || 'Erreur lors du placement de l\'enchère';
          alert(`❌ Erreur: ${errorMessage}`);
        }
      });

    } catch (error) {
      console.error('❌ Erreur processus enchère:', error);
      alert('❌ Erreur lors du processus d\'enchère');
    }
  }

  // Product-detail.component.ts - AJOUTER
  private async identifierAncienLeader(produitId: number):
    Promise<{ancienLeaderId: number, montant: number} | null> {

    return new Promise((resolve) => {
      // Si on a déjà des enchères dans l'historique, trouver l'ancien leader
      if (this.historiqueEncheres.length > 0) {

        // Trier par montant décroissant pour trouver le leader actuel
        const encheresTriees = [...this.historiqueEncheres]
          .sort((a, b) => (b.amount || 0) - (a.amount || 0));

        const ancienLeader = encheresTriees[0];

        if (ancienLeader && ancienLeader.encherisseurId && ancienLeader.amount) {
          console.log('👤 Ancien leader identifié:', ancienLeader.encherisseurId, 'Montant:', ancienLeader.amount);
          resolve({
            ancienLeaderId: ancienLeader.encherisseurId,
            montant: ancienLeader.amount
          });
          return;
        }
      }

      // Si pas d'ancien leader identifié
      console.log('ℹ️ Aucun ancien leader à libérer');
      resolve(null);
    });
  }

  // Méthode pour débiter le client
  private async debiterClient(clientId: number, montant: number, produitId: number):
    Promise<{success: boolean; message: string; newBalance?: number}> {

    return new Promise((resolve) => {
      this.fundsReservationService.debiterEnchere(clientId, montant, produitId)
        .subscribe({
          next: (response) => {
            if (response.success) {
              resolve({
                success: true,
                message: 'Débit effectué',
                newBalance: response.newBalance
              });
            } else {
              resolve({
                success: false,
                message: response.message || 'Erreur de débit'
              });
            }
          },
          error: (error) => {
            console.error('❌ Erreur service débit:', error);
            resolve({
              success: false,
              message: 'Erreur de connexion au service de débit'
            });
          }
        });
    });
  }

// ✅ CORRECTION : Méthode renommée pour plus de clarté
  private async rembourserAncienLeader(ancienLeaderId: number, montant: number, produitId: number): Promise<void> {
    return new Promise((resolve) => {
      console.log(`🔄 Remboursement ANCIEN leader ${ancienLeaderId}, montant: ${montant}`);

      // 🔥 CORRECTION : Bien utiliser l'ID de l'ancien leader
      this.fundsReservationService.rembourserEnchere(ancienLeaderId, montant, produitId)
        .subscribe({
          next: (response) => {
            if (response.success) {
              console.log('✅ Ancien leader remboursé, nouveau solde:', response.newBalance);
            } else {
              console.error('❌ Erreur remboursement ancien leader:', response.message);
            }
            resolve();
          },
          error: (err) => {
            console.error('❌ Erreur technique remboursement ancien leader:', err);
            resolve();
          }
        });
    });
  }

  // ✅ CORRECTION : Méthode pour rembourser en cas d'erreur
  private rembourserClient(clientIdARembourser: number, montant: number, produitId: number): void {
    console.log(`🔄 Remboursement compensation client ${clientIdARembourser}, montant: ${montant}`);

    this.fundsReservationService.rembourserEnchere(clientIdARembourser, montant, produitId)
      .subscribe({
        next: (response) => {
          if (response.success) {
            console.log('✅ Client remboursé en cas d\'erreur');
          } else {
            console.error('❌ Erreur remboursement compensation:', response.message);
          }
        },
        error: (err) => {
          console.error('❌ Erreur technique remboursement compensation:', err);
        }
      });
  }

  handleAddToFavorites(): void {
    if (!this.isAuthenticated) {
      this.router.navigate(['/connexion']);
      return;
    }
    alert('Ajouté aux favoris!');
  }

  onBidAmountChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value;
    const numValue = parseFloat(value);

    if (value === '' || !isNaN(numValue)) {
      this.bidAmount = value;
    }
  }

  formatPrice(amount: number | undefined): string {
    if (!amount) return '0 DH';
    return amount.toLocaleString('fr-MA') + ' DH';
  }

  // Mettre à jour getInitials pour être plus robuste
  getInitials(name: string | undefined): string {
    if (!name) return '??';

    const parts = name.split(' ');
    return (parts[0][0] + (parts[1]?.[0] || '')).toUpperCase();
  }

  // 🔄 Formater la date d'inscription
  formatJoinDate(dateString: string | undefined): string {
    if (!dateString) return 'Membre depuis 2020';

    try {
      const date = new Date(dateString);
      const year = date.getFullYear();
      const currentYear = new Date().getFullYear();
      const yearsAgo = currentYear - year;

      if (yearsAgo === 0) {
        return 'Nouveau membre';
      } else if (yearsAgo === 1) {
        return 'Membre depuis 1 an';
      } else {
        return `Membre depuis ${yearsAgo} ans`;
      }
    } catch (error) {
      console.error('Error formatting join date:', error);
      return 'Membre depuis 2020';
    }
  }

  formatPublicationDate(produit: Produit): string {
    console.log('📅 FormatPublicationDate appelée avec produit:', produit);

    // Chercher la date dans différentes propriétés possibles
    let dateString: string | undefined;

    // Priorité 1: datePublication (camelCase)
    if (produit.datePublication) {
      dateString = produit.datePublication;
      console.log('📅 Utilisation datePublication (camelCase):', dateString);
    }
    // Priorité 2: datepublication (minuscules)
    else if ((produit as any).datepublication) {
      dateString = (produit as any).datepublication;
      console.log('📅 Utilisation datepublication (minuscules):', dateString);
    }

    if (!dateString) {
      console.log('❌ Aucune propriété de date trouvée');
      return 'Date inconnue';
    }

    // Logique de formatage existante
    try {
      let date: Date;

      if (dateString.includes('T')) {
        date = new Date(dateString);
      } else if (dateString.includes(' ')) {
        date = new Date(dateString.replace(' ', 'T'));
      } else {
        date = new Date(dateString + 'T00:00:00');
      }

      if (isNaN(date.getTime())) {
        console.log('❌ Date invalide après parsing:', dateString);
        return 'Date inconnue';
      }

      console.log('✅ Date parsée avec succès:', date);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });

    } catch (error) {
      console.error('❌ Erreur lors du formatage de la date:', error);
      return 'Date inconnue';
    }
  }

  // ⭐ Calculer la note du vendeur (temporairement statique)
  getSellerRating(): number {
    return 4.8; // À remplacer par un vrai calcul
  }

  // 📈 Calculer le nombre de ventes (temporairement statique)
  getSellerSales(): number {
    return 156; // À remplacer par un vrai calcul
  }

  protected readonly parseFloat = parseFloat;

  // Mettre à jour isEnchereActive
  get isEnchereActive(): boolean {
    return this.produit?.etat === 'en_enchere';
  }

  get auctionEndTime(): Date | null {
    if (!this.produit?.dateenchere || !this.produit.dureeEnchereJours) {
      return null;
    }

    try {
      const startDate = new Date(this.produit.dateenchere);
      // Ajouter la durée en jours
      const endDate = new Date(startDate);
      endDate.setDate(endDate.getDate() + this.produit.dureeEnchereJours);
      return endDate;
    } catch (e) {
      console.error('Erreur calcul date fin enchère:', e);
      return null;
    }
  }

  get auction() {
    return {
      currentBid: this.enchereActuelle,
      minIncrement: 50, // Vous pouvez le rendre dynamique aussi
      startingBid: this.produit?.prixDebut || 0,
      bids: this.nombreEncheres,
      endTime: this.auctionEndTime || new Date(),
      bidHistory: this.historiqueEncheres
    };
  }

  // 🖼️ Construire l'URL complète de l'image de profil des enchérisseurs
  getBidderProfileImageUrl(bidderId: number): string {
    console.log('🖼️ Chargement image profil enchérisseur ID:', bidderId);
    return `${this.API_BASE_URL}/api/clients/images/${bidderId}.jpg`;
  }

  // Gérer l'erreur de chargement de l'image de profil des enchérisseurs
  onBidderProfileImageError(event: any, bid: any) {
    console.log(`❌ Profile image not found for bidder ${bid.encherisseurId}, using default avatar`);

    // Ajouter une propriété pour afficher l'avatar par défaut
    bid.showDefaultAvatar = true;

    // Optionnel: cacher l'image défectueuse
    const imgElement = event.target as HTMLImageElement;
    imgElement.style.display = 'none';
  }

  // Méthode pour vérifier le solde du wallet (sans débiter)
  private checkWalletBalance(amount: number): Promise<boolean> {
    return new Promise((resolve, reject) => {
      const url = 'http://localhost:8080/api/wallet/balance';

      this.http.get<any>(url).subscribe({
        next: (response) => {
          if (response.success) {
            const walletBalance = response.balance;
            console.log('💰 Solde du wallet:', walletBalance, 'Montant requis:', amount);

            // Vérifier seulement si le solde est suffisant
            if (walletBalance >= amount) {
              resolve(true);
            } else {
              resolve(false);
            }
          } else {
            console.error('❌ Erreur lors du chargement du solde:', response.error);
            reject(new Error('Erreur lors de la vérification du solde'));
          }
        },
        error: (error) => {
          console.error('❌ Erreur API wallet:', error);
          reject(new Error('Erreur de connexion au wallet'));
        }
      });
    });
  }

  // Vérifier si l'utilisateur connecté est le vendeur du produit
  isCurrentUserSeller(): boolean {
    if (!this.isAuthenticated || !this.produit || !this.vendeur) {
      return false;
    }

    const currentUserId = this.authService.getCurrentUserId();

    // Comparer l'ID de l'utilisateur connecté avec l'ID du vendeur
    if (currentUserId && this.vendeur.idClient === currentUserId) {
      console.log('👤 Utilisateur connecté est le vendeur du produit');
      return true;
    }

    return false;
  }

  // Mettre à jour isEnchereTerminee pour être plus précis
  get isEnchereTerminee(): boolean {
    // Vérifier d'abord l'état du produit
    if (this.produit?.etat === 'enchere_termine') {
      return true;
    }

    // Si le produit n'est pas en enchère, alors l'enchère n'est pas terminée
    if (!this.isEnchereActive) {
      return false;
    }

    // Vérifier la date de fin
    const endTime = this.auctionEndTime;
    if (!endTime) {
      return false;
    }

    return new Date().getTime() > endTime.getTime();
  }

  // Vérifier si l'utilisateur connecté est en tête de l'enchère
  isCurrentUserLeading(): boolean {
    if (!this.isAuthenticated || this.historiqueEncheres.length === 0) {
      return false;
    }

    const currentUserId = this.authService.getCurrentUserId();
    if (!currentUserId) return false;

    // Trouver l'enchère avec le montant le plus élevé avec vérification de sécurité
    const highestBid = this.historiqueEncheres.reduce((prev, current) => {
      const prevAmount = prev.amount || 0;
      const currentAmount = current.amount || 0;
      return (prevAmount > currentAmount) ? prev : current;
    });

    // Vérifier si l'utilisateur courant est l'auteur de l'enchère la plus élevée
    if (highestBid.encherisseurId === currentUserId) {
      console.log('👑 Utilisateur connecté est en tête de l\'enchère avec', highestBid.amount);
      return true;
    }

    return false;
  }

  // Vérifier si l'utilisateur peut enchérir
  canUserBid(): boolean {
    if (!this.isAuthenticated) return false;
    if (this.isCurrentUserSeller()) return false;
    if (this.isCurrentUserLeading()) return false;
    if (this.isEnchereTerminee) return false;

    return true;
  }
}
