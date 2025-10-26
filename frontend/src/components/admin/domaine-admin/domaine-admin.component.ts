import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';

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
  domaine: {
    idDomaine: number;
    nomDomaine: string;
    description: string;
    image: string;
  };
}

interface DomaineWithCount {
  idDomaine: number;
  nomDomaine: string;
  slug: string;
  image: string;
  description: string;
  categoriesCount: number;
}

@Component({
  selector: 'app-domaine',
  standalone: true,
  imports: [CommonModule, RouterModule, HttpClientModule, FormsModule],
  templateUrl: './domaine-admin.component.html',
  styleUrls: ['./domaine-admin.component.css']
})
export class DomaineAdminComponent implements OnInit {

  domaines: DomaineWithCount[] = [];
  isLoading = true;
  error = '';

  //  URL de base pour les API
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  // Variables pour la modal d'ajout
  showAddDomaineModal = false;
  step2 = false;
  isCreatingDomaine = false;

  newDomaine = { nomDomaine: '', description: '', image: '' };

  // Catégorie courante en cours de saisie
  currentCategorie: any = {
    nomCategorie: '',
    description: '',
    image: '',
    file: null,
    fileName: ''
  };

  // Liste de toutes les catégories ajoutées
  newCategories: any[] = [];

  selectedFile: File | null = null;

  constructor(
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadDomainesWithCategoriesCount();
  }

  loadDomainesWithCategoriesCount() {
    this.isLoading = true;
    this.error = '';

    this.getDomaines().subscribe({
      next: (domaines) => {
        console.log('📄 Domaines reçus:', domaines);

        if (!domaines || domaines.length === 0) {
          this.domaines = [];
          this.isLoading = false;
          return;
        }

        const categoryRequests = domaines.map(domaine =>
          this.getCategoriesByDomaine(domaine.idDomaine).pipe(
            map(categories => {
              const imageUrl = this.getDomaineImageUrl(domaine.image);

              console.log(` Domaine: ${domaine.nomDomaine}`);
              console.log(`   Image source: "${domaine.image}"`);
              console.log(`   Image finale: "${imageUrl}"`);

              return {
                idDomaine: domaine.idDomaine,
                nomDomaine: domaine.nomDomaine,
                slug: this.createSlug(domaine.nomDomaine),
                image: imageUrl,
                description: domaine.description,
                categoriesCount: categories?.length || 0
              };
            }),
            catchError(error => {
              console.error(`Erreur catégories pour domaine ${domaine.idDomaine}:`, error);
              return [{
                idDomaine: domaine.idDomaine,
                nomDomaine: domaine.nomDomaine,
                slug: this.createSlug(domaine.nomDomaine),
                image: this.getDomaineImageUrl(domaine.image),
                description: domaine.description,
                categoriesCount: 0
              }];
            })
          )
        );

        forkJoin(categoryRequests).subscribe({
          next: (domainesWithCount) => {
            this.domaines = domainesWithCount;
            this.isLoading = false;
            console.log('Domaines chargés:', this.domaines);
          },
          error: (error) => {
            console.error(' Erreur forkJoin:', error);
            this.error = 'Erreur lors du chargement des données';
            this.isLoading = false;
          }
        });
      },
      error: (error) => {
        console.error(' Erreur chargement domaines:', error);
        this.error = 'Erreur lors du chargement des domaines';
        this.isLoading = false;
      }
    });
  }

  getDomaines(): Observable<Domaine[]> {
    return this.http.get<Domaine[]>(`${this.API_BASE_URL}/api/domaines`).pipe(
      catchError(error => {
        console.error(' Erreur API domaines:', error);
        throw error;
      })
    );
  }

