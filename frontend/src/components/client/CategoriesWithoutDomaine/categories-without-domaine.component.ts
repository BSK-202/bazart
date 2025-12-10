import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';

interface Category {
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
  domaineName: string;
}

@Component({
  selector: 'app-categories-without-domaine',
  standalone: true,
  imports: [CommonModule, RouterModule, HttpClientModule],
  templateUrl: './categories-without-domaine.component.html',
  styleUrls: ['./categories-without-domaine.component.css']
})
export class CategoriesWithoutDomaineComponent implements OnInit {
  categories: CategoryWithSlug[] = [];
  isLoading = true;

  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadAllCategories();
  }

  loadAllCategories() {
    this.isLoading = true;

    this.http.get<Category[]>(`${this.API_BASE_URL}/api/categories`).subscribe({
      next: (categories) => {
        console.log('📄 Toutes les catégories reçues:', categories);

        this.categories = categories.map(cat => {
          const categoryWithSlug = {
            idCategorie: cat.idCategorie,
            nomCategorie: cat.nomCategorie,
            slug: this.createSlug(cat.nomCategorie),
            image: this.getCategorieImageUrl(cat.image),
            description: cat.description,
            count: 0, // Vous pouvez ajouter la logique pour compter les enchères si nécessaire
            domaineName: cat.domaine?.nomDomaine || 'Non classé'
          };

          console.log(`🔍 Catégorie: ${cat.nomCategorie}`);
          console.log(`   Domaine: ${categoryWithSlug.domaineName}`);
          console.log(`   Image: "${categoryWithSlug.image}"`);

          return categoryWithSlug;
        });

        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Erreur chargement des catégories:', error);
        this.isLoading = false;
      }
    });
  }

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
    imgElement.onerror = null;
  }
}
