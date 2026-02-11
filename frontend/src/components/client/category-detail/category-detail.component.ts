import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { InteractionService } from '../../../services/interaction.service';
import { CommentaireService } from '../../../services/commentaire.service';
import { AuthService } from '../../../services/auth.service';
import { CommentModalComponent } from '../comment-modal/comment-modal.component';
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

// Interface pour la réponse du like
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

  // Variables pour la modal de commentaires
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
      this.domaineSlug = params.get('domaineSlug') || '';
      this.categorieSlug = params.get('slug1') || '';
      const categorieIdParam = params.get('slug2') || '0';
      this.categorieId = parseInt(categorieIdParam, 10) || 0;

      console.log('🔥 Paramètres reçus:');
      console.log('   Domaine:', this.domaineSlug);
      console.log('   Catégorie Slug:', this.categorieSlug);
      console.log('   Catégorie ID:', this.categorieId);

      this.loadCategoryInfo();
      this.loadProduits();
    });

    this.route.queryParamMap.subscribe(queryParams => {
      this.categorieImage = queryParams.get('image') || '';
      console.log('🖼️ Image de catégorie depuis queryParams:', this.categorieImage);
    });
  }

  // ✅ NOUVELLE MÉTHODE : Redirection vers la page détail du produit
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
        console.log('✅ Informations catégorie reçues:', categoryData);

        const categoryImage = this.categorieImage || categoryData.image;

        this.currentCategory = {
          id: categoryData.idCategorie,
          nom: categoryData.nomCategorie,
          image: this.getCategorieImageUrl(categoryImage),
          description: categoryData.description
        };

        console.log('📋 Catégorie actuelle:', this.currentCategory);
      },
      error: (err) => {
        console.error('❌ Erreur chargement info catégorie:', err);
        this.currentCategory = {
          id: this.categorieId,
          nom: this.categorieSlug.replace(/-/g, ' '),
          image: this.getCategorieImageUrl(this.categorieImage),
          description: `Catégorie ${this.categorieSlug}`
        };
      }
    });
  }

  getCategorieImageUrl(imageName: string | undefined): string {
    if (!imageName || imageName.trim() === '') {
      return 'assets/images/placeholder.jpg';
    }

    if (imageName.startsWith('http') || imageName.startsWith('/') || imageName.startsWith('./')) {
      return imageName;
    }

    const cleanImageName = imageName.trim();
    return `${this.categorieImageBasePath}${cleanImageName}`;
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

    // Vérifier si l'utilisateur est connecté
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
    // Recharger les produits pour mettre à jour le compteur de commentaires
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
    const url = `${this.API_BASE_URL}/api/produits/categorie/${this.categorieId}/acceptes`;

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

        // ✅ Vérifier les likes pour chaque produit de manière SYNCHRONE
        this.checkAllLikes();
      },
      error: (err) => {
        console.error("❌ Erreur lors du chargement des produits:", err);
        this.produits = [];
        this.isLoading = false;
      }
    });
  }

  // ✅ NOUVELLE MÉTHODE : Vérifier les likes de manière synchrone
  private checkAllLikes(): void {
    if (!this.authService.isLoggedIn()) {
      console.log('👤 Utilisateur non connecté - pas de vérification des likes');
      // Si l'utilisateur n'est pas connecté, tous les coeurs seront blancs
      this.produits.forEach(prod => prod.hasLiked = false);
      this.isLoading = false;
      return;
    }

    console.log('🔍 Vérification des likes pour tous les produits...');

    // Créer un tableau d'observables pour vérifier les likes
    const likeChecks = this.produits.map(produit =>
      this.interactionService.checkLike(produit.id)
    );

    // Utiliser forkJoin pour attendre toutes les requêtes
    forkJoin(likeChecks).subscribe({
      next: (results: boolean[]) => {
        // Associer les résultats aux produits
        results.forEach((isLiked, index) => {
          this.produits[index].hasLiked = isLiked;
          console.log(`❤️ Produit ${this.produits[index].id} → hasLiked: ${isLiked}`);
        });

        this.isLoading = false;
        console.log('✅ Tous les likes vérifiés:', this.produits);
      },
      error: (err) => {
        console.error('❌ Erreur lors de la vérification des likes:', err);
        // En cas d'erreur, mettre tous les likes à false
        this.produits.forEach(prod => prod.hasLiked = false);
        this.isLoading = false;
      }
    });
  }
  // Méthode pour obtenir le texte à afficher selon l'état
  getEtatDisplayText(etat: string): string {
    switch (etat?.toLowerCase()) {
      case 'accepte':
        return 'Publié';
      case 'en_enchere':
        return 'En enchère';
      case 'vendu':
        return 'Vendu';
      default:
        return etat || 'Publié';
    }
  }

// Méthode pour obtenir la classe CSS selon l'état
  getEtatClass(etat: string): string {
    switch (etat?.toLowerCase()) {
      case 'accepte':
        return 'status-published';
      case 'en_enchere':
        return 'status-auction';
      case 'vendu':
        return 'status-sold';
      default:
        return 'status-published';
    }
  }
}
