import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface ProduitAValider {
  id: number; // CHANGÉ DE idproduit À id
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number;
  etat: string;
  datePublication: string;

  // Informations vendeur
  vendeurId: number;
  vendeurNom: string;
  vendeurEmail: string;
  vendeurTelephone: string;

  // Informations acheteur
  acheteurId: number;
  acheteurNom: string;
  acheteurEmail: string;
  acheteurTelephone: string;

  // Images
  images: string[];

  // Catégorie
  categorieNom: string;
}

@Component({
  selector: 'app-validation-achat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validation-achat.component.html',
  styleUrls: ['./validation-achat.component.css']
})
export class ValidationAchatComponent implements OnInit {
  produits: ProduitAValider[] = [];
  produitsFiltres: ProduitAValider[] = [];
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  // Filtres
  searchTerm = '';
  selectedVendeur = '';
  selectedAcheteur = '';

  // Liste unique pour les filtres
  vendeurs: string[] = [];
  acheteurs: string[] = [];

  private readonly API_BASE_URL = 'http://localhost:8080/api';
  private readonly placeholderImage = 'assets/images/placeholder.jpg'; // AJOUTÉ

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadProduitsAValider();
  }

  loadProduitsAValider() {
    this.isLoading = true;
    this.errorMessage = '';

    this.http.get<ProduitAValider[]>(`${this.API_BASE_URL}/produits/etat/vendeur_accepte_vente`)
      .subscribe({
        next: (produits) => {
          this.produits = produits;
          this.produitsFiltres = [...produits];
          this.extractFilterLists();
          this.isLoading = false;
          console.log('Produits chargés:', this.produits);

          // Debug: vérifiez les images pour chaque produit
          produits.forEach((produit, index) => {
            console.log(`Produit ${index}:`, produit.nom);
            console.log('ID produit:', produit.id);
            console.log('Images:', produit.images);
            console.log('URL générée:', this.getProductImageUrl(produit));
          });
        },
        error: (error) => {
          console.error('Erreur chargement produits:', error);
          this.errorMessage = 'Erreur lors du chargement des produits à valider';
          this.isLoading = false;
        }
      });
  }

  private extractFilterLists() {
    // Extraire les vendeurs uniques
    this.vendeurs = [...new Set(this.produits.map(p => p.vendeurNom))];

    // Extraire les acheteurs uniques
    this.acheteurs = [...new Set(this.produits.map(p => p.acheteurNom))];
  }

  applyFilters() {
    this.produitsFiltres = this.produits.filter(produit => {
      // Filtre par recherche
      const matchesSearch = this.searchTerm === '' ||
        produit.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        produit.description.toLowerCase().includes(this.searchTerm.toLowerCase());

      // Filtre par vendeur
      const matchesVendeur = this.selectedVendeur === '' ||
        produit.vendeurNom === this.selectedVendeur;

      // Filtre par acheteur
      const matchesAcheteur = this.selectedAcheteur === '' ||
        produit.acheteurNom === this.selectedAcheteur;

      return matchesSearch && matchesVendeur && matchesAcheteur;
    });
  }

  // MÉTHODE CORRIGÉE POUR LES IMAGES - SIMILAIRE À pub-en-attente-admin
  getProductImageUrl(produit: ProduitAValider): string {
    // Vérification plus robuste des images
    if (!produit.images || produit.images.length === 0 || !produit.images[0]) {
      return this.placeholderImage;
    }

    const imageName = produit.images[0];

    // Nettoyage du nom d'image (au cas où)
    const cleanImageName = imageName.trim();

    return `${this.API_BASE_URL}/produits/images/${produit.id}/${cleanImageName}`;
  }

  // MÉTHODE POUR GÉRER LES ERREURS D'IMAGE
  onImageError(event: Event) {
    const imgElement = event.target as HTMLImageElement;
    console.warn('Erreur de chargement image, utilisation placeholder:', imgElement.src);
    imgElement.src = this.placeholderImage;
    // Empêcher la propagation de l'erreur
    event.preventDefault();
  }

  validerVente(produit: ProduitAValider) {
    if (!confirm(`Voulez-vous vraiment valider la vente du produit "${produit.nom}" ?`)) {
      return;
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.http.put(`${this.API_BASE_URL}/produits/${produit.id}/valider-vente`, {})
      .subscribe({
        next: (response: any) => {
          if (response.success) {
            this.successMessage = `Vente validée avec succès pour "${produit.nom}"`;

            // Recharger la liste
            setTimeout(() => {
              this.loadProduitsAValider();
            }, 2000);
          } else {
            this.errorMessage = response.message || 'Erreur lors de la validation';
          }
        },
        error: (error) => {
          console.error('Erreur validation:', error);
          this.errorMessage = 'Erreur serveur lors de la validation';
        }
      });
  }

  refuserVente(produit: ProduitAValider) {
    const raison = prompt(`Pourquoi refusez-vous la vente du produit "${produit.nom}" ?`);

    if (raison === null) {
      return; // Annulé
    }

    this.successMessage = '';
    this.errorMessage = '';

    this.http.put(`${this.API_BASE_URL}/produits/${produit.id}/refuser-vente`, {
      raison: raison
    })
      .subscribe({
        next: (response: any) => {
          if (response.success) {
            this.successMessage = `Vente refusée pour "${produit.nom}"`;

            // Recharger la liste
            setTimeout(() => {
              this.loadProduitsAValider();
            }, 2000);
          } else {
            this.errorMessage = response.message || 'Erreur lors du refus';
          }
        },
        error: (error) => {
          console.error('Erreur refus:', error);
          this.errorMessage = 'Erreur serveur lors du refus';
        }
      });
  }

  clearFilters() {
    this.searchTerm = '';
    this.selectedVendeur = '';
    this.selectedAcheteur = '';
    this.produitsFiltres = [...this.produits];
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
