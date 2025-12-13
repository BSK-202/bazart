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
  slots?: string[]; // Ajout de ? pour indiquer que c'est optionnel
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
  expertiseExpired = false; // Global expiration state

  expertiseId!: number;
  expertise: ExpertiseRequest | null = null;
  produit: Produit | null = null;
  vendeur: Seller | null = null;
  isLoading = true;
  error = '';
  selectedImage = 0;
  showDefaultAvatar = false;

  // Timers
  private timers: any[] = [];
  expertResponseExpired = false;
  reportDeadlineExpired = false;
  canSubmitReport = false;
  selectedSlotExpired = false;

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
  selectedSlotDate: Date | null = null;

  // NOUVEAU : Variable pour gérer l'état local immédiat
  localStatus: string | null = null;

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
    this.timers.forEach(timer => clearInterval(timer));
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

          // Vérifier les expirations
          this.checkExpirations();

          // Vérifier si le rapport peut être soumis
          this.checkCanSubmitReport();

          // Démarrer les timers pour les compteurs
          this.startTimers();

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
          // Réinitialiser l'index si besoin
          this.selectedImage = 0;
          this.lightboxIndex = 0;
        },
        error: () => { /* on ignore si pas d'images supplémentaires */ }
      });
  }

  private startTimers() {
    // Timer pour l'expiration de la réponse de l'expert
    if (this.expertise?.expertResponseDeadline) {
      const timer1 = setInterval(() => {
        this.expertResponseExpired = this.isDateExpired(this.expertise!.expertResponseDeadline!);
        this.expertiseExpired = this.expertResponseExpired || this.reportDeadlineExpired;
      }, 1000);
      this.timers.push(timer1);
    }

    // Timer pour l'expiration de soumission du rapport
    if (this.expertise?.reportSubmissionDeadline) {
      const timer2 = setInterval(() => {
        this.reportDeadlineExpired = this.isDateExpired(this.expertise!.reportSubmissionDeadline!);
        this.expertiseExpired = this.expertResponseExpired || this.reportDeadlineExpired;
        this.reportSubmissionCountdown = this.formatCountdown(this.expertise!.reportSubmissionDeadline!);
      }, 1000);
      this.timers.push(timer2);
    }

    // Timer pour vérifier si le créneau sélectionné est expiré
    if (this.selectedSlotDate) {
      const timer3 = setInterval(() => {
        this.selectedSlotExpired = this.isDateExpired(this.selectedSlotDate!.toISOString());
        this.checkCanSubmitReport();
      }, 1000);
      this.timers.push(timer3);
    }
  }
  private checkExpirations() {
    // Vérifier si la date de réponse de l'expert est expirée
    if (this.expertise?.expertResponseDeadline && this.isPendingDecision()) {
      this.expertResponseExpired = this.isDateExpired(this.expertise.expertResponseDeadline);
    } else {
      this.expertResponseExpired = false;
    }

    // Vérifier si la date de soumission du rapport est expirée
    if (this.expertise?.reportSubmissionDeadline && this.isPlanned()) {
      this.reportDeadlineExpired = this.isDateExpired(this.expertise.reportSubmissionDeadline);
    } else {
      this.reportDeadlineExpired = false;
    }

    // ✅ EXPERTISE EXPIRÉE GLOBALE
    if (this.isPendingDecision()) {
      this.expertiseExpired = this.expertResponseExpired;
    } else if (this.isPlanned()) {
      this.expertiseExpired = this.reportDeadlineExpired;
    } else {
      this.expertiseExpired = false;
    }
  }
  private checkCanSubmitReport() {
    if (!this.isPlanned() || this.expertise?.reportExists) {
      this.canSubmitReport = false;
      return;
    }

    // Si expertise ONLINE, on peut soumettre le rapport si la deadline n'est pas expirée
    if (this.expertise?.method === 'ONLINE') {
      this.canSubmitReport = !this.reportDeadlineExpired;
      return;
    }

    // Si expertise ONSITE, on doit attendre que le créneau soit passé
    if (this.expertise?.method === 'ONSITE') {
      if (this.selectedSlotIndex && this.selectedSlotDate) {
        this.canSubmitReport = this.selectedSlotExpired;
      } else if (this.expertise.confirmedDateTime) {
        const confirmedDate = new Date(this.expertise.confirmedDateTime);
        this.canSubmitReport = confirmedDate.getTime() < Date.now();
      } else {
        this.canSubmitReport = false;
      }
    }
  }
  // Ajoutez cette méthode dans la classe
  shouldShowOnsiteWaitingMessage(): boolean {
    return this.isPlanned() &&
      this.expertise?.method === 'ONSITE' &&
      !this.canSubmitReport &&
      !this.expertise?.reportExists;
  }

  private isDateExpired(dateString: string): boolean {
    if (!dateString) return true;
    const date = new Date(dateString);
    return date.getTime() < Date.now();
  }

  selectImage(index: number): void {
    this.selectedImage = index;
  }

  chooseSlot(index: number) {
    if (!this.isPendingDecision() || this.expertResponseExpired) return;
    this.selectedSlotIndex = index + 1;

    // Mettre à jour la date du créneau sélectionné
    const slots = this.getSlots();
    if (slots && slots[index]) {
      this.selectedSlotDate = new Date(slots[index]);
      this.checkCanSubmitReport();
    }
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

  // NOUVELLE : Méthode pour obtenir le statut actuel (priorité à localStatus)
  getCurrentStatus(): string {
    return this.localStatus || this.expertise?.status || '';
  }

  // NOUVELLE : Méthode pour mettre à jour l'état local immédiatement
  private updateStatusImmediately(newStatus: string): void {
    this.localStatus = newStatus;

    if (newStatus === 'PLANNED') {
      // Réinitialiser l'expiration de la réponse expert
      this.expertResponseExpired = false;

      // Démarrer le timer pour le rapport si nécessaire
      if (this.expertise?.reportSubmissionDeadline && !this.reportCountdownInterval) {
        this.reportCountdownInterval = setInterval(() => {
          this.reportDeadlineExpired = this.isDateExpired(this.expertise!.reportSubmissionDeadline!);
          this.expertiseExpired = this.reportDeadlineExpired;
          this.reportSubmissionCountdown = this.formatCountdown(this.expertise!.reportSubmissionDeadline!);
        }, 1000);

        // Mise à jour immédiate
        this.reportSubmissionCountdown = this.formatCountdown(this.expertise.reportSubmissionDeadline);
      }
    }

    // Mettre à jour les vérifications
    this.checkExpirations();
    this.checkCanSubmitReport();
  }
  onAcceptRequest() {
    if (!this.expertise || this.expertiseExpired) {
      alert('❌ DÉLAI DÉPASSÉ - La période pour accepter cette demande est expirée.');
      return;
    }

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
        next: (response: any) => {
          this.isLoading = false;

          // ✅ 1. Mettre à jour immédiatement l'état local
          this.localStatus = 'PLANNED';

          // ✅ 2. Réinitialiser l'expiration de la réponse expert
          this.expertResponseExpired = false;

          // ✅ 3. Mettre à jour les propriétés de l'expertise avec la réponse du serveur
          if (response) {
            if (response.reportSubmissionDeadline) {
              this.expertise!.reportSubmissionDeadline = response.reportSubmissionDeadline;
            }
            if (response.confirmedDateTime && this.expertise?.method === 'ONSITE') {
              this.expertise!.confirmedDateTime = response.confirmedDateTime;
              this.selectedSlotDate = new Date(response.confirmedDateTime);
            }
          }

          // ✅ 4. Démarrer le timer pour le compte à rebours du rapport
          if (this.expertise?.reportSubmissionDeadline) {
            // Nettoyer l'ancien timer s'il existe
            if (this.reportCountdownInterval) {
              clearInterval(this.reportCountdownInterval);
            }

            // Calculer immédiatement le compteur
            this.reportSubmissionCountdown = this.formatCountdown(this.expertise.reportSubmissionDeadline);

            // Démarrer le nouveau timer
            this.reportCountdownInterval = setInterval(() => {
              this.reportDeadlineExpired = this.isDateExpired(this.expertise!.reportSubmissionDeadline!);
              this.expertiseExpired = this.reportDeadlineExpired;
              this.reportSubmissionCountdown = this.formatCountdown(this.expertise!.reportSubmissionDeadline!);
            }, 1000);
          }

          // ✅ 5. Mettre à jour les vérifications
          this.checkExpirations();
          this.checkCanSubmitReport();

          alert('✅ Demande acceptée avec succès !');
        },
        error: (err) => {
          this.localStatus = null; // Annuler le changement en cas d'erreur
          this.isLoading = false;
          this.checkExpirations();
          console.error('Erreur lors de l\'acceptation:', err);
          alert("Erreur lors de l'acceptation: " + (err.error?.message || ''));
        }
      });
  }

  onRefuseRequest() {
    if (!this.expertise || this.expertiseExpired) {
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
        error: () => {
          this.isLoading = false;
          alert('Erreur lors du refus.');
        }
      });
  }

  onSubmitReport() {
    // Vérification d'expiration en premier
    // Vérification d'expiration en premier
    if (this.expertiseExpired) {
      alert('❌ DÉLAI DÉPASSÉ - La période pour soumettre un rapport est expirée.');
      return;
    }

    // Vérifications de base
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

    // Vérifier si on peut soumettre le rapport (différent pour ONLINE vs ONSITE)
    if (!this.canSubmitReport) {
      if (this.expertise?.method === 'ONSITE') {
        alert('Vous ne pouvez pas soumettre le rapport avant que le créneau sélectionné ne soit passé.');
      } else {
        alert('Vous ne pouvez pas soumettre le rapport pour le moment.');
      }
      return;
    }

    // Vérifier si la date limite est expirée (uniquement pour ONLINE)
    if (this.expertise?.method === 'ONLINE' && this.reportDeadlineExpired) {
      alert('La date limite pour soumettre le rapport est expirée.');
      return;
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

  canSubmitReportNow(): boolean {
    if (!this.isPlanned() || this.expertise?.reportExists) return false;

    if (this.expertise?.method === 'ONLINE') {
      // ONLINE : vérifier que la deadline n'est pas expirée
      return !this.reportDeadlineExpired;
    }

    if (this.expertise?.method === 'ONSITE') {
      // ONSITE : vérifier si le créneau est passé
      if (this.selectedSlotDate) {
        return this.selectedSlotExpired;
      } else if (this.expertise.confirmedDateTime) {
        const confirmedDate = new Date(this.expertise.confirmedDateTime);
        return confirmedDate.getTime() < Date.now();
      }
    }

    return false;
  }

  getCurrentExpertId(): number {
    const user = JSON.parse(localStorage.getItem('userData') || '{}');
    return user.id;
  }

  isPendingDecision(): boolean {
    return this.getCurrentStatus() === 'PENDING_EXPERT_DECISION';
  }

  isPlanned(): boolean {
    return this.getCurrentStatus() === 'PLANNED';
  }

  isCompleted(): boolean {
    return this.getCurrentStatus() === 'COMPLETED' || this.getCurrentStatus() === 'REPORT_SUBMITTED';
  }

  isCancelled(): boolean {
    return this.getCurrentStatus() === 'CANCELLED' || this.getCurrentStatus() === 'EXPERT_REFUSED';
  }

  // ================ MÉTHODES UTILITAIRES POUR LE TEMPLATE ================

  shouldShowAcceptRefuse(): boolean {
    return this.isPendingDecision() && !this.expertiseExpired;
  }

  shouldShowExpiredMessage(): boolean {
    return this.isPendingDecision() && this.expertResponseExpired;
  }

  shouldShowReportForm(): boolean {
    // Le formulaire est visible si l'état est PLANNED et que :
    // - Pour ONLINE: immédiatement après acceptation (sauf si deadline expirée)
    // - Pour ONSITE: seulement après que le créneau soit passé (pas de deadline)
    // - Et qu'aucun rapport n'existe déjà

    if (!this.isPlanned() || this.expertise?.reportExists) {
      return false;
    }

    if (this.expertise?.method === 'ONLINE') {
      // ONLINE : vérifier que la deadline n'est pas expirée
      return !this.reportDeadlineExpired;
    }

    if (this.expertise?.method === 'ONSITE') {
      // ONSITE : vérifier si le créneau est passé
      // Pas de vérification de deadline car il n'y en a pas pour ONSITE
      if (this.selectedSlotDate) {
        return this.selectedSlotExpired; // Le créneau est passé
      } else if (this.expertise.confirmedDateTime) {
        const confirmedDate = new Date(this.expertise.confirmedDateTime);
        return confirmedDate.getTime() < Date.now();
      }
      return false;
    }

    return false;
  }

  shouldShowReportDeadlineExpired(): boolean {
    // Afficher l'expiration uniquement pour les expertises ONLINE
    // Les ONSITE n'ont pas de deadline à expirer
    return this.isPlanned() &&
      this.expertise?.method === 'ONLINE' &&
      this.reportDeadlineExpired;
  }

  shouldShowReportCountdown(): boolean {
    // Afficher le compte à rebours uniquement pour les expertises ONLINE
    // Les ONSITE n'ont pas de deadline
    return this.isPlanned() &&
      this.expertise?.method === 'ONLINE' &&
      !this.expertiseExpired &&
      !this.expertise?.reportExists;
  }
  shouldShowWaitingSlotMessage(): boolean {
    // Afficher le message d'attente pour les ONSITE qui n'ont pas encore atteint leur créneau
    return this.isPlanned() &&
      this.expertise?.method === 'ONSITE' &&
      !this.canSubmitReport &&
      !this.expertise?.reportExists;
  }

  // NOUVELLE : Méthode pour obtenir les slots de manière sécurisée
  getSlots(): string[] {
    return this.expertise?.slots || [];
  }

  shouldShowSlots(): boolean {
    return this.expertise?.method === 'ONSITE' &&
      this.shouldShowAcceptRefuse() &&
      this.getSlots().length > 0;
  }

  getSlotStatus(slotDate: string | undefined | null): string {
    if (!slotDate) return 'expired';

    const date = new Date(slotDate);
    const now = new Date();

    if (date.getTime() < now.getTime()) {
      return 'expired';
    } else if (date.getTime() - now.getTime() < 24 * 60 * 60 * 1000) {
      return 'soon';
    } else {
      return 'available';
    }
  }
  // Ajoutez cette méthode après la méthode onImageError
  onFileSelected(event: any): void {
    const fileInput = event.target as HTMLInputElement;
    if (fileInput.files && fileInput.files.length > 0) {
      this.reportForm.document = fileInput.files[0];
    } else {
      this.reportForm.document = null;
    }
  }
}
