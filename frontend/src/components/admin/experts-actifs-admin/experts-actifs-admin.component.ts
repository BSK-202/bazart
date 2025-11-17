import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface Client {
  idclient: number;
  nom: string;
  prenom: string;
  email: string;
  tel: string;
  pays: string;
  ville: string;
  photoprofil?: string;
  dateinscription: string;
  enabled: boolean;
  roles: any[];
  fullName?: string;
}

interface Domaine {
  idDomaine: number;
  nomDomaine: string;
  description: string;
  image: string;
}

interface Categorie {
  idCategorie: number;
  nomCategorie: string;
  description: string;
  image: string;
  domaine: Domaine;
}

interface Expert {
  id: number;
  biography: string;
  anneesExperience: number;
  domaine: Domaine;
  categories: Categorie[];
  langues: string[];
  dateEmbauche: string | null;
  signatureImages: string[];
  nombreProduitsExpertise: number;
  client: Client;
  active: boolean;
}

@Component({
  selector: 'app-experts-actifs-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './experts-actifs-admin.component.html',
  styleUrls: ['./experts-actifs-admin.component.css']
})
export class ExpertsActifsAdminComponent implements OnInit {

  experts: Expert[] = [];
  loading = true;
  error: string | null = null;

  private readonly BACKEND_BASE_URL = 'http://localhost:8080';

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.fetchActiveExperts();
  }

  fetchActiveExperts() {
    this.loading = true;
    this.error = null;

    this.http.get<Expert[]>(`${this.BACKEND_BASE_URL}/api/experts/actifs`).subscribe({
      next: (response) => {
        this.experts = response.map(expert => {
          if (expert.client) {
            // Construire le nom complet
            expert.client.fullName = `${expert.client.prenom || ''} ${expert.client.nom || ''}`;

            // Si aucune photo, fallback
            if (!expert.client.photoprofil || expert.client.photoprofil.trim() === '') {
              expert.client.photoprofil = 'assets/default-profile.png';
            }
          }
          return expert;
        });
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Erreur chargement experts:', err);
        this.error = 'Erreur lors du chargement des experts actifs.';
        this.loading = false;
      }
    });
  }

  getPhotoUrl(expert: Expert): string {
    if (expert.client?.photoprofil && !expert.client.photoprofil.startsWith('assets/')) {
      // Construire URL complète pour backend
      return `${this.BACKEND_BASE_URL}/api/clients/images/${expert.client.photoprofil}`;
    }
    // Fallback
    return expert.client?.photoprofil || 'assets/default-profile.png';
  }

  viewExpertDetails(expertId: number) {
    this.router.navigate(['/admin/experts-inactifs', expertId]);
  }

  onImageError(event: any, expert: Expert) {
    console.error(`Erreur image pour expert ${expert.id}:`, event.target.src);
    event.target.src = 'assets/default-profile.png';
  }

  onImageLoad(event: any, expert: Expert) {
    console.log(`Image chargée pour expert ${expert.id}:`, event.target.src);
  }
}
