import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService } from '../../../services/auth.service';

interface Benefit {
  icon: string;
  title: string;
  description: string;
}

interface FormData {
  title: string;
  domain: string;
  category: string;
  description: string;
  startingPrice: string;
  condition: string;
  images: File[];
  isExpertised: boolean;
  expertiseType: string;   // 'online' | 'onsite'
  expertiseSlot1: string;
  expertiseSlot2: string;
  expertiseSlot3: string;
}

@Component({
  selector: 'app-sell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HttpClientModule],
  templateUrl: './sell.component.html',
  styleUrls: ['./sell.component.css']
})
export class SellComponent implements OnInit {
  currentStep: number = 1;

  formData: FormData = {
    title: '',
    domain: '',
    category: '',
    description: '',
    startingPrice: '',
    condition: '',
    images: [],
    isExpertised: false,
    expertiseType: '',
    expertiseSlot1: '',
    expertiseSlot2: '',
    expertiseSlot3: ''
  };

  imagePreviews: string[] = [];
  isDragOver = false;

  domaines: any[] = [];
  filteredCategories: any[] = [];

  benefits: Benefit[] = [
    { icon: 'trending-up', title: 'Meilleur Prix', description: 'Le système d\'enchères garantit le meilleur prix pour vos articles' },
    { icon: 'shield', title: 'Sécurisé', description: 'Transactions sécurisées et paiements garantis' },
    { icon: 'users', title: 'Large Audience', description: 'Accédez à des milliers de collectionneurs passionnés' }
  ];

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    const urlDomaines = 'http://localhost:8080/api/domaines';
    console.log('[INIT] Appel API domaines ->', urlDomaines);
    this.http.get<any[]>(urlDomaines).pipe(
      tap(res => {
        console.log('[INIT] Réponse domaines (raw):', res);
      }),
      catchError(err => {
        console.error('[INIT] Erreur en chargeant domaines', err);
        return of([]);
      })
    ).subscribe(data => {
      this.domaines = data || [];
      console.log('[INIT] domaines stockés :', this.domaines);
    });
  }

  nextStep() {
    if (this.validateCurrentStep()) {
      if (this.currentStep < 3) {
        this.currentStep++;
      }
    }
  }

  prevStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  validateCurrentStep(): boolean {
    switch (this.currentStep) {
      case 1:
        if (!this.formData.title || !this.formData.startingPrice || !this.formData.domain ||
          !this.formData.category || !this.formData.description) {
          alert('Veuillez remplir tous les champs obligatoires de cette étape');
          return false;
        }
        return true;

      case 2:
        if (this.formData.isExpertised && !this.formData.expertiseType) {
          alert('Veuillez sélectionner un type d\'expertise');
          return false;
        }
        if (this.formData.isExpertised && this.formData.expertiseType === 'onsite') {
          if (!this.formData.expertiseSlot1 || !this.formData.expertiseSlot2 || !this.formData.expertiseSlot3) {
            alert('Veuillez remplir les 3 créneaux d\'expertise');
            return false;
          }
          // Validate slots are within next 4 days
          const slots = [this.formData.expertiseSlot1, this.formData.expertiseSlot2, this.formData.expertiseSlot3];
          for (let i = 0; i < slots.length; i++) {
            if (!this.isSlotWithinNext4Days(slots[i])) {
              alert(`Le créneau ${i + 1} doit être dans les 4 prochains jours`);
              return false;
            }
          }
        }
        return true;

      case 3:
        if (this.formData.images.length < 3) {
          alert('Veuillez ajouter au moins 3 photos');
          return false;
        }
        return true;

      default:
        return true;
    }
  }

  onPreview() {
    console.log('Aperçu:', this.formData);

    if (this.formData.images.length < 3) {
      alert('Veuillez ajouter au moins 3 photos pour l\'aperçu');
      return;
    }

    // For preview, we'll just show an alert with the data
    let previewText = `Titre: ${this.formData.title}\n`;
    previewText += `Prix de départ: ${this.formData.startingPrice} DH\n`;
    previewText += `Expertise: ${this.formData.isExpertised ? 'Oui' : 'Non'}\n`;
    previewText += `Photos: ${this.formData.images.length}\n`;
    previewText += `\nVotre article sera soumis à validation.`;

    alert('Aperçu de votre annonce:\n\n' + previewText);
  }

  onDomainChange(domainId: string) {
    const id = Number(domainId);
    console.log("onDomainChange -> id reçu :", id);

    if (domainId != null) {
      const url = `http://localhost:8080/api/categories/domaine/${id}`;
      console.log("[API] Appel catégories ->", url);

      this.http.get<any[]>(url).subscribe({
        next: (data) => {
          console.log("[API] Réponse catégories (raw):", data);
          this.filteredCategories = data;
          console.log("[API] filteredCategories stockées:", this.filteredCategories);
          this.formData.category = '';
        },
        error: (err) => {
          console.error("[API] Erreur lors de la récupération des catégories:", err);
        }
      });
    } else {
      console.warn("Aucun domaine sélectionné, reset filteredCategories");
      this.filteredCategories = [];
    }
  }

  // Gestion drag & drop images
  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    if (event.dataTransfer?.files) {
      this.handleFiles(event.dataTransfer.files);
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(input.files);
    }
  }

  private handleFiles(files: FileList) {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (!file.type.startsWith('image/')) {
        alert('Veuillez sélectionner uniquement des images');
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        alert('L\'image est trop volumineuse (max 10MB)');
        continue;
      }

      this.formData.images.push(file);
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreviews.push(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(index: number) {
    this.formData.images.splice(index, 1);
    this.imagePreviews.splice(index, 1);
  }

  private isSlotWithinNext4Days(slot?: string): boolean {
    if (!slot) return false;
    const d = new Date(slot);
    if (isNaN(d.getTime())) return false;

    const now = new Date();
    const limit = new Date(now.getTime() + 4 * 24 * 60 * 60 * 1000);
    return d >= now && d <= limit;
  }

  onSubmit() {
    // Final validation
    if (!this.validateCurrentStep()) {
      return;
    }

    const vendeurId = this.authService.getCurrentUserId();
    if (!vendeurId) {
      alert("❌ Impossible de récupérer votre identifiant utilisateur.\nVeuillez vous reconnecter pour continuer.");
      return;
    }

    console.log("✅ Formulaire validé, préparation de l'envoi...");

    const formDataToSend = new FormData();

    // Création de l'objet produit
    const produit: any = {
      nom: this.formData.title,
      description: this.formData.description,
      prixDebut: Number(this.formData.startingPrice),
      prixFin: Number(this.formData.startingPrice) * 2,
      aExpertise: this.formData.isExpertised,
      categorieId: Number(this.formData.category),
      vendeurId: vendeurId
    };

    if (this.formData.isExpertised) {
      if (!this.formData.expertiseType) {
        alert("Veuillez choisir le type d'expertise (en ligne ou sur place).");
        return;
      }

      if (this.formData.expertiseType === 'onsite') {
        const slots = [
          { name: 'Créneau 1', value: this.formData.expertiseSlot1 },
          { name: 'Créneau 2', value: this.formData.expertiseSlot2 },
          { name: 'Créneau 3', value: this.formData.expertiseSlot3 },
        ];

        for (const s of slots) {
          if (!this.isSlotWithinNext4Days(s.value)) {
            alert(`⚠️ ${s.name} doit être dans les 4 prochains jours (et après maintenant).`);
            return;
          }
        }

        produit.expertiseMethod = 'ONSITE';
        produit.expertiseSlot1 = this.formData.expertiseSlot1 || null;
        produit.expertiseSlot2 = this.formData.expertiseSlot2 || null;
        produit.expertiseSlot3 = this.formData.expertiseSlot3 || null;
      } else {
        produit.expertiseMethod = 'ONLINE';
        produit.expertiseSlot1 = null;
        produit.expertiseSlot2 = null;
        produit.expertiseSlot3 = null;
      }
    }

    formDataToSend.append("produit", new Blob([JSON.stringify(produit)], { type: "application/json" }));

    // Ajout des images
    this.formData.images.forEach(file => {
      formDataToSend.append("images", file, file.name);
    });

    console.log("🚀 Envoi du produit au backend...");

    this.http.post("http://localhost:8080/api/produits", formDataToSend).subscribe({
      next: (response) => {
        console.log("✅ Produit créé avec succès!", response);
        alert("🎉 Votre produit a été créé avec succès et est en attente de validation !");
        this.router.navigate(['/profil'], { queryParams: { tab: 'pending' } });
        this.resetForm();
      },
      error: (err) => {
        console.error("❌ Erreur backend:", err);
        const status = err.status;
        const msg = err.error || err.error?.message || "Une erreur inattendue s'est produite.";

        if (status === 400 && typeof msg === 'string' && msg.includes("Solde insuffisant")) {
          alert("❌ Solde insuffisant pour lancer l'expertise. Veuillez recharger votre wallet.");
          return;
        }

        alert("❌ Échec de la création du produit :\n\n" + (typeof msg === 'string' ? msg : "Erreur inconnue."));
      }
    });
  }

  resetForm() {
    this.formData = {
      title: '',
      domain: '',
      category: '',
      description: '',
      startingPrice: '',
      condition: '',
      images: [],
      isExpertised: false,
      expertiseType: '',
      expertiseSlot1: '',
      expertiseSlot2: '',
      expertiseSlot3: ''
    };
    this.imagePreviews = [];
    this.currentStep = 1;
  }
}
