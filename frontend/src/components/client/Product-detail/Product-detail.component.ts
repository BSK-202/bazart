import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import {AuthService} from '../../../services/auth.service';

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

  // Données temporaires pour la démo (à remplacer par les vraies données)
  auction = {
    currentBid: 2400,
    minIncrement: 50,
    startingBid: 1500,
    bids: 12,
    endTime: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000),
    bidHistory: [
      { bidder: 'Mohammed K.', amount: 2400, time: 'Il y a 15 minutes', isLeading: true },
      { bidder: 'Fatima B.', amount: 2350, time: 'Il y a 1 heure', isLeading: false },
      { bidder: 'Ahmed M.', amount: 2300, time: 'Il y a 2 heures', isLeading: false },
      { bidder: 'Yasmine L.', amount: 2200, time: 'Il y a 4 heures', isLeading: false },
    ]
  };

  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.produitId = this.route.snapshot.paramMap.get('id') || '';
    console.log('🆔 ID Produit:', this.produitId);

    if (this.produitId) {
      this.loadProduitDetails();
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
  // Méthodes existantes avec adaptations
  get minBid(): number {
    return this.auction.currentBid + this.auction.minIncrement;
  }

  get isAuthenticated(): boolean {
    return this.authService.isLoggedIn();
  }

  private startTimer(): void {
    this.timer = setInterval(() => {
      const now = new Date().getTime();
      const distance = this.auction.endTime.getTime() - now;

      if (distance < 0) {
        this.timeLeft = 'Enchère terminée';
        clearInterval(this.timer);
        return;
      }

      const days = Math.floor(distance / (1000 * 60 * 60 * 24));
      const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((distance % (1000 * 60)) / 1000);

      this.timeLeft = `${days}j ${hours}h ${minutes}m ${seconds}s`;
    }, 1000);
  }

  selectImage(index: number): void {
    this.selectedImage = index;
  }

  handleBid(): void {
    if (!this.isAuthenticated) {
      this.router.navigate(['/connexion']);
      return;
    }

    const amount = parseFloat(this.bidAmount);

    if (!this.bidAmount || isNaN(amount)) {
      alert('Veuillez entrer un montant valide');
      return;
    }

    if (amount < this.minBid) {
      alert(`L'enchère doit être d'au moins ${this.minBid.toLocaleString('fr-MA')} DH`);
      return;
    }

    console.log('[v0] Placing bid:', this.bidAmount);
    alert('Enchère placée avec succès!');
    this.bidAmount = '';
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

  formatPrice(amount: number): string {
    return amount.toLocaleString('fr-MA') + ' DH';
  }

  getInitials(name: string): string {
    const parts = name.split(' ');
    return (parts[0][0] + (parts[1]?.[0] || '')).toUpperCase();
  }

  // 🔄 Formater la date d'inscription
  formatJoinDate(dateString: string): string {
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

// 🔄 Formater la date de publication - VERSION ROBUSTE
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
}
