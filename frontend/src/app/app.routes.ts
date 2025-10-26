import { Routes } from '@angular/router';
import { AllCategoriesComponent } from '../components/client/all-categories/all-categories.component';
import { DomaineComponent } from '../components/client/domaine/domaine.component';
import { CategoryDetailComponent } from '../components/client/category-detail/category-detail.component';
import { LoginComponent } from '../components/login/login.component';
import { SignupComponent } from '../components/signup/signup.component';
import { SellComponent } from '../components/client/sell/sell.component';
import { AuthGuard } from '../services/auth.guard';
import {PubEnAttenteAdminComponent} from '../components/admin/pub-en-attente-admin/pub-en-attente-admin.component';
import {ProduitDetailsAdminComponent} from '../components/admin/produit-details-admin/produit-details-admin.component';
import {DomaineAdminComponent} from '../components/admin/domaine-admin/domaine-admin.component';
import {AllCategoriesAdminComponent} from '../components/admin/all-categories-admin/all-categories-admin.component';
import {CategoryDetailAdminComponent} from '../components/admin/category-detail-admin/category-detail-admin.component';
export const routes: Routes = [
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
    canActivate: [AuthGuard] // ✅ SEULEMENT cette route est protégée
  },
  {
    path: '',
    redirectTo: '/domaines',
    pathMatch: 'full'
  },
  { path: 'pub-en-attente-admin', component: PubEnAttenteAdminComponent },
  { path: 'produit-details-admin/:id', component: ProduitDetailsAdminComponent },
  {path: 'domaines-admin',
  component: DomaineAdminComponent
},

{ path: 'domaines-admin/:slug',
  component: AllCategoriesAdminComponent
},
{
  path: 'domaines-admin/:domaineSlug/categories-admin/:slug1/:slug2',
    component: CategoryDetailAdminComponent
},
];
