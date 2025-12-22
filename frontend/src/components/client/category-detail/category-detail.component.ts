import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import {forkJoin, Observable} from 'rxjs';
import { InteractionService } from '../../../services/interaction.service';
import { CommentaireService } from '../../../services/commentaire.service';
import { AuthService } from '../../../services/auth.service';
import { CommentModalComponent } from '../comment-modal/comment-modal.component';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faHeart as faHeartSolid } from '@fortawesome/free-solid-svg-icons';
import { faHeart as faHeartRegular, faComment } from '@fortawesome/free-regular-svg-icons';
import {map} from 'rxjs/operators';

interface Produit {
  id: number;
  nom: string;
  description: string;
  prixDebut: number;
  prixFin: number | null;
  vendeurNom: string;
  acheteurNom: string | null;
  categorieNom: string;
  nombreInteractions: number;
  nombreCommentaires: number;
  hasLiked?: boolean;
  images: string[];
  imagePrincipale?: string;
  etat: string;
  expertiseApproved?: boolean;
  expertisePublicComment?: string;
  expertiseAuthenticityLevel?: string;
  expertiseProductCondition?: string;
}

interface CategoryInfo {
  id: number;
  nom: string;
  image: string;
  description: string;
}

interface LikeResponse {
  liked: boolean;
  interactionCount: number;
  message?: string;
}

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    HttpClientModule,
    FaIconComponent,
    CommentModalComponent
  ],
  templateUrl: './category-detail.component.html',
  styleUrls: ['./category-detail.component.css']
})
export class CategoryDetailComponent implements OnInit {
  domaineSlug: string = '';
  categorieSlug: string = '';
  categorieId: number = 0;
  categorieImage: string = '';

  produits: Produit[] = [];
  currentCategory: CategoryInfo | undefined;
  isLoading: boolean = true;

  showCommentModal: boolean = false;
  selectedProduit: Produit | null = null;

  faHeartSolid = faHeartSolid;
  faHeartRegular = faHeartRegular;
  faComment = faComment;

  protected readonly categorieImageBasePath = '/assets/categorie/';
  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private interactionService: InteractionService,
    private commentaireService: CommentaireService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      // Gérer les deux types de routes
      this.domaineSlug = params.get('domaineSlug') || '';
      this.categorieSlug = params.get('slug') || params.get('slug1') || '';
      const categorieIdParam = params.get('id') || params.get('slug2') || '0';
      this.categorieId = parseInt(categorieIdParam, 10) || 0;

      console.log('🔥 Paramètres reçus:');
      console.log('   Domaine:', this.domaineSlug);
      console.log('   Catégorie Slug:', this.categorieSlug);
      console.log('   Catégorie ID:', this.categorieId);
      console.log('   Contexte:', this.isFromCategoriesWithoutDomaine() ? 'SANS DOMAINE' : 'AVEC DOMAINE');

