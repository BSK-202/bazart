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
  slots?: string[];
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
  expertId?: number;
  reportExists?: boolean;
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

  // Timers et états
  private timers: any[] = [];
  expertResponseExpired = false;
  reportDeadlineExpired = false;
  selectedSlotExpired = false;
  reportSubmissionCountdown = '';
  private reportCountdownInterval: any;

  // Lightbox
  lightboxOpen = false;
  lightboxIndex = 0;

  // Formulaires et sélections
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

  selectedSlotIndex: number | null = null;
  selectedSlotDate: Date | null = null;

  productConditions = Object.values(ProductCondition);
  authenticityLevels = Object.values(AuthenticityLevel);
  recommendations = Object.values(ExpertiseRecommendation);

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
    // Nettoyer tous les timers
    this.timers.forEach(timer => clearInterval(timer));
    if (this.reportCountdownInterval) clearInterval(this.reportCountdownInterval);
  }

  // ==================== CHARGEMENT DES DONNÉES ====================
  loadExpertiseDetails(): void {
    this.isLoading = true;
    this.http
      .get<any>(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/detail`)
      .subscribe({
        next: (data) => {
          this.processExpertiseData(data);
        },
        error: (err) => {
          console.error('Erreur chargement expertise:', err);
          this.error = "Erreur lors du chargement des détails de l'expertise.";
          this.isLoading = false;
        }
      });
  }

  private processExpertiseData(data: any): void {
    this.expertise = data;

    // Images
    const images = this.processImages(data);

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

    // Charger images supplémentaires
    this.loadAdditionalImages(data.produitId);

    // Initialiser les vérifications d'état
    this.initializeStateVerifications();

    this.isLoading = false;
  }

  private processImages(data: any): string[] {
    const images = (
      (data.produitImages && data.produitImages.length > 0)
        ? data.produitImages
        : data.produitImage
          ? [data.produitImage]
          : []
    ).map((img: string) => this.getProduitImageUrl(data.produitId, img));

    // Toujours au moins un placeholder
    if (images.length === 0) {
      images.push(this.PLACEHOLDER);
    }

    return images;
  }

  private loadAdditionalImages(produitId: number): void {
    this.http
      .get<string[]>(`${this.API_BASE_URL}/api/produits/${produitId}/images`)
      .subscribe({
        next: (files) => {
          if (!files || files.length === 0 || !this.produit) return;

          const mapped = files.map((f: string) => this.getProduitImageUrl(produitId, f));
          const set = new Set<string>([...this.produit.images, ...mapped]);
          this.produit.images = Array.from(set);

          // Réinitialiser les sélections d'image
          if (this.selectedImage >= this.produit.images.length) {
            this.selectedImage = 0;
          }
          if (this.lightboxIndex >= this.produit.images.length) {
            this.lightboxIndex = 0;
          }
        },
        error: () => {
          // Ignorer silencieusement si pas d'images supplémentaires
        }
      });
  }

  // ==================== GESTION DES ÉTATS ET TIMERS ====================
  private initializeStateVerifications(): void {
    if (!this.expertise) return;

    // Réinitialiser tous les timers existants
    this.clearAllTimers();

    // Vérifier l'expiration initiale
    this.checkAllExpirations();

    // Démarrer les timers selon l'état
    if (this.isPendingDecision() && this.expertise.expertResponseDeadline) {
      this.startExpertResponseTimer();
    }

    if (this.isPlanned()) {
      if (this.expertise.method === 'ONLINE' && this.expertise.reportSubmissionDeadline) {
        this.startReportCountdownTimer();
      } else if (this.expertise.method === 'ONSITE') {
        this.checkOnsiteSlotAvailability();
      }
    }
  }

  private checkAllExpirations(): void {
    if (!this.expertise) return;

    // Réinitialiser tous les états d'expiration
    this.expertResponseExpired = false;
    this.reportDeadlineExpired = false;
    this.selectedSlotExpired = false;

    // Vérifier expiration réponse expert
    if (this.isPendingDecision() && this.expertise.expertResponseDeadline) {
      this.expertResponseExpired = this.isDateExpired(this.expertise.expertResponseDeadline);
    }

    // Vérifier expiration rapport (ONLINE seulement)
    if (this.isPlanned() && this.expertise.method === 'ONLINE' && this.expertise.reportSubmissionDeadline) {
      this.reportDeadlineExpired = this.isDateExpired(this.expertise.reportSubmissionDeadline);

      // Mettre à jour le compteur
      if (!this.reportDeadlineExpired) {
        this.reportSubmissionCountdown = this.formatCountdown(this.expertise.reportSubmissionDeadline);
      } else {
        this.reportSubmissionCountdown = 'EXPIRÉ';
      }
    }

    // Vérifier expiration créneau (ONSITE)
    if (this.isPlanned() && this.expertise.method === 'ONSITE') {
      if (this.selectedSlotDate) {
        this.selectedSlotExpired = this.isDateExpired(this.selectedSlotDate.toISOString());
      } else if (this.expertise.confirmedDateTime) {
        const confirmedDate = new Date(this.expertise.confirmedDateTime);
        this.selectedSlotExpired = confirmedDate.getTime() < Date.now();
      }
    }
  }

  private startExpertResponseTimer(): void {
    const timer = setInterval(() => {
      if (!this.expertise?.expertResponseDeadline) return;

      const wasExpired = this.expertResponseExpired;
      this.expertResponseExpired = this.isDateExpired(this.expertise.expertResponseDeadline);

      // Si l'état change d'expiré à non expiré (peu probable mais sécurisé)
      if (wasExpired !== this.expertResponseExpired) {
        this.checkAllExpirations();
      }
    }, 1000);

    this.timers.push(timer);
  }

  private startReportCountdownTimer(): void {
    if (!this.expertise?.reportSubmissionDeadline) return;

    // Nettoyer l'ancien timer s'il existe
    if (this.reportCountdownInterval) {
      clearInterval(this.reportCountdownInterval);
    }

    this.reportCountdownInterval = setInterval(() => {
      if (!this.expertise?.reportSubmissionDeadline) return;

      const wasExpired = this.reportDeadlineExpired;
      this.reportDeadlineExpired = this.isDateExpired(this.expertise.reportSubmissionDeadline);

      // Mettre à jour le compteur
      if (!this.reportDeadlineExpired) {
        this.reportSubmissionCountdown = this.formatCountdown(this.expertise.reportSubmissionDeadline);
      } else {
        this.reportSubmissionCountdown = 'EXPIRÉ';
        clearInterval(this.reportCountdownInterval);
      }

      // Si l'état d'expiration change
      if (wasExpired !== this.reportDeadlineExpired) {
        this.checkAllExpirations();
      }
    }, 1000);
  }

  private checkOnsiteSlotAvailability(): void {
    const timer = setInterval(() => {
      if (!this.expertise || this.expertise.method !== 'ONSITE') return;

      let slotDate: Date | null = null;

      if (this.selectedSlotDate) {
        slotDate = this.selectedSlotDate;
      } else if (this.expertise.confirmedDateTime) {
        slotDate = new Date(this.expertise.confirmedDateTime);
      }

      if (slotDate) {
        const wasExpired = this.selectedSlotExpired;
        this.selectedSlotExpired = slotDate.getTime() < Date.now();

        // Si l'état change
        if (wasExpired !== this.selectedSlotExpired) {
          this.checkAllExpirations();
        }
      }
    }, 1000);

    this.timers.push(timer);
  }

  private clearAllTimers(): void {
    this.timers.forEach(timer => clearInterval(timer));
    this.timers = [];

    if (this.reportCountdownInterval) {
      clearInterval(this.reportCountdownInterval);
      this.reportCountdownInterval = null;
    }
  }

  // ==================== MÉTHODES D'ÉTAT ====================
  isPendingDecision(): boolean {
    return this.expertise?.status === 'PENDING_EXPERT_DECISION';
  }

  isPlanned(): boolean {
    return this.expertise?.status === 'PLANNED';
  }

  isCompleted(): boolean {
    return this.expertise?.status === 'COMPLETED' ||
      this.expertise?.status === 'EXPERTISED' ||
      this.expertise?.reportExists === true;
  }

  isCancelled(): boolean {
    return this.expertise?.status === 'CANCELLED' ||
      this.expertise?.status === 'ALL_EXPERTS_TRIED' ||
      this.expertise?.status === 'NO_EXPERTS_AVAILABLE';
  }

  isExpired(): boolean {
    if (this.isPendingDecision()) {
      return this.expertResponseExpired;
    } else if (this.isPlanned() && this.expertise?.method === 'ONLINE') {
      return this.reportDeadlineExpired;
    }
    return false;
  }

  // ==================== MÉTHODES D'AFFICHAGE CONDITIONNEL ====================
  shouldShowAcceptRefuse(): boolean {
    return this.isPendingDecision() && !this.expertResponseExpired;
  }

  shouldShowExpiredMessage(): boolean {
    return this.isPendingDecision() && this.expertResponseExpired;
  }

  shouldShowSlots(): boolean {
    return this.isPendingDecision() &&
      !this.expertResponseExpired &&
      this.expertise?.method === 'ONSITE' &&
      this.getSlots().length > 0;
  }

  shouldShowWaitingSlotMessage(): boolean {
    return this.isPlanned() &&
      this.expertise?.method === 'ONSITE' &&
      !this.selectedSlotExpired &&
      !this.expertise?.reportExists;
  }

  shouldShowReportCountdown(): boolean {
    return this.isPlanned() &&
      this.expertise?.method === 'ONLINE' &&
      !this.reportDeadlineExpired &&
      !this.expertise?.reportExists;
  }

  shouldShowReportDeadlineExpired(): boolean {
    return this.isPlanned() &&
      this.expertise?.method === 'ONLINE' &&
      this.reportDeadlineExpired &&
      !this.expertise?.reportExists;
  }

  shouldShowReportForm(): boolean {
    if (!this.isPlanned() || this.expertise?.reportExists) {
      return false;
    }

    if (this.expertise?.method === 'ONLINE') {
      return !this.reportDeadlineExpired;
    }

    if (this.expertise?.method === 'ONSITE') {
      return this.selectedSlotExpired;
    }

    return false;
  }

  // ==================== ACTIONS UTILISATEUR ====================
  chooseSlot(index: number): void {
    if (!this.isPendingDecision() || this.expertResponseExpired) return;

    this.selectedSlotIndex = index + 1;

    // Mettre à jour la date du créneau sélectionné
    const slots = this.getSlots();
    if (slots && slots[index]) {
      this.selectedSlotDate = new Date(slots[index]);

      // Vérifier immédiatement si le créneau est expiré
      this.selectedSlotExpired = this.isDateExpired(slots[index]);

      // Si le créneau est expiré, marquer comme tel
      if (this.selectedSlotExpired) {
        console.warn('Créneau déjà expiré lors de la sélection');
      }
    }
  }

  onAcceptRequest(): void {
    if (!this.expertise || this.expertResponseExpired) {
      alert('❌ DÉLAI DÉPASSÉ - La période pour accepter cette demande est expirée.');
      return;
    }

    // Validation ONSITE : créneau requis
    if (this.expertise.method === 'ONSITE' && !this.selectedSlotIndex) {
      alert('Sélectionnez un créneau !');
      return;
    }

    const params: any = { expertId: this.getCurrentExpertId() };
    if (this.expertise.method === 'ONSITE') {
      params.slotIndex = this.selectedSlotIndex;
    }

    this.isLoading = true;

    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/accept`, null, { params })
      .subscribe({
        next: (response: any) => {
          // Mettre à jour l'expertise avec les nouvelles données
          if (response) {
            Object.assign(this.expertise!, response);
          }

          // Réinitialiser les états
          this.expertResponseExpired = false;
          this.selectedSlotExpired = false;

          // Redémarrer les vérifications d'état
          this.initializeStateVerifications();

          alert('✅ Demande acceptée avec succès !');
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Erreur acceptation:', err);
          this.isLoading = false;
          alert("Erreur lors de l'acceptation: " + (err.error?.message || err.message || 'Erreur inconnue'));
        }
      });
  }

  onRefuseRequest(): void {
    if (!this.expertise || this.expertResponseExpired) {
      alert('❌ DÉLAI DÉPASSÉ - La période pour refuser cette demande est expirée.');
      return;
    }

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
        error: (err) => {
          console.error('Erreur refus:', err);
          this.isLoading = false;
          alert('Erreur lors du refus: ' + (err.error?.message || err.message || 'Erreur inconnue'));
        }
      });
  }

  onSubmitReport(): void {
    // Vérifications de base
    if (!this.expertise) return;

    // Vérifier si on peut soumettre
    if (!this.canSubmitReport()) {
      alert('Vous ne pouvez pas soumettre le rapport pour le moment.');
      return;
    }

    // Validation des champs obligatoires
    if (!this.reportForm.productCondition ||
      !this.reportForm.authenticityLevel ||
      !this.reportForm.recommendation) {
      alert('Veuillez renseigner tous les champs obligatoires (État, Authenticité, Recommandation).');
      return;
    }

    // Validation des prix
    if (this.reportForm.estimatedMaxPrice &&
      this.reportForm.estimatedMinPrice &&
      this.reportForm.estimatedMaxPrice < this.reportForm.estimatedMinPrice) {
      alert('Le prix maximum estimé doit être supérieur ou égal au prix minimum.');
      return;
    }

    // Préparation du FormData
    const formData = new FormData();
    formData.append('productCondition', this.reportForm.productCondition);
    formData.append('authenticityLevel', this.reportForm.authenticityLevel);
    formData.append('recommendation', this.reportForm.recommendation);

    if (this.reportForm.estimatedMinPrice != null) {
      formData.append('estimatedMinPrice', String(this.reportForm.estimatedMinPrice));
    }

    if (this.reportForm.estimatedMaxPrice != null) {
      formData.append('estimatedMaxPrice', String(this.reportForm.estimatedMaxPrice));
    }

    if (this.reportForm.recommendedStartPrice != null) {
      formData.append('recommendedStartPrice', String(this.reportForm.recommendedStartPrice));
    }

    formData.append('commentsPublic', this.reportForm.commentsPublic || '');
    formData.append('commentsInternal', this.reportForm.commentsInternal || '');

    if (this.reportForm.document) {
      formData.append('document', this.reportForm.document);
    }

    const expertId = this.getCurrentExpertId();

    this.isLoading = true;

    this.http
      .post(
        `${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/submit-report`,
        formData,
        { params: { expertId } }
      )
      .subscribe({
        next: () => {
          alert('✅ Rapport soumis avec succès !');
          this.router.navigate(['/expert/expertise']);
        },
        error: (err) => {
          console.error('Erreur soumission rapport:', err);
          this.isLoading = false;
          alert("❌ Erreur lors de l'envoi du rapport: " + (err.error?.message || err.message || 'Erreur inconnue'));
        }
      });
  }

  canSubmitReport(): boolean {
    return this.shouldShowReportForm();
  }

  // ==================== MÉTHODES UTILITAIRES ====================
  private isDateExpired(dateString: string): boolean {
    if (!dateString) return false;
    try {
      const date = new Date(dateString);
      return date.getTime() < Date.now();
    } catch (e) {
      console.error('Erreur parsing date:', dateString, e);
      return true;
    }
  }

  formatCountdown(deadline: string): string {
    if (!deadline) return '';

    const now = new Date().getTime();
    const end = new Date(deadline).getTime();
    const diff = end - now;

    if (diff <= 0) return 'EXPIRÉ';

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
    const mins = Math.floor((diff / (1000 * 60)) % 60);
    const secs = Math.floor((diff / 1000) % 60);

    if (days > 0) {
      return `${days}j ${hours}h`;
    } else if (hours > 0) {
      return `${hours}h ${mins}m`;
    } else {
      return `${mins}m ${secs}s`;
    }
  }

  getSlots(): string[] {
    return this.expertise?.slots || [];
  }

  getSlotStatus(slotDate: string | undefined | null): string {
    if (!slotDate) return 'expired';

    try {
      const date = new Date(slotDate);
      const now = new Date();

      if (date.getTime() < now.getTime()) {
        return 'expired';
      } else if (date.getTime() - now.getTime() < 24 * 60 * 60 * 1000) {
        return 'soon';
      } else {
        return 'available';
      }
    } catch (e) {
      return 'expired';
    }
  }

  getCurrentExpertId(): number {
    try {
      const user = JSON.parse(localStorage.getItem('userData') || '{}');
      return user.id || 0;
    } catch (e) {
      console.error('Erreur récupération expert ID:', e);
      return 0;
    }
  }

  getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === '') return this.PLACEHOLDER;
    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  getProfileImageUrl(userId: number): string {
    this.showDefaultAvatar = false;
    return `${this.API_BASE_URL}/api/clients/images/${userId}.jpg`;
  }

  onProfileImageError(event: any): void {
    this.showDefaultAvatar = true;
    const imgElement = event.target as HTMLImageElement;
    imgElement.style.display = 'none';
  }

  onImageError(event: any): void {
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

  // Lightbox methods
  openLightbox(index: number): void {
    this.lightboxIndex = index;
    this.lightboxOpen = true;
  }

  closeLightbox(): void {
    this.lightboxOpen = false;
  }

  prevLightbox(): void {
    if (!this.produit) return;
    this.lightboxIndex = (this.lightboxIndex - 1 + this.produit.images.length) % this.produit.images.length;
  }

  nextLightbox(): void {
    if (!this.produit) return;
    this.lightboxIndex = (this.lightboxIndex + 1) % this.produit.images.length;
  }

  onFileSelected(event: any): void {
    const fileInput = event.target as HTMLInputElement;
    if (fileInput.files && fileInput.files.length > 0) {
      this.reportForm.document = fileInput.files[0];
    } else {
      this.reportForm.document = null;
    }
  }

  selectImage(index: number): void {
    this.selectedImage = index;
  }
  getCurrentStatus(): string {
    // Retourne le statut en majuscules, ou vide si non défini
    return this.expertise?.status?.toUpperCase() || '';
  }
}
