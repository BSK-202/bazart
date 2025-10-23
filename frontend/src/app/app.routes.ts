import { Routes } from '@angular/router';
import { AllCategoriesComponent } from '../components/client/all-categories/all-categories.component';
import { DomaineComponent } from '../components/client/domaine/domaine.component';
import { CategoryDetailComponent } from '../components/client/category-detail/category-detail.component';
import { LoginComponent } from '../components/login/login.component';
import { SignupComponent } from '../components/signup/signup.component';
import { SellComponent } from '../components/client/sell/sell.component';
import { AuthGuard } from '../services/auth.guard';

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
  }
];
