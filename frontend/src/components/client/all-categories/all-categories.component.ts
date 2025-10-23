import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  selector: 'app-all-categories',
  standalone: true,
  imports: [CommonModule, RouterModule, HttpClientModule],
  templateUrl: './all-categories.component.html',
  styleUrls: ['./all-categories.component.css']
})
export class AllCategoriesComponent implements OnInit {
  domaineSlug: string = '';
  domaineName: string = '';
  filteredCategories: CategoryWithSlug[] = [];
  isLoading = true;

  // 🆕 URL de base pour les API
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.domaineSlug = params.get('slug') || '';
      this.loadCategoriesForDomaine();
    });
  }

  loadCategoriesForDomaine() {
    this.isLoading = true;

    this.http.get<any[]>(`${this.API_BASE_URL}/api/domaines`).subscribe({
      next: (domaines) => {
        const domaine = domaines.find(d =>
          this.createSlug(d.nomDomaine) === this.domaineSlug
        );

        if (domaine) {
          this.domaineName = domaine.nomDomaine;
          this.getCategoriesByDomaine(domaine.idDomaine).subscribe({
            next: (categories) => {
              console.log('📄 Données catégories reçues:', categories);

              this.filteredCategories = categories.map(cat => {
                const categoryWithSlug = {
                  idCategorie: cat.idCategorie,
                  nomCategorie: cat.nomCategorie,
                  slug: this.createSlug(cat.nomCategorie),
                  image: this.getCategorieImageUrl(cat.image),
                  description: cat.description,
                  count: 0,
                  domaineSlug: this.domaineSlug
                };

                console.log(`🔍 Catégorie: ${cat.nomCategorie}`);
                console.log(`   Slug: "${categoryWithSlug.slug}"`);
                console.log(`   ID: ${cat.idCategorie}`);
                console.log(`   Image: "${categoryWithSlug.image}"`);

                return categoryWithSlug;
              });
              this.isLoading = false;
            },
            error: (error) => {
              console.error('Erreur chargement catégories:', error);
              this.isLoading = false;
            }
          });
        } else {
          console.error('Domaine non trouvé');
          this.isLoading = false;
        }
      },
      error: (error) => {
        console.error('Erreur chargement domaines:', error);
        this.isLoading = false;
      }
    });
  }

  getCategoriesByDomaine(idDomaine: number): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(`${this.API_BASE_URL}/api/categories/domaine/${idDomaine}`);
  }

  // 🆕 Méthode pour construire l'URL des images de catégorie
  getCategorieImageUrl(imageName: string): string {
    console.log(`🔧 Construction chemin image catégorie: "${imageName}"`);

    if (!imageName || imageName.trim() === '') {
      console.log('❌ Nom d\'image vide, utilisation placeholder');
      return this.placeholderImage;
    }

    const cleanImageName = imageName.trim();

    // Si c'est déjà une URL complète
    if (cleanImageName.startsWith('http') || cleanImageName.startsWith('data:') || cleanImageName.startsWith('/api/')) {
      console.log('✅ Chemin déjà complet');
      return cleanImageName.startsWith('http') ? cleanImageName : `${this.API_BASE_URL}${cleanImageName}`;
    }

    // Construire l'URL via l'endpoint Spring Boot
    const fullUrl = `${this.API_BASE_URL}/api/categories/images/${cleanImageName}`;
    console.log(`🔗 Chemin construit: ${fullUrl}`);
    return fullUrl;
  }

  createSlug(name: string): string {
    return name
      .toLowerCase()
      .replace(/[éèêë]/g, 'e')
      .replace(/[àâä]/g, 'a')
      .replace(/[îï]/g, 'i')
      .replace(/[ôö]/g, 'o')
      .replace(/[ùûü]/g, 'u')
      .replace(/ç/g, 'c')
      .replace(/[^a-z0-9]/g, '-')
      .replace(/-+/g, '-')
      .replace(/^-|-$/g, '');
  }

  onImageError(event: Event) {
    const imgElement = event.target as HTMLImageElement;
    console.log(`❌ Erreur chargement image: ${imgElement.src}`);
    imgElement.src = this.placeholderImage;
    imgElement.onerror = null; // Empêcher les boucles d'erreur
  }
}
