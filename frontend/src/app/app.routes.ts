// app.routes.ts - VERSION SIMPLIFIÉE
import { Routes } from '@angular/router';
import { AllCategoriesComponent } from '../components/client/all-categories/all-categories.component';
import { DomaineComponent } from '../components/client/domaine/domaine.component';
import { CategoryDetailComponent } from '../components/client/category-detail/category-detail.component';
import { LoginComponent } from '../components/login/login.component';
import { SignupComponent } from '../components/signup/signup.component';
import { SellComponent } from '../components/client/sell/sell.component';
import { AuthGuard } from '../services/auth.guard';
import { PubEnAttenteAdminComponent } from '../components/admin/pub-en-attente-admin/pub-en-attente-admin.component';
import { ProduitDetailsAdminComponent } from '../components/admin/produit-details-admin/produit-details-admin.component';
import { DomaineAdminComponent } from '../components/admin/domaine-admin/domaine-admin.component';
import { AllCategoriesAdminComponent } from '../components/admin/all-categories-admin/all-categories-admin.component';
import { CategoryDetailAdminComponent } from '../components/admin/category-detail-admin/category-detail-admin.component';
import { WalletComponent } from '../components/client/wallet/wallet.component';
import { AdminAuthGuard } from '../services/admin-auth.guard';
import { UserProfileComponent } from '../components/client/profile/profile.component';
import { ProductDetailComponent } from '../components/client/Product-detail/Product-detail.component';

export const routes: Routes = [
  // ===== ROUTES PUBLIQUES CLIENT =====
  {
    path: 'domaines',
    component: DomaineComponent
  },
  {
    path: 'domaines/:slug',
    component: AllCategoriesComponent
  },
  {
    path: 'domaines/:domaineSlug/categories/:slug1/:slug2',
    component: CategoryDetailComponent
  },
  {
    path: 'connexion',
    component: LoginComponent  // ✅ UNE SEULE PAGE DE LOGIN
  },
  {
    path: 'inscription',
    component: SignupComponent
  },
  {
    path: 'vendre',
    component: SellComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'wallet',
    component: WalletComponent,
    canActivate: [AuthGuard]
  },

  // ===== ROUTES ADMIN PROTÉGÉES =====
  // ❌ SUPPRIMER la route 'admin-auth' - utiliser '/connexion' à la place
  {
    path: 'pub-en-attente-admin',
    component: PubEnAttenteAdminComponent,
    canActivate: [AdminAuthGuard]
  },
  {
    path: 'produit-details-admin/:id',
    component: ProduitDetailsAdminComponent,
    canActivate: [AdminAuthGuard]
  },
  {
    path: 'domaines-admin',
    component: DomaineAdminComponent,
    canActivate: [AdminAuthGuard]
  },
  {
    path: 'domaines-admin/:slug',
    component: AllCategoriesAdminComponent,
    canActivate: [AdminAuthGuard]
  },
  {
    path: 'domaines-admin/:domaineSlug/categories-admin/:slug1/:slug2',
    component: CategoryDetailAdminComponent,
    canActivate: [AdminAuthGuard]
  },

  // ===== ROUTES PAR DÉFAUT =====
  {
    path: '',
    redirectTo: '/domaines',
    pathMatch: 'full'
  },
  {
    path: 'produit/:id',
    component: ProductDetailComponent
  },
  {
    path: 'profil',
    component: UserProfileComponent, // ✅ Utiliser le composant directement
    canActivate: [AuthGuard] // ✅ SEULEMENT cette route est protégée

  },
  // Route de fallback (doit être la dernière)
  {
    path: '**',
    redirectTo: '/domaines'
  },


];
