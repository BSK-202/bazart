/*import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HeaderComponent} from '../components/header/header.component';
import { HeaderAdmin } from '../components/header-admin/header-admin.component';

import {FooterComponent} from '../components/footer/footer.component';
import {CategoriesComponent} from '../components/categories/categories.component';
import {AllCategoriesComponent} from '../components/all-categories/all-categories.component';
import {DomaineComponent} from '../components/domaine/domaine.component';
import {LoginComponent} from '../components/login/login.component';

// Import des composants PrimeNG utilisés

@Component({
  selector: 'app-root',
  standalone: true,  // ← IMPORTANT si vous utilisez des composants standalone
  imports: [
    RouterOutlet,
    HeaderComponent,// ← Ajouter le nouveau composant
    FooterComponent, // ✅ Ajouter le footer
    LoginComponent,
    HeaderAdmin,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})

export class App {

  protected readonly title = signal('Marketplace Auctions');
  // Déclaration de la variable boutons qui était manquante

}*/
import { Component, signal } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../components/client/header/header.component';
import { HeaderAdmin } from '../components/admin/header-admin/header-admin.component';
import { FooterComponent } from '../components/footer/footer.component';
import { LoginComponent } from '../components/login/login.component';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,

    HeaderComponent,
    HeaderAdmin,
    FooterComponent,
    LoginComponent
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  title = signal('Marketplace Auctions');
  isAdminRoute = false;

  constructor(private router: Router) {
    // On écoute les changements de route
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      // Vérifie si l'URL contient "admin"
      this.isAdminRoute = event.urlAfterRedirects.includes('admin');
      console.log('Route admin ?', this.isAdminRoute);
    });
  }
}

