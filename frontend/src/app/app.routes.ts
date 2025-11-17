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
import { ExpertsInactifsComponent } from '../components/admin/experts-inactif-admin/experts-inactif-admin.component';
import { ExpertDetailsComponent } from '../components/admin/expert-details-admin/expert-details-admin.component';
import { ExpertsActifsAdminComponent } from '../components/admin/experts-actifs-admin/experts-actifs-admin.component';
import { SingupexpertComponent } from '../components/signupexpert/signupexpert.component';
import {EncheresComponent} from '../components/client/echeres/encheres.component';

export const routes: Routes = [
  // ===== PUBLIC CLIENT ROUTES =====
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
    path: 'produit/:id',
    component: ProductDetailComponent
  },
  {
    path: 'encheres',
    component: EncheresComponent ,
    canActivate: [AuthGuard]// Vous devrez créer ce composant
  },

  // ===== AUTHENTICATION ROUTES =====
  {
    path: 'connexion',
    component: LoginComponent
  },
  {
    path: 'inscription',
    component: SignupComponent
  },
  {
    path: 'demande-expertise',
    component: SingupexpertComponent
  },

  // ===== PROTECTED CLIENT ROUTES =====
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
  {
    path: 'profil',
    component: UserProfileComponent,
    canActivate: [AuthGuard]
  },

  // ===== ADMIN PROTECTED ROUTES =====
  {
    path: 'admin',
    children: [
      {
        path: 'publications',
        component: PubEnAttenteAdminComponent
      },
      {
        path: 'publications/:id',
        component: ProduitDetailsAdminComponent
      },
      {
        path: 'domaines',
        component: DomaineAdminComponent
      },
      {
        path: 'domaines/:slug',
        component: AllCategoriesAdminComponent
      },
      {
        path: 'domaines/:domaineSlug/categories/:slug1/:slug2',
        component: CategoryDetailAdminComponent
      },
      {
        path: 'experts-inactifs',
        component: ExpertsInactifsComponent
      },
      {
        path: 'experts-inactifs/:id',
        component: ExpertDetailsComponent
      },
      {
        path: 'experts-actifs',
        component: ExpertsActifsAdminComponent
      },
      // NOUVELLE ROUTE POUR LES ENCHÈRES

    ]
  },

  // ===== REDIRECT ROUTES =====
  {
    path: '',
    redirectTo: '/domaines',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/domaines'
  }
];
