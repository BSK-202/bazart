import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HeaderComponent} from '../components/client/header/header.component';
import {FooterComponent} from '../components/footer/footer.component';
import {CategoriesComponent} from '../components/client/categories/categories.component';
import {AllCategoriesComponent} from '../components/client/all-categories/all-categories.component';
import {DomaineComponent} from '../components/client/domaine/domaine.component';

// Import des composants PrimeNG utilisés

@Component({
  selector: 'app-root',
  standalone: true,  // ← IMPORTANT si vous utilisez des composants standalone
  imports: [
    RouterOutlet,
    HeaderComponent,// ← Ajouter le nouveau composant
    FooterComponent, // ✅ Ajouter le footer
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('Marketplace Auctions');
  // Déclaration de la variable boutons qui était manquante

}
