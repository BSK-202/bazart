import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

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
  inAppEnabled: boolean = true;
  emailEnabled: boolean = false;

  // Remplacez les données mockées par les vraies données
  produitsEncheres: Produit[] = [];
  produitsVendus: Produit[] = [];
  produitsPublies: Produit[] = [];
  produitsEnAttente: Produit[] = [];

  private readonly API_BASE_URL = 'http://localhost:8080';
  protected isAuctionStarting: boolean | undefined;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

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
}