  getCategoriesByDomaine(idDomaine: number): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(`${this.API_BASE_URL}/api/categories/domaine/${idDomaine}`).pipe(
      catchError(error => {
        console.error(`Erreur API catégories pour domaine ${idDomaine}:`, error);
        return [[]];
      })
    );
  }

  //  Méthode pour construire l'URL des images de domaine
  getDomaineImageUrl(imageName: string): string {
    console.log(' Début getDomaineImageUrl avec:', imageName);

    if (!imageName || imageName.trim() === '') {
      console.log('Image vide, utilisation placeholder');
      return this.placeholderImage;
    }

    const cleanImageName = imageName.trim();

    // Si c'est déjà une URL complète
    if (cleanImageName.startsWith('http') || cleanImageName.startsWith('data:') || cleanImageName.startsWith('/api/')) {
      console.log('URL déjà complète');
      return cleanImageName.startsWith('http') ? cleanImageName : `${this.API_BASE_URL}${cleanImageName}`;
    }

    // Construire l'URL via l'endpoint Spring Boot
    const fullUrl = `${this.API_BASE_URL}/api/domaines/images/${cleanImageName}`;
    console.log(' URL construite:', fullUrl);
    return fullUrl;
  }

  createSlug(name: string): string {
    return name
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');
  }

  retry() {
    this.error = '';
    this.loadDomainesWithCategoriesCount();
  }

  onImageError(event: Event, domaine: DomaineWithCount) {
    const imgElement = event.target as HTMLImageElement;
    console.log(` Erreur image: ${domaine.nomDomaine} - ${imgElement.src}`);

    // Utiliser le placeholder en cas d'erreur
    imgElement.src = this.placeholderImage;
    imgElement.onerror = null; // Empêcher les boucles d'erreur
  }

  openAddDomaineModal() {
    this.showAddDomaineModal = true;
    this.step2 = false;
    this.isCreatingDomaine = false;
    this.newDomaine = { nomDomaine: '', description: '', image: '' };
    this.currentCategorie = {
      nomCategorie: '',
      description: '',
      image: '',
      file: null,
      fileName: ''
    };
    this.newCategories = [];
  }

  closeModal() {
    this.showAddDomaineModal = false;
  }

  goToNextStep() {
    if (!this.newDomaine.nomDomaine.trim() ||
      !this.newDomaine.description.trim() ||
      !this.selectedFile) {
      alert('Veuillez remplir tous les champs du domaine et sélectionner une image.');
      return;
    }
    this.step2 = true;
  }

  goBackToStep1() {
    this.step2 = false;
  }

  // Ajouter la catégorie courante à la liste
  addCurrentCategorie() {
    if (!this.currentCategorie.nomCategorie.trim() ||
      !this.currentCategorie.description.trim()) {
      alert('Veuillez remplir tous les champs de la catégorie.');
      return;
    }

    // Ajouter la catégorie courante à la liste
    this.newCategories.push({
      ...this.currentCategorie
    });

    // Réinitialiser le formulaire de catégorie courante
    this.currentCategorie = {
      nomCategorie: '',
      description: '',
      image: '',
      file: null,
      fileName: ''
    };

    // Réinitialiser l'input file
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    if (fileInput) {
      fileInput.value = '';
    }

    alert(`Catégorie ajoutée ! Vous avez maintenant ${this.newCategories.length} catégorie(s).`);
  }

  // Supprimer une catégorie de la liste
  removeCategorie(index: number) {
    this.newCategories.splice(index, 1);
  }

  // Gérer la sélection d'image pour la catégorie courante
  onCurrentCategorieImageSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.currentCategorie.file = file;
      this.currentCategorie.fileName = file.name;
      this.currentCategorie.image = file.name;
    }
  }

  // Créer le domaine avec toutes les catégories
  // Créer le domaine avec toutes les catégories
