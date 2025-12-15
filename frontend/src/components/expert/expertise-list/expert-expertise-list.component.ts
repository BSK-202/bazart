import { Component, OnDestroy, OnInit } from "@angular/core"
import { CommonModule } from "@angular/common"
import { HttpClient } from "@angular/common/http"
import { Router } from "@angular/router"
import { forkJoin } from "rxjs"

interface ExpertiseRequest {
  id: number
  produitId: number
  produitNom: string
  produitImage?: string
  produitImages?: string[]
  vendeurId: number
  vendeurNom: string
  expertId?: number
  expertFullName?: string
  method: "ONLINE" | "ONSITE"
  status: string
  price: number
  slots: string[]
  confirmedDateTime?: string
  location: string
  expertResponseDeadline?: string
  reportSubmissionDeadline?: string
  expertShare?: number
}

@Component({
  selector: "app-expert-expertise-list",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./expert-expertise-list.component.html",
  styleUrls: ["./expert-expertise-list.component.css"],
})
export class ExpertExpertiseListComponent implements OnInit, OnDestroy {
  private readonly API_BASE_URL = "http://localhost:8080"

  expertId!: number
  isLoading = false
  errorMessage = ""

  // TROIS LISTES DISTINCTES
  pendingRequests: ExpertiseRequest[] = []      // En attente d'acceptation
  inProgressRequests: ExpertiseRequest[] = []   // Planifiées / en cours
  completedRequests: ExpertiseRequest[] = []    // Terminées avec rapport

  selectedSlotIndex: Record<number, number | null> = {}
  countdowns: Record<number, string> = {}
  activeTab: 'pending' | 'inProgress' | 'completed' = 'pending';

