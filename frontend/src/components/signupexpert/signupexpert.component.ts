import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-signup-expert',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './signupexpert.component.html',
  styleUrls: ['./signupexpert.component.css']
})
export class SingupexpertComponent implements OnInit {

  biography = '';
  nombreAnneesExperience = 0;
  domaineId: number | null = null;
  categoriesIds: number[] = [];
  langues: string[] = [];
  languesDisponibles: string[] = [];
  signatureImages: File[] = [];
  selectedSignatureNames: string[] = [];
  domaines: any[] = [];
  categories: any[] = [];
  error = '';
  loading = false;

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService
  ) {}

  async ngOnInit(): Promise<void> {
    console.log("🟦 [INIT] Chargement des domaines et langues...");

    try {
      this.domaines = await lastValueFrom(
        this.http.get<any[]>('http://localhost:8080/api/domaines')
      );

      this.languesDisponibles = await lastValueFrom(
        this.http.get<string[]>('http://localhost:8080/api/experts/langues')
      );

      console.log("✅ Domaines chargés:", this.domaines);
      console.log("✅ Langues chargées:", this.languesDisponibles);

    } catch (err) {
      console.error("❌ Erreur chargement initial:", err);
    }
  }

  async onDomaineChange(): Promise<void> {
    console.log("🟪 [DOMAINE] Domaine choisi:", this.domaineId);

    if (this.domaineId) {
      try {
        this.categories = await lastValueFrom(
          this.http.get<any[]>(`http://localhost:8080/api/categories/domaine/${this.domaineId}`)
        );

        console.log("📌 Catégories chargées:", this.categories);

      } catch (err) {
        console.error("❌ Erreur chargement catégories:", err);
        this.categories = [];
      }
    }
  }

  onCategoryChange(event: any): void {
    const id = Number(event.target.value);
    const checked = event.target.checked;

    console.log(`🟩 [CATEGORIE] ${id} -> ${checked}`);

    if (checked) this.categoriesIds.push(id);
    else this.categoriesIds = this.categoriesIds.filter(c => c !== id);
  }

  onLangueChange(event: any): void {
    const langue = event.target.value;
    const checked = event.target.checked;

    console.log(`🟦 [LANGUE] ${langue} -> ${checked}`);

    if (checked) this.langues.push(langue);
    else this.langues = this.langues.filter(l => l !== langue);
  }

  onSignatureSelected(event: any): void {
    const files = Array.from(event.target.files) as File[];

    console.log("🟧 [SIGNATURE] Fichiers ajoutés:", files);

    if (files.length + this.signatureImages.length > 5) {
      this.error = "Maximum 5 images autorisées";
      console.error("❌ Trop d'images sélectionnées");
      return;
    }

    this.signatureImages.push(...files);
    this.selectedSignatureNames.push(...files.map(f => f.name));
  }

  removeSignature(index: number): void {
    console.log(`🗑️ [DELETE SIGNATURE] Index: ${index}`);
    this.signatureImages.splice(index, 1);
    this.selectedSignatureNames.splice(index, 1);
  }

  private validateExpertFields(): boolean {
    if (!this.biography || !this.nombreAnneesExperience || !this.domaineId ||
      this.categoriesIds.length === 0 || this.langues.length === 0) {

      this.error = "Veuillez remplir tous les champs requis.";
      console.error("❌ Champs manquants pour Expert");
      return false;
    }
    return true;
  }

  async uploadSignatureImages(expertId: number): Promise<void> {
    if (this.signatureImages.length === 0) return;

    console.log(`📤 Envoi de ${this.signatureImages.length} signatures pour expert ${expertId}`);

    const formData = new FormData();
    this.signatureImages.forEach(file => formData.append('signatures', file));

    try {
      await lastValueFrom(
        this.http.post(`http://localhost:8080/api/experts/${expertId}/upload-signatures`, formData)
      );

      console.log("✅ Signatures uploadées avec succès");

    } catch (err) {
      console.error("❌ Erreur upload signatures:", err);
    }
  }

  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();

    console.log("🟦 [SUBMIT] Envoi du formulaire Expert...");

    if (!this.validateExpertFields()) return;

    this.loading = true;

    try {
      const clientId = this.authService.getCurrentUserId();
      console.log("🟨 ID client connecté:", clientId);

      if (!clientId) {
        this.error = "Utilisateur non connecté.";
        return;
      }

      const expertData = {
        biography: this.biography,
        anneesExperience: this.nombreAnneesExperience,
        domaine: { idDomaine: this.domaineId },
        categories: this.categoriesIds.map(id => ({ idCategorie: id })),
        langues: this.langues,
        isActive: false,
        client: { id: clientId }
      };

      console.log("📦 Données Expert envoyées:", expertData);

      const created: any = await lastValueFrom(
        this.http.post('http://localhost:8080/api/experts', expertData)
      );

      console.log("✅ Expert créé:", created);

      await this.uploadSignatureImages(created.id);

      alert("Votre demande d’expertise a été envoyée !");
      this.router.navigate(['/domaines']);

    } catch (err) {
      console.error("❌ Erreur création expert:", err);
      this.error = "Erreur lors de la création du profil expert.";
    }

    this.loading = false;
  }
}