// Créer le domaine avec toutes les catégories
  createDomaineWithAllCategories() {
    if (this.newCategories.length === 0) {
      alert('Veuillez ajouter au moins une catégorie.');
      return;
    }

    this.isCreatingDomaine = true;

    const formData = new FormData();
    formData.append('nomDomaine', this.newDomaine.nomDomaine);
    formData.append('description', this.newDomaine.description);

    // Image du domaine
    if (this.selectedFile) {
      formData.append('image', this.selectedFile, this.selectedFile.name);
    }

    // Préparer les données des catégories
    const categoriesData = this.newCategories.map((categorie, index) => ({
      nomCategorie: categorie.nomCategorie,
      description: categorie.description,
      image: categorie.fileName || `categorie_${index + 1}.jpg`
    }));

    formData.append('categories', JSON.stringify(categoriesData));

    // Ajouter seulement les fichiers qui existent
    this.newCategories.forEach((categorie, index) => {
      if (categorie.file) {
        formData.append('categorieImages', categorie.file, categorie.file.name);
        console.log(` Envoi image catégorie ${index + 1}:`, categorie.fileName);
      }
    });

    console.log(' Envoi au backend:');
    console.log('- Catégories:', categoriesData.length);
    console.log('- Images catégories:', this.newCategories.filter(c => c.file).length);

    this.http.post(`${this.API_BASE_URL}/api/domaines/create-with-categories`, formData)
      .subscribe({
        next: (response: any) => {
          console.log(' Réponse backend:', response);
          alert(` Domaine créé avec ${this.newCategories.length} catégorie(s) !`);
          this.closeModals();
          this.loadDomainesWithCategoriesCount();
          this.isCreatingDomaine = false;
        },
        error: (error) => {
          console.error(' Erreur création domaine :', error);
          console.error(' Détails erreur:', error.error);
          alert(' Erreur lors de la création du domaine.');
          this.isCreatingDomaine = false;
        }
      });
  }

  onDomaineFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.newDomaine.image = file.name;
    }
  }

  // Variables pour l'édition de domaine
  showEditDomaineModal = false;
  selectedDomaine: any = { nomDomaine: '', description: '', image: '' };
  selectedDomaineFile: File | null = null;
  previewDomaineUrl: string | ArrayBuffer | null = null;

  // Méthode pour ouvrir la modal d'édition
  openEditDomaineModal(domaine: any, event?: Event) {
    if (event) event.stopPropagation();

    this.selectedDomaine = { ...domaine };
    this.previewDomaineUrl = domaine.image;
    this.selectedDomaineFile = null;
    this.showEditDomaineModal = true;
  }

  // Méthode pour gérer la sélection de fichier pour le domaine
  onDomaineEditFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedDomaineFile = file;
      const reader = new FileReader();
      reader.onload = e => this.previewDomaineUrl = reader.result;
      reader.readAsDataURL(file);
    }
  }

  // Méthode pour mettre à jour le domaine
  updateDomaine() {
    if (!this.selectedDomaine.nomDomaine.trim() || !this.selectedDomaine.description.trim()) {
      alert('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    const formData = new FormData();
    formData.append('nomDomaine', this.selectedDomaine.nomDomaine);
    formData.append('description', this.selectedDomaine.description);

    if (this.selectedDomaineFile) {
      formData.append('image', this.selectedDomaineFile);
    }

    this.http.put(`${this.API_BASE_URL}/api/domaines/${this.selectedDomaine.idDomaine}`, formData)
      .subscribe({
        next: (response: any) => {
          alert(' Domaine mis à jour avec succès !');
          this.closeModals();
          this.loadDomainesWithCategoriesCount();
        },
        error: (error) => {
          console.error('Erreur mise à jour domaine:', error);
          alert(' Erreur lors de la mise à jour du domaine.');
        }
      });
  }

  // Méthode pour fermer toutes les modals
  closeModals() {
    this.showAddDomaineModal = false;
    this.showEditDomaineModal = false;
    this.step2 = false;
    this.isCreatingDomaine = false;
    this.selectedDomaine = { nomDomaine: '', description: '', image: '' };
    this.selectedDomaineFile = null;
    this.previewDomaineUrl = null;
  }
}
