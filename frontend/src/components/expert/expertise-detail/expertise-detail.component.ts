import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

export enum ProductCondition {
  EXCELLENT = 'EXCELLENT',
  VERY_GOOD = 'VERY_GOOD',
  GOOD = 'GOOD',
  FAIR = 'FAIR',
  POOR = 'POOR'
}
export enum AuthenticityLevel {
  AUTHENTIC = 'AUTHENTIC',
  PROBABLE = 'PROBABLE',
  UNKNOWN = 'UNKNOWN',
  FAKE = 'FAKE'
}
export enum ExpertiseRecommendation {
  AUTHORISE_AUCTION = 'AUTHORISE_AUCTION',
  REFUSE = 'REFUSE'
}

interface Seller {
  idClient: number;
  nom: string;
  prenom: string;
  email?: string;
  tel?: string;
  ville?: string;
  pays?: string;
  dateInscription?: string;
  photoProfil?: string;
  enabled?: boolean;
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
  images: string[];
  vendeurId: number;
  datePublication?: string;
  aExpertise: boolean;
  dateenchere?: string;
  dureeEnchereJours?: number;
}
interface ExpertiseRequest {
  id: number;
  produitId: number;
  produitNom: string;
  produitDescription?: string;
  produitEtat?: string;
  produitCategorie?: string;
  produitImages?: string[];
  method: 'ONLINE' | 'ONSITE';
  status: string;
  price: number;
  slots: string[];
  confirmedDateTime?: string;
  location?: string;
  expertResponseDeadline?: string;
  reportSubmissionDeadline?: string;
  vendeurNom?: string;
  vendeurPrenom?: string;
  vendeurVille?: string;
  vendeurPays?: string;
  vendeurTel?: string;
  vendeurId?: number;
  expertShare?: number;
}

@Component({
  selector: 'app-expert-expertise-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './expertise-detail.component.html',
  styleUrls: ['./expertise-detail.component.css']
})
export class ExpertiseDetailComponent implements OnInit, OnDestroy {
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly PLACEHOLDER = 'assets/images/placeholder.jpg';

  expertiseId!: number;
  expertise: ExpertiseRequest | null = null;
  produit: Produit | null = null;
  vendeur: Seller | null = null;
  isLoading = true;
  error = '';
  selectedImage = 0;
  showDefaultAvatar = false;

  // Lightbox
  lightboxOpen = false;
  lightboxIndex = 0;

  reportSubmissionCountdown = '';
  private reportCountdownInterval: any;

  reportForm = {
    productCondition: '',
    authenticityLevel: '',
    estimatedMinPrice: null as number | null,
    estimatedMaxPrice: null as number | null,
    recommendedStartPrice: null as number | null,
    recommendation: '',
    commentsPublic: '',
    commentsInternal: '',
    document: null as File | null
  };

  productConditions = Object.values(ProductCondition);
  authenticityLevels = Object.values(AuthenticityLevel);
  recommendations = Object.values(ExpertiseRecommendation);

