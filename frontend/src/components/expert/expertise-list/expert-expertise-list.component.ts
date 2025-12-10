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

  loadPendingRequests(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.http
      .get<ExpertiseRequest[]>(`${this.API_BASE_URL}/api/expertise/expert/${this.expertId}/assigned`)
      .subscribe({
        next: (data) => {
          this.requests = data || [];
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

  acceptRequest(req: ExpertiseRequest): void {
    const params: any = { expertId: this.expertId };
    if (req.method === 'ONSITE') {
      const slotIdx = this.selectedSlotIndex[req.id];
      if (!slotIdx) {
        alert("Veuillez sélectionner un créneau avant d'accepter.");
        return;
      }
      params.slotIndex = slotIdx;
    }

    this.isLoading = true;
    this.http
      .post(`${this.API_BASE_URL}/api/expertise/requests/${req.id}/accept`, null, { params })
      .subscribe({
        next: () => {
          alert('✅ Expertise acceptée !');
          this.loadPendingRequests();
        },
        error: () => {
          this.isLoading = false;
          alert("Erreur lors de l'acceptation de la demande.");
        }
      });
  }

  refuseRequest(req: ExpertiseRequest): void {
    this.isLoading = true;
    this.http
      .post(
        `${this.API_BASE_URL}/api/expertise/requests/${req.id}/refuse`,
        null,
        { params: { expertId: this.expertId } }
      )
      .subscribe({
        next: () => {
          alert('Demande refusée, elle sera réassignée.');
          this.loadPendingRequests();
        },
        error: () => {
          this.isLoading = false;
          alert('Erreur lors du refus de la demande.');
        }
      });
  }

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
