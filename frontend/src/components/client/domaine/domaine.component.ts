import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

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
  imports: [CommonModule, RouterModule, HttpClientModule],
  templateUrl: './domaine.component.html',
  styleUrls: ['./domaine.component.css']
})
export class DomaineComponent implements OnInit {

  domaines: DomaineWithCount[] = [];
  isLoading = true;
  error = '';

  @ViewChild('domainesSection') domainesSection!: ElementRef;
  // 🆕 URL de base pour les API
  private readonly API_BASE_URL = 'http://localhost:8080';
  private readonly placeholderImage = 'assets/images/placeholder.jpg';

  constructor(
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.loadDomainesWithCategoriesCount();
  }

  scrollToDomaines() {
    // Récupérer l'élément par son ID
    const domainesSection = document.getElementById('domaines-section');

    if (domainesSection) {
      // Défilement fluide vers l'élément
      domainesSection.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    } else {
      // Fallback si l'élément n'est pas trouvé
      window.scrollTo({
        top: window.innerHeight,
        behavior: 'smooth'
      });
    }
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

              console.log(`🖼️ Domaine: ${domaine.nomDomaine}`);
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
            console.log('✅ Domaines chargés:', this.domaines);
          },
          error: (error) => {
            console.error('❌ Erreur forkJoin:', error);
            this.error = 'Erreur lors du chargement des données';
            this.isLoading = false;
          }
        });
      },
      error: (error) => {
        console.error('❌ Erreur chargement domaines:', error);
        this.error = 'Erreur lors du chargement des domaines';
        this.isLoading = false;
      }
    });
  }

  getDomaines(): Observable<Domaine[]> {
    return this.http.get<Domaine[]>(`${this.API_BASE_URL}/api/domaines`).pipe(
      catchError(error => {
        console.error('❌ Erreur API domaines:', error);
        throw error;
      })
    );
  }

  getCategoriesByDomaine(idDomaine: number): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(`${this.API_BASE_URL}/api/categories/domaine/${idDomaine}`).pipe(
      catchError(error => {
        console.error(`❌ Erreur API catégories pour domaine ${idDomaine}:`, error);
        return [[]];
      })
    );
  }

  // 🆕 Méthode pour construire l'URL des images de domaine
  getDomaineImageUrl(imageName: string): string {
    console.log('🔄 Début getDomaineImageUrl avec:', imageName);

    if (!imageName || imageName.trim() === '') {
      console.log('📝 Image vide, utilisation placeholder');
      return this.placeholderImage;
    }

    const cleanImageName = imageName.trim();

    // Si c'est déjà une URL complète
    if (cleanImageName.startsWith('http') || cleanImageName.startsWith('data:') || cleanImageName.startsWith('/api/')) {
      console.log('✅ URL déjà complète');
      return cleanImageName.startsWith('http') ? cleanImageName : `${this.API_BASE_URL}${cleanImageName}`;
    }

    // Construire l'URL via l'endpoint Spring Boot
    const fullUrl = `${this.API_BASE_URL}/api/domaines/images/${cleanImageName}`;
    console.log('🔗 URL construite:', fullUrl);
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
    console.log(`❌ Erreur image: ${domaine.nomDomaine} - ${imgElement.src}`);

    // Utiliser le placeholder en cas d'erreur
    imgElement.src = this.placeholderImage;
    imgElement.onerror = null; // Empêcher les boucles d'erreur
  }
}
