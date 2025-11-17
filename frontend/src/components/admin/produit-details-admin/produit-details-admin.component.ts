import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  imports: [CommonModule, FormsModule],
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

  // États des modales
  showAcceptModal = false;
  showRejectModal = false;
  rejectionNote = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.produitId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProductDetails();
  }

  goBackToList() {
    this.router.navigate(['/admin/publications']);
  }

  getStatusLabel(etat: string): string {
    switch(etat) {
      case 'en_attente': return 'En attente';
      case 'accepte': return 'Accepté';
      case 'refuse': return 'Refusé';
      default: return etat;
    }
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

  // Méthodes pour les modales
  openAcceptModal() {
    this.showAcceptModal = true;
  }

  openRejectModal() {
    this.showRejectModal = true;
    this.rejectionNote = '';
  }

  closeModals() {
    this.showAcceptModal = false;
    this.showRejectModal = false;
    this.rejectionNote = '';
  }

  confirmAccept() {
    this.updateProductState('accepte', '');
    this.closeModals();
  }

  confirmReject() {
    if (!this.rejectionNote.trim()) {
      alert('Veuillez saisir une note expliquant le refus.');
      return;
    }
    this.updateProductState('refuse', this.rejectionNote);
    this.closeModals();
  }

  private updateProductState(newState: string, note: string) {
    const requestBody = {
      etat: newState,
      noteAdmin: note
    };

    this.http.put(`${this.API_BASE_URL}/api/produits/${this.produitId}/etat`, requestBody)
      .subscribe({
        next: () => {
          const message = newState === 'accepte'
            ? 'Produit accepté avec succès !'
            : 'Produit refusé avec succès !';

          alert(message);
          this.router.navigate(['/admin/publications']);
        },
        error: (error) => {
          console.error('Erreur mise à jour état produit:', error);
          alert('Erreur lors de la mise à jour du produit');
        }
      });
  }
}
