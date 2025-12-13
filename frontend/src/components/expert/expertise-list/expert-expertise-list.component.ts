import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

interface ExpertiseRequest {
  id: number;
  produitId: number;
  produitNom: string;
  produitImage?: string;
  produitImages?: string[];
  vendeurId: number;
  vendeurNom: string;
  expertId?: number;
  expertFullName?: string;
  method: 'ONLINE' | 'ONSITE';
  status: string;
  price: number;
  slots: string[];
  confirmedDateTime?: string;
  location: string;
  expertResponseDeadline?: string;
  reportSubmissionDeadline?: string;
  expertShare?: number;
}

@Component({
  selector: 'app-expert-expertise-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './expert-expertise-list.component.html',
  styleUrls: ['./expert-expertise-list.component.css']
})
export class ExpertExpertiseListComponent implements OnInit, OnDestroy {
  private readonly API_BASE_URL = 'http://localhost:8080';

  expertId!: number;
  isLoading = false;
  errorMessage = '';

  requests: ExpertiseRequest[] = [];
  selectedSlotIndex: Record<number, number | null> = {};
  countdowns: Record<number, string> = {};

  // Variables de pagination
  currentPage: number = 1;
  itemsPerPage: number = 6; // Nombre d'éléments par page
  totalPages: number = 1;

  private intervalId: any;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    const storedUser = localStorage.getItem('userData');
    if (!storedUser) {
      this.errorMessage = 'Impossible de récupérer vos informations. Veuillez vous reconnecter.';
      return;
    }
    const user = JSON.parse(storedUser);
    if (!user.id) {
      this.errorMessage = "Identifiant expert introuvable.";
      return;
    }
    this.expertId = user.id;
    this.loadPendingRequests();
  }

  ngOnDestroy(): void {
    if (this.intervalId) clearInterval(this.intervalId);
  }

  // Méthode pour obtenir les éléments de la page courante
  getCurrentPageRequests(): ExpertiseRequest[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.requests.slice(startIndex, endIndex);
  }

  // Méthode pour aller à une page spécifique
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      // Optionnel: Scroll vers le haut de la page
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  // Méthode pour calculer le nombre total de pages
  updatePagination(): void {
    this.totalPages = Math.ceil(this.requests.length / this.itemsPerPage);
    // Si la page courante est au-delà du nombre total de pages, revenir à la première
    if (this.currentPage > this.totalPages && this.totalPages > 0) {
      this.currentPage = 1;
    }
  }

  // Méthode pour obtenir le texte du statut
  getStatusText(status: string): string {
    const statusMap: {[key: string]: string} = {
      'PENDING_EXPERT_DECISION': 'En attente',
      'EXPERT_ACCEPTED': 'Accepté',
      'EXPERT_REFUSED': 'Refusé',
      'COMPLETED': 'Terminé',
      'CANCELLED': 'Annulé',
      'EXPERTISE_IN_PROGRESS': 'En cours',
      'REPORT_SUBMITTED': 'Rapport soumis'
    };
    return statusMap[status] || status;
  }

  loadPendingRequests(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.http
      .get<ExpertiseRequest[]>(`${this.API_BASE_URL}/api/expertise/expert/${this.expertId}/assigned`)
      .subscribe({
        next: (data) => {
          this.requests = data || [];
          this.updatePagination(); // Mettre à jour la pagination
          this.requests.forEach((r) => (this.selectedSlotIndex[r.id] = null));

          // countdown uniquement pour les PENDING
          if (this.intervalId) clearInterval(this.intervalId);
          const hasPending = this.requests.some(r => r.status === 'PENDING_EXPERT_DECISION');
          if (hasPending) {
            this.updateCountdowns();
            this.intervalId = setInterval(() => this.updateCountdowns(), 1000);
          }
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Erreur chargement demandes expertise:', err);
          this.errorMessage = "Erreur lors du chargement des demandes d'expertise.";
          this.isLoading = false;
        }
      });
  }

  updateCountdowns(): void {
    const now = Date.now();
    this.requests.forEach((r) => {
      if (!r.expertResponseDeadline) {
        this.countdowns[r.id] = '';
        return;
      }
      const diff = new Date(r.expertResponseDeadline).getTime() - now;
      if (diff <= 0) {
        this.countdowns[r.id] = 'Expiré';
        return;
      }
      const d = Math.floor(diff / (1000 * 60 * 60 * 24));
      const h = Math.floor((diff / (1000 * 60 * 60)) % 24);
      const m = Math.floor((diff / (1000 * 60)) % 60);
      const s = Math.floor((diff / 1000) % 60);
      this.countdowns[r.id] = `${d}j ${h}h ${m}m ${s}s`;
    });
  }

  formatCountdown(deadline: string): string {
    if (!deadline) return '';
    const now = Date.now();
    const diff = new Date(deadline).getTime() - now;
    if (diff <= 0) return 'Expiré';
    const d = Math.floor(diff / (1000 * 60 * 60 * 24));
    const h = Math.floor((diff / (1000 * 60 * 60)) % 24);
    const m = Math.floor((diff / (1000 * 60)) % 60);
    const s = Math.floor((diff / 1000) % 60);
    return `${d}j ${h}h ${m}m ${s}s`;
  }

  getImageUrl(request: ExpertiseRequest): string {
    const img =
      request.produitImages && request.produitImages.length > 0
        ? request.produitImages[0]
        : request.produitImage || '';
    if (!img) return 'assets/images/placeholder.jpg';
    return `${this.API_BASE_URL}/api/produits/images/${request.produitId}/${img}`;
  }

  onViewDetails(request: ExpertiseRequest): void {
    this.router.navigate(['/expert/expertise', request.id]);
  }

  onChooseSlot(request: ExpertiseRequest, slotIndex: number): void {
    if (request.status !== 'PENDING_EXPERT_DECISION') return;
    this.selectedSlotIndex[request.id] = slotIndex;
  }

  // Note: Les méthodes acceptRequest() et refuseRequest() sont gardées pour référence
  // mais ne sont plus utilisées dans le template HTML mis à jour

  confirmOnsiteExpertise(request: ExpertiseRequest): void {
    const slotIdx = this.selectedSlotIndex[request.id];
    if (slotIdx == null) {
      alert('Veuillez sélectionner un créneau avant de confirmer.');
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.http
      .post(
        `${this.API_BASE_URL}/api/expertise/requests/${request.id}/accept`,
        null,
        { params: { expertId: this.expertId, slotIndex: slotIdx } }
      )
      .subscribe({
        next: () => {
          alert('✅ Expertise sur place confirmée avec succès !');
          this.loadPendingRequests();
        },
        error: (err) => {
          console.error('Erreur confirmation expertise:', err);
          this.errorMessage = err.error?.message || "Erreur lors de la confirmation de l'expertise.";
          this.isLoading = false;
        }
      });
  }
}
