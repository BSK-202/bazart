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
import { LoginAdminComponent } from '../components/admin/login-admin/login-admin.component';
import { AdminAuthGuard } from '../services/admin-auth.guard';

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
    component: LoginComponent
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
  {
    path: 'admin-auth',
    component: LoginAdminComponent
  },
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

  // Route de fallback (doit être la dernière)
  {
    path: '**',
    redirectTo: '/domaines'
  }
];
