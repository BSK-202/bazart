import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { AuthService } from '../../services/auth.service';

interface RegisterResponse {
  idclient: number;
  nom: string;
  prenom: string;
  email: string;
  tel: string;
  pays: string;
  ville: string;
  photoprofil: string;
}

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent implements OnInit {

  // --- Champs Client ---
  nom = '';
  prenom = '';
  email = '';
  tel = '';
  pays = '';
  ville = '';
  photoprofil = '';
  password = '';
  confirmPassword = '';
  error = '';
  loading = false;
  selectedFile: File | null = null;

  // --- Type utilisateur ---
  userType: 'client' | 'expert' = 'client';

  // --- Champs Expert ---
  biography = '';
  nombreAnneesExperience = 0;
  domaineId: number | null = null;
  categoriesIds: number[] = [];
  langues: string[] = [];
  languesDisponibles: string[] = [];
  dateEmbauche: string | null = null;
  nombreProduitsExpertise = 0;
  signatureImages: File[] = [];
  selectedSignatureNames: string[] = [];

  domaines: any[] = [];
  categories: any[] = [];

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      this.domaines = await lastValueFrom(this.http.get<any[]>('http://localhost:8080/api/domaines'));
      this.languesDisponibles = await lastValueFrom(this.http.get<string[]>('http://localhost:8080/api/experts/langues'));
    } catch (err) {
      console.error('❌ Erreur chargement domaines/langues', err);
    }
  }

  async onDomaineChange(): Promise<void> {
    if (this.domaineId && !isNaN(Number(this.domaineId))) {
      try {
        this.categories = await lastValueFrom(
          this.http.get<any[]>(`http://localhost:8080/api/categories/domaine/${this.domaineId}`)
        );
      } catch (err) {
        console.error('❌ Erreur chargement catégories', err);
        this.categories = [];
      }
    } else {
      this.categories = [];
    }
  }

  onCategoryChange(event: any): void {
    const categoryId = Number(event.target.value);
    if (event.target.checked) {
      if (!this.categoriesIds.includes(categoryId)) this.categoriesIds.push(categoryId);
    } else {
      this.categoriesIds = this.categoriesIds.filter(id => id !== categoryId);
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type.match('image.*')) {
      this.selectedFile = file;
      this.photoprofil = file.name;
      this.error = '';
    } else {
      this.error = 'Veuillez sélectionner une image valide (JPG, PNG, GIF)';
    }
  }

  onSignatureSelected(event: any): void {
    const files = Array.from(event.target.files) as File[];
    if (files.length + this.signatureImages.length > 5) {
      this.error = 'Maximum 5 images de signature autorisées';
      return;
    }
    const invalidFiles = files.filter(f => !f.type.match('image.*'));
    if (invalidFiles.length) {
      this.error = 'Seules les images sont autorisées pour les signatures';
      return;
    }
    const oversizedFiles = files.filter(f => f.size > 2 * 1024 * 1024);
    if (oversizedFiles.length) {
      this.error = 'Les images de signature ne doivent pas dépasser 2MB chacune';
      return;
    }
    this.signatureImages.push(...files);
    this.selectedSignatureNames.push(...files.map(f => f.name));
    this.error = '';
  }

  removeSignature(index: number): void {
    this.signatureImages.splice(index, 1);
    this.selectedSignatureNames.splice(index, 1);
  }

  toggleLangue(langue: string) {
    const index = this.langues.indexOf(langue);
    if (index === -1) this.langues.push(langue);
    else this.langues.splice(index, 1);
  }

  onLangueChange(event: any) {
    const langue = event.target.value;
    if (event.target.checked) this.langues.push(langue);
    else this.langues = this.langues.filter(l => l !== langue);
  }

  private validateExpertFields(): boolean {
    if (!this.biography || !this.nombreAnneesExperience || !this.domaineId || this.categoriesIds.length === 0 || this.langues.length === 0) {
      this.error = 'Veuillez remplir tous les champs requis pour le profil Expert.';
      return false;
    }
    return true;
  }

  private async signup(clientData: any, password: string): Promise<RegisterResponse> {
    const registerData = { ...clientData, password };
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return lastValueFrom(this.http.post<RegisterResponse>('http://localhost:8080/api/auth/register', registerData, { headers }));
  }

  private async uploadProfileImage(clientId: number, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await lastValueFrom(this.http.post<{ fileName: string }>(`http://localhost:8080/api/clients/${clientId}/upload-profile-image`, formData));
    return response.fileName;
  }

  private async createExpertProfile(clientId: number): Promise<void> {
    const expertData = {
      biography: this.biography,
      anneesExperience: this.nombreAnneesExperience,
      domaine: { idDomaine: this.domaineId },
      categories: this.categoriesIds.map(id => ({ idCategorie: id })),
      langues: this.langues,
      isActive: false,
      dateEmbauche: this.dateEmbauche,
      nombreProduitsExpertise: this.nombreProduitsExpertise,
      signatureImages: this.signatureImages.map(f => f.name),
      client: { id: clientId }
    };
    const createdExpert: any = await lastValueFrom(this.http.post('http://localhost:8080/api/experts', expertData));
    const expertId = createdExpert.id;
    if (this.signatureImages.length) await this.uploadSignatureImages(expertId, this.signatureImages);
  }

// Dans signup.component.ts - CORRECTION
  private async uploadSignatureImages(expertId: number, files: File[]): Promise<void> {
    if (files.length === 0) return;

    // 🆕 CORRECTION : Envoyer TOUTES les images en UNE SEULE requête
    const formData = new FormData();

    // Ajouter TOUS les fichiers
    files.forEach(file => {
      formData.append('signatures', file);
    });

    try {
      console.log(`📤 Envoi de ${files.length} signatures en une seule requête`);
      const response = await lastValueFrom(
        this.http.post(`http://localhost:8080/api/experts/${expertId}/upload-signatures`, formData)
      );
      console.log('✅ Toutes les signatures uploadées avec succès:', response);
    } catch (err) {
      console.error('❌ Erreur upload des signatures:', err);
      throw err;
    }
  }

  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    if (this.password !== this.confirmPassword) {
      this.error = 'Les mots de passe ne correspondent pas';
      return;
    }
    if (this.userType === 'expert' && !this.validateExpertFields()) return;
    this.loading = true;
    this.error = '';

    try {
      const clientData = { nom: this.nom, prenom: this.prenom, email: this.email, tel: this.tel, pays: this.pays, ville: this.ville, photoprofil: this.selectedFile ? this.selectedFile.name : '' };
      const response = await this.signup(clientData, this.password);
      if (this.selectedFile && response.idclient) await this.uploadProfileImage(response.idclient, this.selectedFile);
      if (this.userType === 'expert') await this.createExpertProfile(response.idclient);
      await this.authService.registerWithFirebase(this.email, this.password);
      alert('Un email de vérification vous a été envoyé.');
      this.router.navigate(['/connexion']);
    } catch (err) {
      console.error('❌ Erreur inscription', err);
      this.error = 'Une erreur est survenue lors de l\'inscription.';
    } finally {
      this.loading = false;
    }
  }
}
