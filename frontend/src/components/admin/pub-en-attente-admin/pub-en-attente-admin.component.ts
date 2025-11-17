import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
}

@Component({
  selector: 'app-pub-en-attente',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pub-en-attente-admin.component.html',
  styleUrls: ['./pub-en-attente-admin.component.css']
})
export class PubEnAttenteAdminComponent implements OnInit {
  produits: Produit[] = [];
  isLoading = true;
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadPendingProducts();
  }

  loadPendingProducts() {
    this.isLoading = true;
    this.http.get<Produit[]>(`${this.API_BASE_URL}/api/produits/en-attente`)
      .subscribe({
        next: (produits) => {
          this.produits = produits;
          this.isLoading = false;
          console.log('Produits en attente chargés:', produits);

          // Debug: vérifiez les images pour chaque produit
          produits.forEach((produit, index) => {
            console.log(`Produit ${index}:`, produit.nom);
            console.log('Images:', produit.images);
            console.log('URL générée:', this.getProductImageUrl(produit));
          });
        },
        error: (error) => {
          console.error('Erreur chargement produits en attente:', error);
          this.isLoading = false;
        }
      });
  }

  getProductImageUrl(produit: Produit): string {
    // Vérification plus robuste des images
    if (!produit.images || produit.images.length === 0 || !produit.images[0]) {
      return this.placeholderImage;
    }

    const imageName = produit.images[0];

    // Nettoyage du nom d'image (au cas où)
    const cleanImageName = imageName.trim();

    return `${this.API_BASE_URL}/api/produits/images/${produit.id}/${cleanImageName}`;
  }

  onImageError(event: Event) {
    const imgElement = event.target as HTMLImageElement;
    console.warn('Erreur de chargement image, utilisation placeholder:', imgElement.src);
    imgElement.src = this.placeholderImage;
    // Empêcher la propagation de l'erreur
    event.preventDefault();
  }

  viewDetails(produitId: number) {
    this.router.navigate(['/admin/publications', produitId]);
  }
}
