import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { filter, Subscription } from 'rxjs';

interface User {
  name: string;
  email: string;
}

@Component({
  selector: 'app-header-admin',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './header-admin.component.html',
  styleUrls: ['./header-admin.component.css']
})
export class HeaderAdmin implements OnInit, OnDestroy {
  isAuthenticated = false;
  isDropdownOpen = false;
  user: User | null = null;
  pendingCount = 0;
  private routerSubscription: Subscription | undefined;

  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit() {
    this.checkAuthState();
    this.loadPendingCount();

    // Vérifier l'état d'authentification à chaque changement de route
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.checkAuthState();
      });

    // Rafraîchir le compteur toutes les 30 secondes
    setInterval(() => this.loadPendingCount(), 30000);
  }

  ngOnDestroy() {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  private checkAuthState() {
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');
    console.log("Admin - Data Storage:", userData);

    if (authToken && userData) {
      try {
        const parsedUser = JSON.parse(userData);

        // Adapter selon les propriétés du backend
        this.user = {
          name: parsedUser.nom
            ? `${parsedUser.prenom} ${parsedUser.nom}`.trim()
            : parsedUser.name || 'Administrateur',
          email: parsedUser.email
        };

        this.isAuthenticated = true;
      } catch (e) {
        console.error('Erreur parsing user data:', e);
        this.logout();
      }
    } else {
      this.isAuthenticated = false;
      this.user = null;
    }
  }

  loadPendingCount() {
    this.http.get<{count: number}>(`${this.API_BASE_URL}/api/produits/en-attente/count`)
      .subscribe({
        next: (response) => {
          this.pendingCount = response.count;
        },
        error: (error) => {
          console.error('Erreur chargement compteur en attente:', error);
        }
      });
  }

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeDropdown() {
    this.isDropdownOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-dropdown')) {
      this.isDropdownOpen = false;
    }
  }

  logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
    this.isAuthenticated = false;
    this.user = null;
    this.isDropdownOpen = false;
    window.location.href = '/';
  }

}
