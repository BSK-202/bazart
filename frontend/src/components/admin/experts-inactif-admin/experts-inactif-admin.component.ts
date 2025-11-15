import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router'; // 🆕 IMPORT AJOUTÉ

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
  selector: 'app-experts-inactifs-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './experts-inactif-admin.component.html',
  styleUrls: ['./experts-inactif-admin.component.css']
})
export class ExpertsInactifsComponent implements OnInit {

  experts: Expert[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private http: HttpClient,
    private router: Router // 🆕 INJECTION DU ROUTER
  ) { }

  ngOnInit(): void {
    console.log('🔄 Initialisation du composant Experts Inactifs');
    this.fetchInactiveExperts();
  }

  fetchInactiveExperts() {
    this.loading = true;
    this.error = null;

    console.log('🚀 Début de fetchInactiveExperts()');

    this.http.get<Expert[]>('http://localhost:8080/api/experts/inactifs/verifies').subscribe({
      next: (response) => {
        console.log('✅ SUCCÈS - Données reçues:', response);

        this.experts = response.map((expert, index) => {
          console.log(`🔍 Expert ${index} - photoprofil:`, expert.client?.photoprofil);

          if (expert.client) {
            // Créer le nom complet
            expert.client.fullName = `${expert.client.prenom || ''} ${expert.client.nom || ''}`;

            // Vérifier si l'URL est déjà construite par le backend
            if (!expert.client.photoprofil || expert.client.photoprofil.trim() === '') {
              console.log(`❌ Expert ${expert.id}: Aucune photoprofil définie`);
              expert.client.photoprofil = 'assets/default-profile.png';
            } else {
              console.log(`🖼️ Expert ${expert.id}: URL déjà construite par le backend`);
            }
          }

          return expert;
        });

        console.log('📋 Liste finale des experts:', this.experts);
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('❌ ERREUR HTTP:', err);
        this.error = 'Erreur de chargement des experts';
        this.loading = false;
      }
    });
  }

  // Dans experts-inactif-admin.component.ts
  viewExpertDetails(expertId: number): void {

     this.router.navigate(['/expert-details-admin', expertId])
      .then(success => {
        if (success) {
          console.log(` Navigation réussie vers expert-details-admin/${expertId}`);
        } else {
          console.error(`Navigation échouée`);
        }
      })
      .catch(error => {
        console.error(`Erreur navigation:`, error);
      });
  }
  onImageError(event: any, expert: Expert) {
    console.error(`🖼️ ERREUR image pour expert ${expert.id}:`, event.target.src);
    event.target.src = 'assets/default-profile.png';
  }

  onImageLoad(event: any, expert: Expert) {
    console.log(`✅ Image chargée avec succès pour expert ${expert.id}:`, event.target.src);
  }
}
