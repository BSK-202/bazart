import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import {AuthService} from '../../../services/auth.service';
import {Enchere, EnchereService} from '../../../services/enchere.service';

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
    private enchereService: EnchereService // Nouveau service
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

  private updateTimeLeft(): void {
    if (!this.isEnchereActive || !this.auctionEndTime) {
      this.timeLeft = 'Enchère non active';
      return;
    }

    const now = new Date().getTime();
    const distance = this.auctionEndTime.getTime() - now;

    if (distance < 0) {
      this.timeLeft = 'Enchère terminée';
      // Optionnel: Mettre à jour l'état du produit
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

  private handleAuctionEnd(): void {
    console.log('🏁 Enchère terminée pour le produit:', this.produit?.id);
    // Ici vous pouvez appeler une API pour mettre à jour l'état du produit
    clearInterval(this.timer);
  }

  handleBid(): void {
    if (!this.isAuthenticated) {
      this.router.navigate(['/connexion']);
      return;
    }

    const amount = parseFloat(this.bidAmount);
    const produitIdNum = parseInt(this.produitId);

    if (!this.bidAmount || isNaN(amount)) {
      alert('Veuillez entrer un montant valide');
      return;
    }

    if (amount < this.minBid) {
      alert(`L'enchère doit être d'au moins ${this.formatPrice(this.minBid)}`);
      return;
    }

    // Récupérer l'ID du client connecté
    const clientId = this.authService.getCurrentUserId();
    if (!clientId) {
      alert('Erreur: Utilisateur non identifié');
      return;
    }

    console.log('🎯 Placement enchère:', { produitId: produitIdNum, clientId, montant: amount });

    this.enchereService.placerEnchere(produitIdNum, clientId, amount).subscribe({
      next: (enchere) => {
        console.log('✅ Enchère placée avec succès:', enchere);
        alert('Enchère placée avec succès!');
        this.bidAmount = '';

        // Recharger les données d'enchère
        this.loadDonneesEnchere();
      },
      error: (err) => {
        console.error('❌ Erreur placement enchère:', err);
        const errorMessage = err.error?.error || 'Erreur lors du placement de l\'enchère';
        alert(`Erreur: ${errorMessage}`);
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
    // @ts-ignore
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

  get isEnchereActive(): boolean {
    return this.produit?.etat === 'en_enchere';
  }

  get auctionEndTime(): Date | null {
    if (!this.produit?.dateenchere || !this.produit.dureeEnchereJours) {
      return null;
    }

    try {
      const startDate = new Date(this.produit.dateenchere);
      const endDate = new Date(startDate.getTime() + this.produit.dureeEnchereJours * 24 * 60 * 60 * 1000);
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
}