  selectedSlotIndex: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.expertiseId = Number(this.route.snapshot.paramMap.get('expertiseId'));
    this.loadExpertiseDetails();
  }

  ngOnDestroy(): void {
    if (this.reportCountdownInterval) clearInterval(this.reportCountdownInterval);
  }

  loadExpertiseDetails(): void {
    this.isLoading = true;
    this.http
      .get<any>(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/detail`)
      .subscribe({
        next: (data) => {
          this.expertise = data;

          // Images de base venant du détail
          const images =
            (data.produitImages && data.produitImages.length > 0
              ? data.produitImages
              : data.produitImage
              ? [data.produitImage]
              : []
            ).map((img: string) => this.getProduitImageUrl(data.produitId, img));

          // Garantir au moins un placeholder pour éviter un src undefined
          if (images.length === 0) {
            images.push(this.PLACEHOLDER);
          }

          this.produit = {
            id: data.produitId,
            nom: data.produitNom,
            description: data.produitDescription || '',
            etat: data.produitEtat || '',
            prixDebut: data.prixDebut ?? 0,
            prixFin: data.prixFin ?? null,
            acheteurNom: data.acheteurNom ?? null,
            vendeurNom: data.vendeurNom || '',
            vendeurId: data.vendeurId,
            categorieNom: data.produitCategorie || '',
            images,
            aExpertise: true,
            datePublication: data.datePublication,
            dateenchere: data.dateenchere,
            dureeEnchereJours: data.dureeEnchereJours
          };

          this.vendeur = {
            idClient: data.vendeurId,
            nom: data.vendeurNom || '',
            prenom: data.vendeurPrenom || '',
            ville: data.vendeurVille,
            pays: data.vendeurPays,
            tel: data.vendeurTel
          };

          // Charger les autres images du produit et fusionner
          this.loadProduitImages(data.produitId);

          if (this.expertise?.reportSubmissionDeadline) {
            this.startReportCountdown(this.expertise.reportSubmissionDeadline);
          }

          this.isLoading = false;
        },
        error: () => {
          this.error = "Erreur lors du chargement des détails de l'expertise.";
          this.isLoading = false;
        }
      });
  }

  private loadProduitImages(produitId: number) {
    this.http
      .get<string[]>(`${this.API_BASE_URL}/api/produits/${produitId}/images`)
      .subscribe({
        next: (files) => {
          if (!files || files.length === 0 || !this.produit) return;
          const mapped = files.map((f: string) => this.getProduitImageUrl(produitId, f));
          const set = new Set<string>([...this.produit.images, ...mapped]);
          this.produit.images = Array.from(set);
          // Réinitialiser l’index si besoin
          this.selectedImage = 0;
          this.lightboxIndex = 0;
        },
        error: () => { /* on ignore si pas d’images supplémentaires */ }
      });
  }

  startReportCountdown(deadline: string) {
    this.reportCountdownInterval = setInterval(() => {
      this.reportSubmissionCountdown = this.formatCountdown(deadline);
    }, 1000);
  }

  selectImage(index: number): void {
    this.selectedImage = index;
  }

  chooseSlot(index: number) {
    if (!this.isPendingDecision()) return;
    this.selectedSlotIndex = index;
  }

  // Lightbox helpers
  openLightbox(index: number) {
    this.lightboxIndex = index;
    this.lightboxOpen = true;
  }
  closeLightbox() { this.lightboxOpen = false; }
  prevLightbox() {
    if (!this.produit) return;
    this.lightboxIndex = (this.lightboxIndex - 1 + this.produit.images.length) % this.produit.images.length;
  }
  nextLightbox() {
    if (!this.produit) return;
    this.lightboxIndex = (this.lightboxIndex + 1) % this.produit.images.length;
  }

  formatCountdown(deadline: string): string {
    if (!deadline) return '';
    const now = new Date().getTime();
    const end = new Date(deadline).getTime();
    const diff = end - now;
    if (diff <= 0) return 'Expiré';
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
    const mins = Math.floor((diff / (1000 * 60)) % 60);
    const secs = Math.floor((diff / 1000) % 60);
    return `${days}j ${hours}h ${mins}m ${secs}s`;
  }

  getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === '') return this.PLACEHOLDER;
    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  getProfileImageUrl(userId: number): string {
    this.showDefaultAvatar = false;
    return `${this.API_BASE_URL}/api/clients/images/${userId}.jpg`;
  }

  onProfileImageError(event: any) {
    this.showDefaultAvatar = true;
    const imgElement = event.target as HTMLImageElement;
    imgElement.style.display = 'none';
  }

  onImageError(event: any) {
    const img = event.target as HTMLImageElement;
    if (img) {
      img.src = this.PLACEHOLDER;
      img.onerror = null;
    }
  }

  getInitials(name: string | undefined): string {
    if (!name) return '??';
    const parts = name.trim().split(' ');
    return (parts[0][0] + (parts[1]?.[0] ?? '')).toUpperCase();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    this.reportForm.document = file;
  }

  onAcceptRequest() {
    if (!this.expertise) return;
    const params: any = { expertId: this.getCurrentExpertId() };
    if (this.expertise.method === 'ONSITE') {
      if (!this.selectedSlotIndex) {
        alert('Sélectionnez un créneau !');
        return;
      }
      params.slotIndex = this.selectedSlotIndex;
    }
    this.isLoading = true;
    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/accept`, null, { params })
      .subscribe({
        next: () => {
          this.loadExpertiseDetails();
        },
        error: () => {
          this.isLoading = false;
          alert("Erreur lors de l'acceptation.");
        }
      });
  }

  onRefuseRequest() {
    if (!this.expertise) return;
    this.isLoading = true;
    this.http
      .post(
        `${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/refuse`,
        null,
        { params: { expertId: this.getCurrentExpertId() } }
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/expert/expertise']);
        },
        error: () => {
          this.isLoading = false;
          alert('Erreur lors du refus.');
        }
      });
  }

    onSubmitReport() {
      if (
        !this.reportForm.productCondition ||
        !this.reportForm.authenticityLevel ||
        !this.reportForm.recommendation
      ) {
        alert('Veuillez renseigner tous les champs obligatoires du rapport.');
        return;
      }
      if (
        this.reportForm.estimatedMaxPrice &&
        this.reportForm.estimatedMinPrice &&
        this.reportForm.estimatedMaxPrice < this.reportForm.estimatedMinPrice
      ) {
        alert('Le prix maximum estimé doit être supérieur ou égal au minimum.');
        return;
      }
      if (this.expertise?.method === 'ONSITE' && (!this.selectedSlotIndex || this.selectedSlotIndex < 1)) {
        alert("Sélectionnez d'abord un créneau.");
        return;
      }

      if (this.expertise?.method === 'ONSITE') {
          const slotDateStr =
            this.expertise?.confirmedDateTime ||
            (this.expertise?.slots && this.selectedSlotIndex
              ? this.expertise.slots[this.selectedSlotIndex - 1]
              : null);

          if (!slotDateStr) {
            alert("Créneau choisi introuvable. Rechargez la page ou re-sélectionnez un créneau.");
            return;
          }

          const slotDate = new Date(slotDateStr);
          if (slotDate.getTime() > Date.now()) {
            alert(`Vous pourrez soumettre le rapport après le ${slotDate.toLocaleString('fr-FR')}.`);
            return;
          }
        }

      const formData = new FormData();
      formData.append('productCondition', this.reportForm.productCondition);
      formData.append('authenticityLevel', this.reportForm.authenticityLevel);
      if (this.reportForm.estimatedMinPrice != null)
        formData.append('estimatedMinPrice', String(this.reportForm.estimatedMinPrice));
      if (this.reportForm.estimatedMaxPrice != null)
        formData.append('estimatedMaxPrice', String(this.reportForm.estimatedMaxPrice));
      if (this.reportForm.recommendedStartPrice != null)
        formData.append('recommendedStartPrice', String(this.reportForm.recommendedStartPrice));
      formData.append('recommendation', this.reportForm.recommendation);
      formData.append('commentsPublic', this.reportForm.commentsPublic || '');
      formData.append('commentsInternal', this.reportForm.commentsInternal || '');
      if (this.reportForm.document) formData.append('document', this.reportForm.document);
      if (this.expertise?.method === 'ONSITE' && this.selectedSlotIndex) {
        formData.append('slotIndex', String(this.selectedSlotIndex));
      }

      const expertId = this.getCurrentExpertId();
      const params: any = { expertId };
      this.isLoading = true;

      this.http
        .post(
          `${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/submit-report`,
          formData,
          { params }
        )
        .subscribe({
          next: () => {
            alert('✅ Rapport soumis, PDF généré.');
            this.router.navigate(['/expert/expertise']);
          },
          error: () => {
            this.isLoading = false;
            alert("❌ Erreur lors de l'envoi du rapport.");
          }
        });
    }

  getCurrentExpertId(): number {
    const user = JSON.parse(localStorage.getItem('userData') || '{}');
    return user.id;
  }

  isPendingDecision(): boolean {
    return this.expertise?.status === 'PENDING_EXPERT_DECISION';
  }

  isPlanned(): boolean {
    return this.expertise?.status === 'PLANNED';
  }
}
