import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { InteractionService } from '../../../services/interaction.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faHeart as faHeartSolid } from '@fortawesome/free-solid-svg-icons';
import { faHeart as faHeartRegular, faComment } from '@fortawesome/free-regular-svg-icons';

interface Produit {
  id: number;
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number | null;
  etat: string;
  vendeurNom: string;
  acheteurNom: string | null;
  categorieNom: string;
  nombreInteractions: number;
  nombreCommentaires: number;
  hasLiked?: boolean;
  images: string[];
  imagePrincipale?: string;
}

interface CategoryInfo {
  id: number;
  nom: string;
  image: string;
  description: string;
}

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [CommonModule, RouterModule, HttpClientModule],
  templateUrl: './category-detail-admin.component.html',
  styleUrls: ['./category-detail-admin.component.css']
})
export class CategoryDetailAdminComponent implements OnInit {
  domaineSlug: string = '';
  categorieSlug: string = '';
  categorieId: number = 0;
  categorieImage: string = '';

  produits: Produit[] = [];
  currentCategory: CategoryInfo | undefined;

  faHeartSolid = faHeartSolid;
  faHeartRegular = faHeartRegular;
  faComment = faComment;

  protected readonly categorieImageBasePath = '/assets/categorie/';
  // 🆕 URL de base pour les images de produits via Spring Boot
  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private interactionService: InteractionService
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.domaineSlug = params.get('domaineSlug') || '';
      this.categorieSlug = params.get('slug1') || '';
      const categorieIdParam = params.get('slug2') || '0';
      this.categorieId = parseInt(categorieIdParam, 10) || 0;

      console.log('🔥 Paramètres reçus:');
      console.log('   Domaine:', this.domaineSlug);
      console.log('   Catégorie Slug:', this.categorieSlug);
      console.log('   Catégorie ID:', this.categorieId);
    });

    this.route.queryParamMap.subscribe(queryParams => {
      this.categorieImage = queryParams.get('image') || '';
      console.log('🖼️ Image de catégorie depuis queryParams:', this.categorieImage);

      this.loadCategoryInfo();
      this.loadProduits();
    });
  }

  loadCategoryInfo() {
    if (!this.categorieId) {
      console.error('❌ ID de catégorie invalide');
      return;
    }

    const url = `${this.API_BASE_URL}/api/categories/${this.categorieId}`;

    this.http.get<any>(url).subscribe({
      next: (categoryData) => {
        console.log('✅ Informations catégorie reçues:', categoryData);

        // 🆕 PRIORITÉ à l'image des queryParams, sinon celle de l'API
        const categoryImage = this.categorieImage || categoryData.image;

        console.log('🖼 Image sélectionnée:', {
          fromQueryParams: this.categorieImage,
          fromAPI: categoryData.image,
          final: categoryImage
        });

        this.currentCategory = {
          id: categoryData.idCategorie,
          nom: categoryData.nomCategorie,
          image: this.getCategorieImageUrl(categoryImage), // Utilise la méthode mise à jour
          description: categoryData.description
        };

        console.log('📋 Catégorie actuelle:', this.currentCategory);
      },
      error: (err) => {
        console.error('❌ Erreur chargement info catégorie:', err);

        // Fallback avec l'image des queryParams
        const fallbackImage = this.categorieImage || '';

        this.currentCategory = {
          id: this.categorieId,
          nom: this.categorieSlug.replace(/-/g, ' '),
          image: this.getCategorieImageUrl(fallbackImage), // Utilise la méthode mise à jour
          description: `Catégorie ${this.categorieSlug}`
        };

        console.log('🔄 Catégorie fallback:', this.currentCategory);
      }
    });
  }
  getCategorieImageUrl(imageName: string | undefined): string {
    console.log('🖼 Construction URL image catégorie:', imageName);

    if (!imageName || imageName.trim() === '') {
      console.log('❌ Nom d\'image vide, utilisation placeholder');
      return 'assets/images/placeholder.jpg';
    }

    const cleanImageName = imageName.trim();

    // Si c'est déjà une URL complète (http, https, data:)
    if (cleanImageName.startsWith('http') || cleanImageName.startsWith('data:')) {
      console.log('✅ Chemin déjà complet');
      return cleanImageName;
    }

    // Si c'est un chemin d'API Spring Boot
    if (cleanImageName.startsWith('/api/')) {
      const fullUrl = `${this.API_BASE_URL}${cleanImageName}`;
      console.log(`🔗 Chemin API construit: ${fullUrl}`);
      return fullUrl;
    }

    // Si c'est un chemin relatif Angular (assets/)
    if (cleanImageName.startsWith('assets/') || cleanImageName.startsWith('./')) {
      console.log('✅ Chemin assets Angular');
      return cleanImageName;
    }

    // Construire l'URL via l'endpoint Spring Boot pour les images de catégories
    const fullUrl = `${this.API_BASE_URL}/api/categories/images/${cleanImageName}`;
    console.log(`🔗 Chemin catégorie construit: ${fullUrl}`);
    return fullUrl;
  }


  // 🆕 MÉTHODE pour construire l'URL complète de l'image produit
  getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === '') {
      return 'assets/images/placeholder.jpg';
    }

    // Si l'image commence par http ou /, c'est déjà une URL complète
    if (imageName.startsWith('http') || imageName.startsWith('/api/')) {
      return `${this.API_BASE_URL}${imageName}`;
    }

    // Sinon, construire l'URL via l'endpoint Spring Boot
    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  toggleLike(produit: Produit, event: Event) {
    event.stopPropagation();
    console.log('🎯 Clic sur like - Produit:', produit.id, 'Actuellement liké:', produit.hasLiked);

    this.interactionService.toggleLike(produit.id).subscribe({
      next: (response: any) => {
        console.log('✅ Like réussi:', response);
        produit.hasLiked = response.liked;
        produit.nombreInteractions = response.interactionCount;
        this.triggerLikeAnimation(produit.id);
      },
      error: (err) => {
        console.error('❌ Erreur lors du like:', err);
        this.router.navigate(['/connexion'], {
          queryParams: {
            returnUrl: this.router.url,
            message: 'Veuillez vous connecter pour aimer ce produit'
          }
        });
      }
    });
  }

  private triggerLikeAnimation(produitId: number) {
    const element = document.querySelector(`[data-produit-id="${produitId}"] .interaction`);
    if (element) {
      element.classList.add('liked');
      setTimeout(() => element.classList.remove('liked'), 300);
    }
  }

  loadProduits() {
    if (!this.categorieId) {
      console.error("❌ ID catégorie est invalide");
      return;
    }

    const url = `${this.API_BASE_URL}/api/produits/categorie/${this.categorieId}/acceptes`;

    this.http.get<Produit[]>(url).subscribe({
      next: (data) => {
        console.log("✅ Produits reçus:", data);

        this.produits = data.map(prod => {
          // 🆕 Utiliser la nouvelle méthode pour construire l'URL
          prod.imagePrincipale = prod.images && prod.images.length > 0
            ? this.getProduitImageUrl(prod.id, prod.images[0])
            : 'assets/images/placeholder.jpg';

          console.log(`📸 Produit ${prod.id} → imagePrincipale: ${prod.imagePrincipale}`);
          return prod;
        });

        console.table(this.produits);
      },
      error: (err) => {
        console.error("❌ Erreur lors du chargement des produits:", err);
        this.produits = [];
      }
    });
  }
}
