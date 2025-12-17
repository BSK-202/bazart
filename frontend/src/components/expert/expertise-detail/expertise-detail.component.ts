import { Component, OnInit, OnDestroy } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpClient } from "@angular/common/http";

export enum ProductCondition {
  EXCELLENT = "EXCELLENT",
  VERY_GOOD = "VERY_GOOD",
  GOOD = "GOOD",
  FAIR = "FAIR",
  POOR = "POOR",
}

export enum AuthenticityLevel {
  AUTHENTIC = "AUTHENTIC",
  PROBABLE = "PROBABLE",
  UNKNOWN = "UNKNOWN",
  FAKE = "FAKE",
}

export enum ExpertiseRecommendation {
  AUTHORISE_AUCTION = "AUTHORISE_AUCTION",
  REFUSE = "REFUSE",
  REQUEST_MORE_INFO = "REQUEST_MORE_INFO",
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
  method: "ONLINE" | "ONSITE";
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
  selector: "app-expert-expertise-detail",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./expertise-detail.component.html",
  styleUrls: ["./expertise-detail.component.css"],
})
export class ExpertiseDetailComponent implements OnInit, OnDestroy {
  private readonly API_BASE_URL = "http://localhost:8080";
  private readonly PLACEHOLDER = "assets/images/placeholder.jpg";

  expertiseId!: number;
  expertise: ExpertiseRequest | null = null;
  produit: Produit | null = null;
  vendeur: Seller | null = null;
  isLoading = true;
  error = "";
  selectedImage = 0;
  showDefaultAvatar = false;

  private timers: any[] = [];
  expertResponseExpired = false;
  reportDeadlineExpired = false;
  selectedSlotExpired = false;
  reportSubmissionCountdown = "";
  private reportCountdownInterval: any;

  lightboxOpen = false;
  lightboxIndex = 0;

  // ========== FORMULAIRE DE RAPPORT AMÉLIORÉ ==========
  reportForm = {
    // AUTHENTICITÉ
    authenticityLevel: "",
    authenticityConfidence: null as number | null,
    authenticityProof: "",

    // ÉTAT DU PRODUIT
    productCondition: "",
    conditionScore: null as number | null,
    visualCondition: "",
    functionalCondition: "",
    conformityDescription: null as boolean | null,

    // DESCRIPTION DÉTAILLÉE
    detailedDescription: "",
    positivePoints: "",
    negativePoints: "",
    testsPerformed: "",

    // ESTIMATION DE VALEUR
    estimatedMinPrice: null as number | null,
    estimatedMaxPrice: null as number | null,
    recommendedStartPrice: null as number | null,
    priceJustification: "",

    // RECOMMANDATIONS
    recommendation: "",
    saleRecommendations: "",
    buyerWarnings: "",

    // COMMENTAIRES
    commentsPublic: "",
    commentsInternal: "",
  };

  selectedSlotIndex: number | null = null;
  selectedSlotDate: Date | null = null;

  productConditions = Object.values(ProductCondition);
  authenticityLevels = Object.values(AuthenticityLevel);
  recommendations = Object.values(ExpertiseRecommendation);

