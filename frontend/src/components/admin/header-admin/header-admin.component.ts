import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { filter, Subscription } from 'rxjs';
import { NotificationComponent } from '../../notification/notification.component';

interface User {
  name: string;
  email: string;
}

@Component({
  selector: 'app-header-admin',
  standalone: true,
  imports: [CommonModule, RouterLink, NotificationComponent],
  templateUrl: './header-admin.component.html',
  styleUrls: ['./header-admin.component.css']
})
export class HeaderAdmin implements OnInit, OnDestroy {
  isAuthenticated = false;
  isDropdownOpen = false;
  user: User | null = null;
  adminUserId: number | null = null;
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
    const adminToken = localStorage.getItem('adminToken');
    const adminData = localStorage.getItem('adminData');

    if (adminToken && adminData) {
      try {
        const parsedAdmin = JSON.parse(adminData);

        // ✅ Vérifiez que l'ID est bien présent
        if (parsedAdmin.id) {
          this.user = {
            name: 'Administrateur',
            email: parsedAdmin.email || 'admin@bazart.ma'
          };
          this.adminUserId = parsedAdmin.id;
          this.isAuthenticated = true;
          console.log("✅ Admin authentifié, ID:", this.adminUserId);

          // ✅ Stockez aussi l'ID séparément pour plus de fiabilité
          // @ts-ignore
          localStorage.setItem('adminUserId', this.adminUserId.toString());
        } else {
          console.error("❌ ID admin manquant dans adminData");
          this.adminUserId = null;
        }
        return;
      } catch (e) {
        console.error('Erreur parsing admin data:', e);
      }
    }

    // SI PAS ADMIN, VÉRIFIER LES TOKENS UTILISATEUR NORMAL
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');
    console.log("Admin - Data Storage:", userData);

    if (authToken && userData) {
      try {
        const parsedUser = JSON.parse(userData);

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
    // SUPPRIMER TOUS LES TOKENS (admin et utilisateur)
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminData');

    this.isAuthenticated = false;
    this.user = null;
    this.isDropdownOpen = false;
    window.location.href = '/';
  }
}