  private intervalId: any

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const storedUser = localStorage.getItem("userData")
    if (!storedUser) {
      this.errorMessage = "Impossible de récupérer vos informations. Veuillez vous reconnecter."
      return
    }
    const user = JSON.parse(storedUser)
    if (!user.id) {
      this.errorMessage = "Identifiant expert introuvable."
      return
    }
    this.expertId = user.id
    this.loadExpertiseRequests()
  }

  ngOnDestroy(): void {
    if (this.intervalId) clearInterval(this.intervalId)
  }

  trackByRequestId(index: number, request: ExpertiseRequest): number {
    return request.id
  }

  getStatusText(status: string): string {
    const statusMap: { [key: string]: string } = {
      PENDING_EXPERT_DECISION: "En attente d'acceptation",
      PLANNED: "Planifiée",
      EXPERTISE_IN_PROGRESS: "En cours d'expertise",
      EXPERTISED: "Terminée",
      COMPLETED: "Terminée",
      VALIDATED: "Validée",
      CANCELLED: "Annulée",
      REFUSED: "Refusée",
      NO_EXPERTS_AVAILABLE: "Aucun expert disponible",
      ALL_EXPERTS_TRIED: "Tous experts contactés",
      CREATED: "Créée"
    }
    return statusMap[status] || status
  }

  loadExpertiseRequests(): void {
    this.isLoading = true
    this.errorMessage = ""

    console.log('🔄 Chargement des demandes pour expert:', this.expertId);

    this.http.get<{
      pending: ExpertiseRequest[],
      inProgress: ExpertiseRequest[],
      completed: ExpertiseRequest[]
    }>(`${this.API_BASE_URL}/api/expertise/expert/${this.expertId}/all-classified`)
      .subscribe({
        next: (response) => {
          console.log('📥 Réponse brute du backend:');
          console.log('- Pending:', response.pending?.length || 0, 'items');
          console.log('- InProgress:', response.inProgress?.length || 0, 'items');
          console.log('- Completed:', response.completed?.length || 0, 'items');

          // Afficher les IDs de chaque liste
          console.log('IDs pending:', response.pending?.map(r => r.id) || []);
          console.log('IDs inProgress:', response.inProgress?.map(r => r.id) || []);
          console.log('IDs completed:', response.completed?.map(r => r.id) || []);

          // Direct assignment
          this.pendingRequests = response.pending || []
          this.inProgressRequests = response.inProgress || []
          this.completedRequests = response.completed || []

          // Vérifier les doublons ENTRE les listes
          const allIds: number[] = [
            ...this.pendingRequests.map(r => r.id),
            ...this.inProgressRequests.map(r => r.id),
            ...this.completedRequests.map(r => r.id)
          ];

          const uniqueIds = new Set(allIds);
          console.log('🔍 Vérification des doublons:');
          console.log('Total items (avec doublons potentiels):', allIds.length);
          console.log('Items uniques:', uniqueIds.size);

          if (allIds.length !== uniqueIds.size) {
            // Trouver les doublons exacts
            const idCount = new Map<number, number>();
            allIds.forEach(id => {
              idCount.set(id, (idCount.get(id) || 0) + 1);
            });

            const duplicates: number[] = [];
            idCount.forEach((count, id) => {
              if (count > 1) duplicates.push(id);
            });

            console.error('🚨 DOUBLONS DÉTECTÉS! IDs:', duplicates);

            // Afficher où se trouvent les doublons
            duplicates.forEach(dupId => {
              const inPending = this.pendingRequests.find(r => r.id === dupId);
              const inProgress = this.inProgressRequests.find(r => r.id === dupId);
              const inCompleted = this.completedRequests.find(r => r.id === dupId);

              console.log(`ID ${dupId} trouvé dans:`);
              if (inPending) console.log('  - Pending');
              if (inProgress) console.log('  - InProgress');
              if (inCompleted) console.log('  - Completed');
            });
          }

          // Reste du code...
          this.isLoading = false
        },
        error: (err) => {
          console.error("❌ Erreur chargement demandes expertise:", err)
          this.errorMessage = "Erreur lors du chargement des demandes d'expertise."
          this.isLoading = false
        },
      })
  }
  updateCountdowns(): void {
    const now = Date.now()
    this.pendingRequests.forEach((r) => {
      if (!r.expertResponseDeadline) {
        this.countdowns[r.id] = ""
        return
      }
      const diff = new Date(r.expertResponseDeadline).getTime() - now
      if (diff <= 0) {
        this.countdowns[r.id] = "Expiré"
        return
      }
      const d = Math.floor(diff / (1000 * 60 * 60 * 24))
      const h = Math.floor((diff / (1000 * 60 * 60)) % 24)
      const m = Math.floor((diff / (1000 * 60)) % 60)
      const s = Math.floor((diff / 1000) % 60)
      this.countdowns[r.id] = `${d}j ${h}h ${m}m ${s}s`
    })
  }

  getImageUrl(request: ExpertiseRequest): string {
    const img =
      request.produitImages && request.produitImages.length > 0
        ? request.produitImages[0]
        : request.produitImage || ""

    if (!img) return "assets/images/placeholder.jpg"

    // Vérifier si c'est une URL complète ou relative
    if (img.startsWith('http')) {
      return img
    }

    return `${this.API_BASE_URL}/api/produits/images/${request.produitId}/${img}`
  }

  onViewDetails(request: ExpertiseRequest): void {
    this.router.navigate(["/expert/expertise", request.id])
  }

  onChooseSlot(request: ExpertiseRequest, slotIndex: number): void {
    if (request.status !== "PENDING_EXPERT_DECISION") return
    this.selectedSlotIndex[request.id] = slotIndex
  }

  confirmOnsiteExpertise(request: ExpertiseRequest): void {
    const slotIdx = this.selectedSlotIndex[request.id]
    if (request.method === "ONSITE" && slotIdx == null) {
      alert("Veuillez sélectionner un créneau avant de confirmer.")
      return
    }

    this.isLoading = true
    this.errorMessage = ""

    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${request.id}/accept`, null, {
        params: {
          expertId: this.expertId.toString(),
          slotIndex: (slotIdx ?? 0).toString()
        },
      })
      .subscribe({
        next: () => {
          alert("✅ Expertise confirmée avec succès !")
          this.loadExpertiseRequests()
        },
        error: (err) => {
          console.error("Erreur confirmation expertise:", err)
          this.errorMessage = err.error?.message || "Erreur lors de la confirmation de l'expertise."
          this.isLoading = false
        },
      })
  }

  // Nouvelle méthode pour refuser une expertise
  refuseExpertise(request: ExpertiseRequest): void {
    if (!confirm("Voulez-vous vraiment refuser cette expertise ?")) {
      return
    }

    this.isLoading = true
    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${request.id}/refuse`, null, {
        params: { expertId: this.expertId.toString() }
      })
      .subscribe({
        next: () => {
          alert("❌ Expertise refusée.")
          this.loadExpertiseRequests()
        },
        error: (err) => {
          console.error("Erreur refus expertise:", err)
          this.errorMessage = err.error?.message || "Erreur lors du refus de l'expertise."
          this.isLoading = false
        },
      })
  }

  // Méthode pour soumettre un rapport
  submitReport(request: ExpertiseRequest): void {
    // Vérifier si le rapport peut être soumis
    this.http
      .get<boolean>(`${this.API_BASE_URL}/api/expertise/requests/${request.id}/can-submit-report`)
      .subscribe({
        next: (canSubmit) => {
          if (canSubmit) {
            this.router.navigate(["/expert/expertise", request.id, "report"])
          } else {
            alert("Le rapport ne peut pas être soumis pour le moment. " +
              "Pour les expertises sur place, attendez la date du rendez-vous.")
          }
        },
        error: (err) => {
          console.error("Erreur vérification soumission:", err)
          alert("Impossible de vérifier si le rapport peut être soumis.")
        },
      })
  }

  // Méthode pour télécharger le rapport PDF
  downloadReport(request: ExpertiseRequest): void {
    window.open(
      `${this.API_BASE_URL}/api/expertise/requests/${request.id}/report-pdf`,
      '_blank'
    )
  }
  // ✅ NOUVEAU: Méthodes pour changer d'onglet
  setActiveTab(tab: 'pending' | 'inProgress' | 'completed'): void {
    this.activeTab = tab;
  }

  // ✅ NOUVEAU: Méthode pour obtenir le compteur d'onglet
  selectedSlots: any;
  getTabCount(tab: 'pending' | 'inProgress' | 'completed'): number {
    switch (tab) {
      case 'pending': return this.pendingRequests.length;
      case 'inProgress': return this.inProgressRequests.length;
      case 'completed': return this.completedRequests.length;
      default: return 0;
    }
  }

  selectSlot(id: number, i: number) {
    
  }
}
