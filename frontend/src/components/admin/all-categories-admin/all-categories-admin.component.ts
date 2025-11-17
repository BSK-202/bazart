import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';

interface Categorie {
  idCategorie: number;
  nomCategorie: string;
  description: string;
  image: string;
  domaine: any;
}

interface CategoryWithSlug {
  idCategorie: number;
  nomCategorie: string;
  slug: string;
  image: string;
  description: string;
  count: number;
  domaineSlug: string;
}

@Component({
  selector: 'app-all-categories-admin',
  standalone: true,
  imports: [CommonModule, RouterModule, HttpClientModule, FormsModule],
  templateUrl: './all-categories-admin.component.html',
  styleUrls: ['./all-categories-admin.component.css']
})
export class AllCategoriesAdminComponent implements OnInit {
  domaineSlug = '';
  domaineName = '';
  filteredCategories: CategoryWithSlug[] = [];
  isLoading = true;
  isLastCategory = false;
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  // modals / sélection
  showEditModal = false;
  showDeleteModal = false;
  selectedCategory: any = { nomCategorie: '', description: '', image: '' };
  hasProducts = false;

  // image
  selectedFile: File | null = null;
  previewUrl: string | ArrayBuffer | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.domaineSlug = params.get('slug') || '';
      this.loadCategoriesForDomaine();
    });
  }

  openCategory(category: CategoryWithSlug) {
    this.router.navigate(['/admin/domaines', this.domaineSlug, 'categories', category.slug, category.idCategorie]);
  }

  onAddCategory() {
    this.selectedCategory = { nomCategorie: '', description: '', image: '', domaineSlug: this.domaineSlug };
    this.previewUrl = null;
    this.selectedFile = null;
    this.showEditModal = true;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = e => (this.previewUrl = reader.result);
      reader.readAsDataURL(file);
    }
  }

  loadCategoriesForDomaine() {
    this.isLoading = true;
    this.http.get<any[]>(`${this.API_BASE_URL}/api/domaines`).subscribe({
      next: (domaines) => {
        const domaine = domaines.find(d => this.createSlug(d.nomDomaine) === this.domaineSlug);
        if (domaine) {
          this.domaineName = domaine.nomDomaine;
          this.getCategoriesByDomaine(domaine.idDomaine).subscribe({
            next: (categories) => {
              this.filteredCategories = categories.map((cat: any) => ({
                idCategorie: cat.idCategorie,
                nomCategorie: cat.nomCategorie,
                slug: this.createSlug(cat.nomCategorie),
                image: this.getCategorieImageUrl(cat.image),
                description: cat.description,
                count: 0,
                domaineSlug: this.domaineSlug
              }));
              this.isLoading = false;
            },
            error: () => this.isLoading = false
          });
        } else this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  getCategoriesByDomaine(idDomaine: number): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(`${this.API_BASE_URL}/api/categories/domaine/${idDomaine}`);
  }

  getCategorieImageUrl(imageName: string): string {
    if (!imageName || imageName.trim() === '') return this.placeholderImage;
    const cleanImageName = imageName.trim();
    return cleanImageName.startsWith('http') ? cleanImageName : `${this.API_BASE_URL}/api/categories/images/${cleanImageName}`;
  }

  createSlug(name: string): string {
    return name.toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)+/g, '');
  }

  onImageError(event: Event) {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = this.placeholderImage;
  }

  /* ---------- MODALS ---------- */
  openEditModal(category: any, event?: Event) {
    if (event) event.stopPropagation();
    this.selectedCategory = { ...category };
    this.previewUrl = category.image || null;
    this.showEditModal = true;
  }

  openDeleteModal(category: any, event?: Event) {
    if (event) event.stopPropagation();
    this.selectedCategory = category;

    // Vérifier d'abord s'il y a des produits
    this.http.get<any[]>(`${this.API_BASE_URL}/api/produits/categorie/${category.idCategorie}`).subscribe({
      next: (produits) => {
        this.hasProducts = produits.length > 0;

        if (!this.hasProducts) {
          // Vérifier si c'est la dernière catégorie du domaine
          this.getDomaineId().then(domaineId => {
            this.http.get<number>(`${this.API_BASE_URL}/api/categories/domaine/${domaineId}/count`).subscribe({
              next: (count) => {
                this.showDeleteModal = true;
                this.isLastCategory = count === 1;
              },
              error: () => {
                this.showDeleteModal = true;
                this.isLastCategory = false;
              }
            });
          });
        } else {
          this.showDeleteModal = true;
          this.isLastCategory = false;
        }
      },
      error: () => {
        this.hasProducts = false;
        this.showDeleteModal = true;
        this.isLastCategory = false;
      }
    });
  }

  // Nouvelle méthode pour obtenir l'ID du domaine
  private getDomaineId(): Promise<number> {
    return new Promise((resolve) => {
      this.http.get<any[]>(`${this.API_BASE_URL}/api/domaines`).subscribe({
        next: (domaines) => {
          const domaine = domaines.find(d => this.createSlug(d.nomDomaine) === this.domaineSlug);
          resolve(domaine?.idDomaine || 0);
        },
        error: () => resolve(0)
      });
    });
  }

  closeModals() {
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.selectedCategory = null;
    this.previewUrl = null;
  }

  saveCategorie() {
    if (!this.selectedCategory.nomCategorie) {
      alert('Veuillez entrer un nom de catégorie');
      return;
    }

    const formData = new FormData();
    formData.append('nomCategorie', this.selectedCategory.nomCategorie);
    formData.append('description', this.selectedCategory.description || '');

    // Ajouter l'image si un fichier est sélectionné
    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    if (this.selectedCategory.idCategorie) {
      // ✅ MODIFICATION : Utilisation de FormData pour permettre l'upload d'image
      this.http.put(`${this.API_BASE_URL}/api/categories/${this.selectedCategory.idCategorie}`, formData)
        .subscribe({
          next: () => {
            alert('Catégorie mise à jour ✅');
            this.closeModals();
            this.loadCategoriesForDomaine();
          },
          error: (error) => {
            console.error('Erreur mise à jour catégorie:', error);
            alert('Erreur lors de la mise à jour de la catégorie');
          }
        });
    } else {
      // CRÉATION (déjà fonctionnel)
      this.http.get<any[]>(`${this.API_BASE_URL}/api/domaines`).subscribe({
        next: (domaines) => {
          const domaine = domaines.find(d => this.createSlug(d.nomDomaine) === this.domaineSlug);
          if (domaine) {
            formData.append('idDomaine', domaine.idDomaine.toString());
            this.http.post(`${this.API_BASE_URL}/api/categories`, formData).subscribe({
              next: () => {
                alert('Catégorie créée ✅');
                this.closeModals();
                this.loadCategoriesForDomaine();
              },
              error: (error) => {
                console.error('Erreur création catégorie:', error);
                alert('Erreur lors de la création de la catégorie');
              }
            });
          }
        },
        error: (error) => {
          console.error('Erreur chargement domaines:', error);
          alert('Erreur lors du chargement des domaines');
        }
      });
    }
  }

  deleteCategorie(id: number): void {
    let confirmationMessage: string;

    if (this.isLastCategory) {
      confirmationMessage = '⚠️ ATTENTION : Cette catégorie est la seule dans ce domaine.\n\nEn la supprimant, le domaine entier sera également supprimé.\n\nConfirmez-vous la suppression ?';
    } else {
      confirmationMessage = 'Êtes-vous sûr de vouloir supprimer cette catégorie ?';
    }

    if (confirm(confirmationMessage)) {
      this.http.delete<any>(`${this.API_BASE_URL}/api/categories/${id}`).subscribe({
        next: (response) => {
          // ✅ Maintenant response est un objet JSON
          alert('✅ ' + response.message);
          this.loadCategoriesForDomaine();
          this.closeModals();

          // Si le domaine a été supprimé, rediriger vers la liste des domaines
          if (response.domaineDeleted === 'true') {
            setTimeout(() => {
              this.router.navigate(['/admin/domaines']);
            }, 1000);
          }
        },
        error: (error) => {
          // ✅ Maintenant error.error est un objet JSON avec un message
          if (error.status === 400 && error.error && error.error.message) {
            alert('❌ ' + error.error.message);
          } else if (error.status === 404 && error.error && error.error.message) {
            alert('❌ ' + error.error.message);
          } else if (error.status === 500 && error.error && error.error.message) {
            alert('⚠️ ' + error.error.message);
          } else {
            alert('⚠️ Erreur inattendue lors de la suppression.');
            console.error('Erreur détaillée:', error);
          }
        }
      });
    }
  }
}
