// expert-details-admin.component.ts - VERSION CORRIGÉE
import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

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
  selector: 'app-expert-details-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './expert-details-admin.component.html',
  styleUrls: ['./expert-details-admin.component.css'],
  providers: [DatePipe]
})
export class ExpertDetailsComponent implements OnInit {

  expert: Expert | null = null;
  loading = true;
  error: string | null = null;
  expertId!: number;
  signatureModalOpen = false;
  currentSignatureIndex = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private datePipe: DatePipe
  ) { }

  ngOnInit(): void {
    this.expertId = Number(this.route.snapshot.paramMap.get('id'));

    if (!this.expertId) {
      this.error = 'ID expert invalide';
      this.loading = false;
      return;
    }

    console.log(`🔍 Chargement des détails pour l'expert ID: ${this.expertId}`);
    this.fetchExpertDetails();
  }

  fetchExpertDetails(): void {
    this.loading = true;
    this.error = null;

    this.http.get<Expert>(`http://localhost:8080/api/experts/${this.expertId}`)
      .subscribe({
        next: (response) => {
          console.log('✅ Détails expert reçus:', response);
          this.expert = response;

          // Formater les données si nécessaire
          if (this.expert.client) {
            this.expert.client.fullName = `${this.expert.client.prenom} ${this.expert.client.nom}`;
          }

          // 🆕 CORRECTION : Vérifier que this.expert n'est pas null
          if (this.expert) {
            console.log('🔍 Signatures disponibles:', {
              signatureImages: this.expert.signatureImages,
              count: this.expert.signatureImages?.length || 0,
              expertId: this.expert.id
            });

            if (this.expert.signatureImages && this.expert.signatureImages.length > 0) {
              this.expert.signatureImages.forEach((signature, index) => {
                const signatureUrl = `http://localhost:8080/api/experts/expert-signatures/${this.expert!.id}/${signature}`;
                console.log(`📸 Signature ${index + 1}:`, {
                  fileName: signature,
                  fullUrl: signatureUrl
                });
              });
            }
          }

          this.loading = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('❌ Erreur chargement détails:', err);
          this.error = 'Erreur lors du chargement des détails de l\'expert';
          this.loading = false;
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/admin/experts-inactifs']);
  }

  onImageError(event: any): void {
    console.error('🖼️ Erreur image de profil:', event.target.src);
    event.target.src = 'assets/default-profile.png';
  }

  // 🆕 CORRECTION : Ajouter le typage et la vérification de null
  onSignatureError(event: any, signature: string): void {
    console.error(`❌ Erreur chargement signature: ${signature}`, {
      targetSrc: event.target.src,
      signature: signature
    });
    event.target.style.display = 'none';
  }

  // 🆕 CORRECTION : Ajouter le typage
  onSignatureLoad(event: any, signature: string): void {
    console.log(`✅ Signature chargée: ${signature}`, event.target.src);
  }

  // 🆕 CORRECTION : Gérer le cas où dateString est undefined
  formatDate(dateString: string | undefined): string {
    if (!dateString) {
      return 'Non spécifiée';
    }
    return this.datePipe.transform(dateString, 'dd/MM/yyyy') || dateString;
  }

  // 🆕 MÉTHODE UTILITAIRE : Construire l'URL de signature en toute sécurité
  getSignatureUrl(signature: string): string {
    if (!this.expert) {
      console.warn('⚠️ Expert non chargé pour construire l URL de signature');
      return '';
    }
    return `http://localhost:8080/api/experts/expert-signatures/${this.expert.id}/${signature}`;
  }
  openSignatureModal(index: number): void {
    this.currentSignatureIndex = index;
    this.signatureModalOpen = true;
    console.log(`📂 Ouverture modal signature: ${index + 1}/${this.expert!.signatureImages.length}`);

    // 🆕 Ajouter les écouteurs de clavier
    document.addEventListener('keydown', this.handleKeyboardNavigation.bind(this));
  }

  closeSignatureModal(): void {
    this.signatureModalOpen = false;

    // 🆕 Supprimer les écouteurs de clavier
    document.removeEventListener('keydown', this.handleKeyboardNavigation.bind(this));
  }

  nextSignature(): void {
    if (this.expert && this.currentSignatureIndex < this.expert.signatureImages.length - 1) {
      this.currentSignatureIndex++;
      console.log(`➡️ Signature suivante: ${this.currentSignatureIndex + 1}`);
    }
  }

  previousSignature(): void {
    if (this.currentSignatureIndex > 0) {
      this.currentSignatureIndex--;
      console.log(`⬅️ Signature précédente: ${this.currentSignatureIndex + 1}`);
    }
  }

  handleKeyboardNavigation(event: KeyboardEvent): void {
    if (!this.signatureModalOpen) return;

    switch (event.key) {
      case 'ArrowLeft':
        event.preventDefault();
        this.previousSignature();
        break;
      case 'ArrowRight':
        event.preventDefault();
        this.nextSignature();
        break;
      case 'Escape':
        event.preventDefault();
        this.closeSignatureModal();
        break;
    }
  }

  getCurrentSignatureUrl(): string {
    if (!this.expert || !this.expert.signatureImages[this.currentSignatureIndex]) {
      return '';
    }
    return this.getSignatureUrl(this.expert.signatureImages[this.currentSignatureIndex]);
  }
  onModalSignatureError(event: any): void {
    console.error('❌ Erreur chargement signature dans modal:', event.target.src);
    event.target.src = 'assets/default-signature.png'; // Image de remplacement
  }


  activateExpert(): void {
    if (!this.expert) return;

    const url = `http://localhost:8080/api/experts/${this.expert.id}/activate`;

    if (!confirm("Voulez-vous vraiment valider cet expert ?")) return;

    this.http.put(url, {})
      .subscribe({
        next: (res) => {
          console.log("✅ Expert activé :", res);
          alert("L'expert a été validé avec succès !");

          // Mettre à jour l’UI sans recharger la page
          this.expert!.active = true;
        },
        error: (err) => {
          console.error("❌ Erreur activation:", err);
          alert("Erreur lors de l’activation de l’expert.");
        }
      });
  }

}