  // Section du formulaire actuellement affichée
  currentSection: 'authenticity' | 'condition' | 'description' | 'estimation' | 'recommendations' = 'authenticity';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.expertiseId = Number(this.route.snapshot.paramMap.get("expertiseId"));
    this.loadExpertiseDetails();
  }

  ngOnDestroy(): void {
    this.timers.forEach((timer) => clearInterval(timer));
    if (this.reportCountdownInterval) clearInterval(this.reportCountdownInterval);
  }

  loadExpertiseDetails(): void {
    this.isLoading = true;
    this.http.get<any>(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/detail`).subscribe({
      next: (data) => {
        this.processExpertiseData(data);
      },
      error: (err) => {
        console.error("Erreur chargement expertise:", err);
        this.error = "Erreur lors du chargement des détails de l'expertise.";
        this.isLoading = false;
      },
    });
  }

  private processExpertiseData(data: any): void {
    this.expertise = data;

    const images = this.processImages(data);

    this.produit = {
      id: data.produitId,
      nom: data.produitNom,
      description: data.produitDescription || "",
      etat: data.produitEtat || "",
      prixDebut: data.prixDebut ?? 0,
      prixFin: data.prixFin ?? null,
      acheteurNom: data.acheteurNom ?? null,
      vendeurNom: data.vendeurNom || "",
      vendeurId: data.vendeurId,
      categorieNom: data.produitCategorie || "",
      images,
      aExpertise: true,
      datePublication: data.datePublication,
      dateenchere: data.dateenchere,
      dureeEnchereJours: data.dureeEnchereJours,
    };

    this.vendeur = {
      idClient: data.vendeurId,
      nom: data.vendeurNom || "",
      prenom: data.vendeurPrenom || "",
      ville: data.vendeurVille,
      pays: data.vendeurPays,
      tel: data.vendeurTel,
    };

    this.loadAdditionalImages(data.produitId);
    this.initializeStateVerifications();

    this.isLoading = false;
  }

  private processImages(data: any): string[] {
    const images = (
      data.produitImages && data.produitImages.length > 0
        ? data.produitImages
        : data.produitImage
          ? [data.produitImage]
          : []
    ).map((img: string) => this.getProduitImageUrl(data.produitId, img));

    if (images.length === 0) {
      images.push(this.PLACEHOLDER);
    }

    return images;
  }

  private loadAdditionalImages(produitId: number): void {
    this.http.get<string[]>(`${this.API_BASE_URL}/api/produits/${produitId}/images`).subscribe({
      next: (files) => {
        if (!files || files.length === 0 || !this.produit) return;

        const mapped = files.map((f: string) => this.getProduitImageUrl(produitId, f));
        const set = new Set<string>([...this.produit.images, ...mapped]);
        this.produit.images = Array.from(set);

        if (this.selectedImage >= this.produit.images.length) {
          this.selectedImage = 0;
        }
        if (this.lightboxIndex >= this.produit.images.length) {
          this.lightboxIndex = 0;
        }
      },
      error: () => {},
    });
  }

  private initializeStateVerifications(): void {
    if (!this.expertise) return;

    this.clearAllTimers();
    this.checkAllExpirations();

    if (this.isPendingDecision() && this.expertise.expertResponseDeadline) {
      this.startExpertResponseTimer();
    }

    if (this.isPlanned()) {
      if (this.expertise.method === "ONLINE" && this.expertise.reportSubmissionDeadline) {
        this.startReportCountdownTimer();
      } else if (this.expertise.method === "ONSITE") {
        this.checkOnsiteSlotAvailability();
      }
    }
  }

  private checkAllExpirations(): void {
    if (!this.expertise) return;

    this.expertResponseExpired = false;
    this.reportDeadlineExpired = false;
    this.selectedSlotExpired = false;

    if (this.isPendingDecision() && this.expertise.expertResponseDeadline) {
      this.expertResponseExpired = this.isDateExpired(this.expertise.expertResponseDeadline);
    }

    if (this.isPlanned() && this.expertise.method === "ONLINE" && this.expertise.reportSubmissionDeadline) {
      this.reportDeadlineExpired = this.isDateExpired(this.expertise.reportSubmissionDeadline);

      if (!this.reportDeadlineExpired) {
        this.reportSubmissionCountdown = this.formatCountdown(this.expertise.reportSubmissionDeadline);
      } else {
        this.reportSubmissionCountdown = "EXPIRÉ";
      }
    }

    if (this.isPlanned() && this.expertise.method === "ONSITE") {
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

      if (wasExpired !== this.expertResponseExpired) {
        this.checkAllExpirations();
      }
    }, 1000);

    this.timers.push(timer);
  }

  private startReportCountdownTimer(): void {
    if (!this.expertise?.reportSubmissionDeadline) return;

    if (this.reportCountdownInterval) {
      clearInterval(this.reportCountdownInterval);
    }

    this.reportCountdownInterval = setInterval(() => {
      if (!this.expertise?.reportSubmissionDeadline) return;

      const wasExpired = this.reportDeadlineExpired;
      this.reportDeadlineExpired = this.isDateExpired(this.expertise.reportSubmissionDeadline);

      if (!this.reportDeadlineExpired) {
        this.reportSubmissionCountdown = this.formatCountdown(this.expertise.reportSubmissionDeadline);
      } else {
        this.reportSubmissionCountdown = "EXPIRÉ";
        clearInterval(this.reportCountdownInterval);
      }

      if (wasExpired !== this.reportDeadlineExpired) {
        this.checkAllExpirations();
      }
    }, 1000);
  }

  private checkOnsiteSlotAvailability(): void {
    const timer = setInterval(() => {
      if (!this.expertise || this.expertise.method !== "ONSITE") return;

      let slotDate: Date | null = null;

      if (this.selectedSlotDate) {
        slotDate = this.selectedSlotDate;
      } else if (this.expertise.confirmedDateTime) {
        slotDate = new Date(this.expertise.confirmedDateTime);
      }

      if (slotDate) {
        const wasExpired = this.selectedSlotExpired;
        this.selectedSlotExpired = slotDate.getTime() < Date.now();

        if (wasExpired !== this.selectedSlotExpired) {
          this.checkAllExpirations();
        }
      }
    }, 1000);

    this.timers.push(timer);
  }

  private clearAllTimers(): void {
    this.timers.forEach((timer) => clearInterval(timer));
    this.timers = [];

    if (this.reportCountdownInterval) {
      clearInterval(this.reportCountdownInterval);
      this.reportCountdownInterval = null;
    }
  }

  isPendingDecision(): boolean {
    return this.expertise?.status === "PENDING_EXPERT_DECISION";
  }

  isPlanned(): boolean {
    return this.expertise?.status === "PLANNED";
  }

  isCompleted(): boolean {
    return (
      this.expertise?.status === "COMPLETED" ||
      this.expertise?.status === "EXPERTISED" ||
      this.expertise?.reportExists === true
    );
  }

  isCancelled(): boolean {
    return (
      this.expertise?.status === "CANCELLED" ||
      this.expertise?.status === "ALL_EXPERTS_TRIED" ||
      this.expertise?.status === "NO_EXPERTS_AVAILABLE"
    );
  }

  isExpired(): boolean {
    if (this.isPendingDecision()) {
      return this.expertResponseExpired;
    } else if (this.isPlanned() && this.expertise?.method === "ONLINE") {
      return this.reportDeadlineExpired;
    }
    return false;
  }

  shouldShowAcceptRefuse(): boolean {
    return this.isPendingDecision() && !this.expertResponseExpired;
  }

  shouldShowExpiredMessage(): boolean {
    return this.isPendingDecision() && this.expertResponseExpired;
  }

  shouldShowSlots(): boolean {
    return (
      this.isPendingDecision() &&
      !this.expertResponseExpired &&
      this.expertise?.method === "ONSITE" &&
      this.getSlots().length > 0
    );
  }

  shouldShowWaitingSlotMessage(): boolean {
    return (
      this.isPlanned() &&
      this.expertise?.method === "ONSITE" &&
      !this.selectedSlotExpired &&
      !this.expertise?.reportExists
    );
  }

  shouldShowReportCountdown(): boolean {
    return (
      this.isPlanned() &&
      this.expertise?.method === "ONLINE" &&
      !this.reportDeadlineExpired &&
      !this.expertise?.reportExists
    );
  }

  shouldShowReportDeadlineExpired(): boolean {
    return (
      this.isPlanned() &&
      this.expertise?.method === "ONLINE" &&
      this.reportDeadlineExpired &&
      !this.expertise?.reportExists
    );
  }

  shouldShowReportForm(): boolean {
    if (!this.isPlanned() || this.expertise?.reportExists) {
      return false;
    }

    if (this.expertise?.method === "ONLINE") {
      return !this.reportDeadlineExpired;
    }

    if (this.expertise?.method === "ONSITE") {
      return this.selectedSlotExpired;
    }

    return false;
  }

  chooseSlot(index: number): void {
    if (!this.isPendingDecision() || this.expertResponseExpired) return;

    this.selectedSlotIndex = index + 1;

    const slots = this.getSlots();
    if (slots && slots[index]) {
      this.selectedSlotDate = new Date(slots[index]);
      this.selectedSlotExpired = this.isDateExpired(slots[index]);

      if (this.selectedSlotExpired) {
        console.warn("Créneau déjà expiré lors de la sélection");
      }
    }
  }

  onAcceptRequest(): void {
    if (!this.expertise || this.expertResponseExpired) {
      alert("❌ DÉLAI DÉPASSÉ - La période pour accepter cette demande est expirée.");
      return;
    }

    if (this.expertise.method === "ONSITE" && !this.selectedSlotIndex) {
      alert("Sélectionnez un créneau !");
      return;
    }

    const params: any = { expertId: this.getCurrentExpertId() };
    if (this.expertise.method === "ONSITE") {
      params.slotIndex = this.selectedSlotIndex;
    }

    this.isLoading = true;

    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/accept`, null, { params })
      .subscribe({
        next: (response: any) => {
          if (response) {
            Object.assign(this.expertise!, response);
          }

          this.expertResponseExpired = false;
          this.selectedSlotExpired = false;

          this.initializeStateVerifications();

          alert("✅ Demande acceptée avec succès !");
          this.isLoading = false;
        },
        error: (err) => {
          console.error("Erreur acceptation:", err);
          this.isLoading = false;
          alert("Erreur lors de l'acceptation: " + (err.error?.message || err.message || "Erreur inconnue"));
        },
      });
  }

  onRefuseRequest(): void {
    if (!this.expertise || this.expertResponseExpired) {
      alert("❌ DÉLAI DÉPASSÉ - La période pour refuser cette demande est expirée.");
      return;
    }

    this.isLoading = true;
    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/refuse`, null, {
        params: { expertId: this.getCurrentExpertId() },
      })
      .subscribe({
        next: () => {
          this.router.navigate(["/expert/expertise"]);
        },
        error: (err) => {
          console.error("Erreur refus:", err);
          this.isLoading = false;
          alert("Erreur lors du refus: " + (err.error?.message || err.message || "Erreur inconnue"));
        },
      });
  }

  onSubmitReport(): void {
    if (!this.expertise) return;

    if (!this.canSubmitReport()) {
      alert("Vous ne pouvez pas soumettre le rapport pour le moment.");
      return;
    }

    if (!this.reportForm.authenticityLevel || !this.reportForm.productCondition || !this.reportForm.recommendation) {
      alert("Veuillez renseigner tous les champs obligatoires (Authenticité, État, Recommandation).");
      return;
    }

    if (
      this.reportForm.estimatedMaxPrice &&
      this.reportForm.estimatedMinPrice &&
      this.reportForm.estimatedMaxPrice < this.reportForm.estimatedMinPrice
    ) {
      alert("Le prix maximum estimé doit être supérieur ou égal au prix minimum.");
      return;
    }

    const formData = new FormData();

    // AUTHENTICITÉ
    formData.append("authenticityLevel", this.reportForm.authenticityLevel);
    if (this.reportForm.authenticityConfidence != null) {
      formData.append("authenticityConfidence", String(this.reportForm.authenticityConfidence));
    }
    formData.append("authenticityProof", this.reportForm.authenticityProof || "");

    // ÉTAT DU PRODUIT
    formData.append("productCondition", this.reportForm.productCondition);
    if (this.reportForm.conditionScore != null) {
      formData.append("conditionScore", String(this.reportForm.conditionScore));
    }
    formData.append("visualCondition", this.reportForm.visualCondition || "");
    formData.append("functionalCondition", this.reportForm.functionalCondition || "");
    if (this.reportForm.conformityDescription != null) {
      formData.append("conformityDescription", String(this.reportForm.conformityDescription));
    }

    // DESCRIPTION DÉTAILLÉE
    formData.append("detailedDescription", this.reportForm.detailedDescription || "");
    formData.append("positivePoints", this.reportForm.positivePoints || "");
    formData.append("negativePoints", this.reportForm.negativePoints || "");
    formData.append("testsPerformed", this.reportForm.testsPerformed || "");

    // ESTIMATION DE VALEUR
    if (this.reportForm.estimatedMinPrice != null) {
      formData.append("estimatedMinPrice", String(this.reportForm.estimatedMinPrice));
    }
    if (this.reportForm.estimatedMaxPrice != null) {
      formData.append("estimatedMaxPrice", String(this.reportForm.estimatedMaxPrice));
    }
    if (this.reportForm.recommendedStartPrice != null) {
      formData.append("recommendedStartPrice", String(this.reportForm.recommendedStartPrice));
    }
    formData.append("priceJustification", this.reportForm.priceJustification || "");

    // RECOMMANDATIONS
    formData.append("recommendation", this.reportForm.recommendation);
    formData.append("saleRecommendations", this.reportForm.saleRecommendations || "");
    formData.append("buyerWarnings", this.reportForm.buyerWarnings || "");

    // COMMENTAIRES
    formData.append("commentsPublic", this.reportForm.commentsPublic || "");
    formData.append("commentsInternal", this.reportForm.commentsInternal || "");

    const expertId = this.getCurrentExpertId();

    this.isLoading = true;

    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${this.expertiseId}/submit-report`, formData, {
        params: { expertId },
      })
      .subscribe({
        next: () => {
          alert("✅ Rapport soumis avec succès !");
          this.router.navigate(["/expert/expertise"]);
        },
        error: (err) => {
          console.error("Erreur soumission rapport:", err);
          this.isLoading = false;
          alert("❌ Erreur lors de l'envoi du rapport: " + (err.error?.message || err.message || "Erreur inconnue"));
        },
      });
  }

  canSubmitReport(): boolean {
    return this.shouldShowReportForm();
  }

  // Navigation entre sections du formulaire
  goToSection(section: 'authenticity' | 'condition' | 'description' | 'estimation' | 'recommendations'): void {
    this.currentSection = section;
  }

  private isDateExpired(dateString: string): boolean {
    if (!dateString) return false;
    try {
      const date = new Date(dateString);
      return date.getTime() < Date.now();
    } catch (e) {
      console.error("Erreur parsing date:", dateString, e);
      return true;
    }
  }

  formatCountdown(deadline: string): string {
    if (!deadline) return "";

    const now = new Date().getTime();
    const end = new Date(deadline).getTime();
    const diff = end - now;

    if (diff <= 0) return "EXPIRÉ";

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
    if (!slotDate) return "expired";

    try {
      const date = new Date(slotDate);
      const now = new Date();

      if (date.getTime() < now.getTime()) {
        return "expired";
      } else if (date.getTime() - now.getTime() < 24 * 60 * 60 * 1000) {
        return "soon";
      } else {
        return "available";
      }
    } catch (e) {
      return "expired";
    }
  }

  getCurrentExpertId(): number {
    try {
      const user = JSON.parse(localStorage.getItem("userData") || "{}");
      return user.id || 0;
    } catch (e) {
      console.error("Erreur récupération expert ID:", e);
      return 0;
    }
  }

  getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === "") return this.PLACEHOLDER;
    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  getProfileImageUrl(userId: number): string {
    this.showDefaultAvatar = false;
    return `${this.API_BASE_URL}/api/clients/images/${userId}.jpg`;
  }

  onProfileImageError(event: any): void {
    this.showDefaultAvatar = true;
    const imgElement = event.target as HTMLImageElement;
    imgElement.style.display = "none";
  }

  onImageError(event: any): void {
    const img = event.target as HTMLImageElement;
    if (img) {
      img.src = this.PLACEHOLDER;
      img.onerror = null;
    }
  }

  getInitials(name: string | undefined): string {
    if (!name) return "??";
    const parts = name.trim().split(" ");
    return (parts[0][0] + (parts[1]?.[0] ?? "")).toUpperCase();
  }

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

  selectImage(index: number): void {
    this.selectedImage = index;
  }

  getCurrentStatus(): string {
    return this.expertise?.status?.toUpperCase() || "";
  }

  // Helper pour obtenir le label des enums
  getConditionLabel(value: string): string {
    const labels: { [key: string]: string } = {
      'EXCELLENT': 'Excellent',
      'VERY_GOOD': 'Très bon',
      'GOOD': 'Bon',
      'FAIR': 'Correct',
      'POOR': 'Mauvais'
    };
    return labels[value] || value;
  }

  getAuthenticityLabel(value: string): string {
    const labels: { [key: string]: string } = {
      'AUTHENTIC': 'Authentique',
      'PROBABLE': 'Probablement authentique',
      'UNKNOWN': 'Inconnu',
      'FAKE': 'Contrefaçon'
    };
    return labels[value] || value;
  }

  getRecommendationLabel(value: string): string {
    const labels: { [key: string]: string } = {
      'AUTHORISE_AUCTION': 'Autoriser la vente',
      'REFUSE': 'Refuser',
      'REQUEST_MORE_INFO': 'Demander plus d\'informations'
    };
    return labels[value] || value;
  }
}
