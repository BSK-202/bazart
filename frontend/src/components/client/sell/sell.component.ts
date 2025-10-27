import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {AuthService} from '../../../services/auth.service';

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
  expertiseType: string;
}

@Component({
  selector: 'app-sell',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HttpClientModule],
  templateUrl: './sell.component.html',
  styleUrls: ['./sell.component.css']
})
export class SellComponent implements OnInit {

  formData: FormData = {
    title: '',
    domain: '',
    category: '',
    description: '',
    startingPrice: '',
    condition: '',
    images: [],
    isExpertised: false,
    expertiseType: ''
  };

  imagePreviews: string[] = [];
  isDragOver = false;

  domaines: any[] = [];
  filteredCategories: any[] = [];

  conditions = [
    { value: 'new', label: 'Neuf' },
    { value: 'excellent', label: 'Excellent état' },
    { value: 'good', label: 'Bon état' },
    { value: 'vintage', label: 'Vintage' }
  ];

  benefits: Benefit[] = [
    { icon: 'trending-up', title: 'Meilleur Prix', description: 'Le système d\'enchères garantit le meilleur prix pour vos articles' },
    { icon: 'shield', title: 'Sécurisé', description: 'Transactions sécurisées et paiements garantis' },
    { icon: 'users', title: 'Large Audience', description: 'Accédez à des milliers de collectionneurs passionnés' }
  ];

  constructor(private http: HttpClient,private authService: AuthService) {}
  onPreview() {
    console.log('Aperçu:', this.formData);

    // Vérification minimum
    if (this.formData.images.length < 3) {
      alert('Veuillez ajouter au moins 3 photos pour l\'aperçu');
      return;
    }

    // Ici tu peux ouvrir un modal ou afficher les données en console
  }

  ngOnInit() {
    // Charger la liste des domaines depuis API (log pour debug)
    const urlDomaines = 'http://localhost:8080/api/domaines';
    console.log('[INIT] Appel API domainess ->', urlDomaines);
    this.http.get<any[]>(urlDomaines).pipe(
      tap(res => {
        console.log('[INIT] Réponse domaines (raw):', res);
      }),
      catchError(err => {
        console.error('[INIT] Erreur en chargeant domaines', err);
        return of([]); // évite de casser l'app
      })
    ).subscribe(data => {
      this.domaines = data || [];
      console.log('[INIT] domaines stockés :', this.domaines);
    });
  }

  /**
   * Appelé quand l'utilisateur choisit un domaine.
   * Ajoute beaucoup de logs pour comprendre ce que renvoie l'API catégories.
   */
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
          this.formData.category = ''; // reset catégorie
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

  onSubmit() {
    // Vérification des champs obligatoires
    if (!this.formData.title || !this.formData.domain || !this.formData.category ||
      !this.formData.description || !this.formData.startingPrice) {
      alert("⚠️ Veuillez remplir tous les champs obligatoires avant de continuer !");
      return;
    }

    // Vérification du nombre d'images
    if (this.formData.images.length < 3) {
      alert("📸 Vous devez ajouter au moins 3 images pour présenter votre produit !");
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
    const produit = {
      nom: this.formData.title,
      description: this.formData.description,
      prixDebut: Number(this.formData.startingPrice),
      prixFin: Number(this.formData.startingPrice) * 2,
      aExpertise: this.formData.isExpertised,
      categorieId: Number(this.formData.category),
      vendeurId: vendeurId
    };

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

        // Reset du formulaire après succès
        this.resetForm();
      },
      error: (err) => {
        console.error("❌ Erreur backend:", err);
        const msg = err.error?.message || "Une erreur inattendue s'est produite.";
        alert("❌ Échec de la création du produit :\n\n" + msg);
      }
    });
  }
// Méthode pour reset le formulaire
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
      expertiseType: ''
    };
    this.imagePreviews = [];
  }

}