      this.loadCategoryInfo();
      this.loadProduits();
    });

    this.route.queryParamMap.subscribe(queryParams => {
      this.categorieImage = queryParams.get('image') || '';
      console.log('🖼️ Image de catégorie depuis queryParams:', this.categorieImage);
      console.log('📱 QueryParams complets:', queryParams);
    });
  }

  // Méthode pour déterminer si on est dans le contexte "sans domaine"
  isFromCategoriesWithoutDomaine(): boolean {
    return !this.domaineSlug;
  }

  // ✅ Redirection vers la page détail du produit
  goToProductDetail(produitId: number): void {
    console.log('🎯 Navigation vers le produit:', produitId);
    this.router.navigate(['/produit', produitId]);
  }

  loadCategoryInfo() {
    if (!this.categorieId) {
      console.error('❌ ID de catégorie invalide');
      return;
    }

    const url = `${this.API_BASE_URL}/api/categories/${this.categorieId}`;

    this.http.get<any>(url).subscribe({
      next: (categoryData) => {
        console.log(' Informations catégorie reçues:', categoryData);

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
          image: this.getCategorieImageUrl(categoryImage),
          description: categoryData.description
        };

        console.log(' Catégorie actuelle:', this.currentCategory);
      },
      error: (err) => {
        console.error('❌ Erreur chargement info catégorie:', err);

        // 🆕 Fallback amélioré avec l'image des queryParams
        const fallbackImage = this.categorieImage || '';

        this.currentCategory = {
          id: this.categorieId,
          nom: this.categorieSlug.replace(/-/g, ' '),
          image: this.getCategorieImageUrl(fallbackImage),
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

  getProduitImageUrl(produitId: number, imageName: string): string {
    if (!imageName || imageName.trim() === '') {
      return 'assets/images/placeholder.jpg';
    }

    if (imageName.startsWith('http') || imageName.startsWith('/api/')) {
      return `${this.API_BASE_URL}${imageName}`;
    }

    return `${this.API_BASE_URL}/api/produits/images/${produitId}/${imageName}`;
  }

  toggleLike(produit: Produit, event: Event) {
    event.stopPropagation();
    console.log('🎯 Clic sur like - Produit:', produit.id, 'Actuellement liké:', produit.hasLiked);

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/connexion'], {
        queryParams: {
          returnUrl: this.router.url,
          message: 'Veuillez vous connecter pour aimer ce produit'
        }
      });
      return;
    }

    this.interactionService.toggleLike(produit.id).subscribe({
      next: (response: LikeResponse) => {
        console.log('✅ Like réussi:', response);
        produit.hasLiked = response.liked;
        produit.nombreInteractions = response.interactionCount;
        this.triggerLikeAnimation(produit.id);
      },
      error: (err) => {
        console.error('❌ Erreur lors du like:', err);
        if (err.status === 401) {
          this.router.navigate(['/connexion'], {
            queryParams: {
              returnUrl: this.router.url,
              message: 'Veuillez vous connecter pour aimer ce produit'
            }
          });
        }
      }
    });
  }

  openCommentModal(produit: Produit, event: Event) {
    event.stopPropagation();

    console.log('🔍 Ouverture modal commentaires - Utilisateur connecté:', this.authService.isLoggedIn());
    console.log('🔍 User ID:', this.authService.getCurrentUserId());

    this.selectedProduit = produit;
    this.showCommentModal = true;
  }

  closeCommentModal() {
    this.showCommentModal = false;
    this.selectedProduit = null;
    this.loadProduits();
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
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    const url = `${this.API_BASE_URL}/api/produits/categorie/${this.categorieId}/all`;

    this.http.get<Produit[]>(url).subscribe({
      next: (data) => {
        console.log("✅ Produits reçus:", data);

        this.produits = data.map(prod => {
          prod.imagePrincipale = prod.images && prod.images.length > 0
            ? this.getProduitImageUrl(prod.id, prod.images[0])
            : 'assets/images/placeholder.jpg';

          console.log(`📸 Produit ${prod.id} → imagePrincipale: ${prod.imagePrincipale}`);
          return prod;
        });

        this.checkAllLikes();
      },
      error: (err) => {
        console.error("❌ Erreur lors du chargement des produits:", err);
        this.produits = [];
        this.isLoading = false;
      }
    });
  }
  // Dans votre component TypeScript (category-detail.component.ts)
  getProduitsValides(): any[] {
    if (!this.produits || this.produits.length === 0) {
      return [];
    }

    // Filtrer les produits dont l'état est différent de "en_attente"
    return this.produits.filter(produit => produit.etat !== 'en_attente');
  }
  getProduitsCountByCategorie(idCategorie: number): Observable<number> {
    return this.http.get<any[]>(
      `${this.API_BASE_URL}/api/produits/categorie/${idCategorie}/all`
    ).pipe(
      map(produits =>
        produits.filter(p => p.etat !== 'en_attente').length
      )
    );
  }
  private checkAllLikes(): void {
    if (!this.authService.isLoggedIn()) {
      console.log('👤 Utilisateur non connecté - pas de vérification des likes');
      this.produits.forEach(prod => prod.hasLiked = false);
      this.isLoading = false;
      return;
    }

    console.log('🔍 Vérification des likes pour tous les produits...');

    const likeChecks = this.produits.map(produit =>
      this.interactionService.checkLike(produit.id)
    );

    forkJoin(likeChecks).subscribe({
      next: (results: boolean[]) => {
        results.forEach((isLiked, index) => {
          this.produits[index].hasLiked = isLiked;
          console.log(`❤️ Produit ${this.produits[index].id} → hasLiked: ${isLiked}`);
        });

        this.isLoading = false;
        console.log('✅ Tous les likes vérifiés:', this.produits);
      },
      error: (err) => {
        console.error('❌ Erreur lors de la vérification des likes:', err);
        this.produits.forEach(prod => prod.hasLiked = false);
        this.isLoading = false;
      }
    });
  }

  getEtatDisplayText(etat: string): string {
    switch (etat?.toLowerCase()) {
      case 'accepte':
        return 'Publié';
      case 'en_enchere':
        return 'En enchère';
      case 'vendu':
        return 'Vendu';
      case 'enchere_termine':
        return 'Enchère terminé';
      case 'relance':
          return 'En enchère';
      default:
        return etat || 'Enchère terminé';
    }
  }


}
