import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface Produit {
  id: number;
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number;
  etat: string;
  vendeurNom: string;
  categorieNom: string;
  images: string[];
  aExpertise: boolean;
}

@Component({
  selector: 'app-produit-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './produit-details-admin.component.html',
  styleUrls: ['./produit-details-admin.component.css']
})
export class ProduitDetailsAdminComponent implements OnInit {
  produit: Produit | null = null;
  isLoading = true;
  mainImageIndex = 0;
  private produitId!: number;
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.produitId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProductDetails();
  }

  loadProductDetails() {
    this.isLoading = true;
    this.http.get<Produit>(`${this.API_BASE_URL}/api/produits/${this.produitId}`)
      .subscribe({
        next: (produit) => {
          this.produit = produit;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur chargement détails produit:', error);
          this.isLoading = false;
        }
      });
  }

  getImageUrl(index: number): string {
    if (!this.produit?.images || this.produit.images.length === 0) {
      return this.placeholderImage;
    }
    const imageName = this.produit.images[index];
    return `${this.API_BASE_URL}/api/produits/images/${this.produitId}/${imageName}`;
  }

  onImageError(event: Event) {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = this.placeholderImage;
  }

  onThumbnailError(event: Event) {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = this.placeholderImage;
  }

  acceptProduct() {
    if (confirm('Êtes-vous sûr de vouloir accepter ce produit ?')) {
      this.updateProductState('accepte');
    }
  }

  rejectProduct() {
    if (confirm('Êtes-vous sûr de vouloir refuser ce produit ?')) {
      this.updateProductState('refuse');
    }
  }

  private updateProductState(newState: string) {
    this.http.put(`${this.API_BASE_URL}/api/produits/${this.produitId}/etat`, { etat: newState })
      .subscribe({
        next: () => {
          alert(`Produit ${newState === 'accepte' ? 'accepté' : 'refusé'} avec succès !`);
          this.router.navigate(['/pub-en-attente']);
        },
        error: (error) => {
          console.error('Erreur mise à jour état produit:', error);
          alert('Erreur lors de la mise à jour du produit');
        }
      });
  }
}
