import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { NotificationComponent } from '../../notification/notification.component';

interface User {
  name: string;
  email: string;
  id?: number;
  photoProfil?: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, NotificationComponent],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {

  isAuthenticated = false;
  user: User | null = null;

  isDropdownOpen = false;
  userProfileImage: string = '';
  showProfileImage: boolean = false;

  // ⭐ Statut expert
  isExpert: boolean = false;
  isExpertActive: boolean = false;

  // ⭐ Gestion des favoris
  currentRoute: string = '';
  isFavoritesActive: boolean = false;

  private routerSubscription?: Subscription;

  constructor(private router: Router) {}

  ngOnInit() {
    this.checkAuthState();

    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.checkAuthState();
        this.updateActiveState(event.url);
      });

    this.updateActiveState(this.router.url);
  }

  ngOnDestroy() {
    this.routerSubscription?.unsubscribe();
  }

  /** ⭐ Gère l’onglet actif du profil */
  private updateActiveState(url: string) {
    this.currentRoute = url.split('?')[0];

    this.isFavoritesActive =
      url.includes('favorites') ||
      (this.currentRoute === '/profil' &&
        (this.router.getCurrentNavigation()?.extras?.state?.['activeTab'] === 'favorites' ||
          new URLSearchParams(window.location.search).get('tab') === 'favorites'));
  }

  /** ⭐ Navigation avec changement d’onglet dans le profil */
  navigateToProfileSection(section: string): void {
    this.closeDropdown();

    if (section === 'favorites') {
      this.isFavoritesActive = true;
    }

    this.router.navigate(['/profil'], {
      state: { activeTab: section },
      queryParams: { tab: section }
    });
  }

  /** ⭐ Vérification authentification + infos user + statut expert */
  private async checkAuthState() {
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');

    if (!authToken || !userData) {
      this.resetUserState();
      return;
    }

    try {
      const parsedUser = JSON.parse(userData);

      this.user = {
        name: parsedUser.nom
          ? `${parsedUser.prenom} ${parsedUser.nom}`.trim()
          : parsedUser.name || 'Utilisateur',
        email: parsedUser.email,
        id: parsedUser.id || parsedUser.userId || parsedUser.idclient,
        photoProfil: parsedUser.photoProfil
      };

      this.isAuthenticated = true;

      if (this.user.photoProfil) {
        this.userProfileImage = this.user.photoProfil;
        this.showProfileImage = true;
      } else {
        this.showProfileImage = false;
      }

      // Check expert
      await this.checkExpertStatus(this.user.id!);

    } catch (err) {
      console.error('Error parsing user data:', err);
      this.logout();
    }
  }

  /** ⭐ Vérifier statut expert via API */
  private async checkExpertStatus(clientId: number) {
    try {
      const response = await fetch(`http://localhost:8080/api/experts/check-expert/${clientId}`);
      const data = await response.json();

      this.isExpert = data.isExpert;
      this.isExpertActive = data.isActive;

    } catch (error) {
      console.error('Erreur API expert:', error);
      this.isExpert = false;
      this.isExpertActive = false;
    }
  }

  /** ⭐ Reset total du user */
  private resetUserState() {
    this.isAuthenticated = false;
    this.user = null;
    this.userProfileImage = '';
    this.showProfileImage = false;

    this.isExpert = false;
    this.isExpertActive = false;

    this.isFavoritesActive = false;
  }

  /** ⭐ Gestion Image Profil */
  onImageError() {
    this.showProfileImage = false;
  }

  /** ⭐ Dropdown */
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

  /** ⭐ Déconnexion */
  logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');

    this.resetUserState();

    window.location.href = '/';
  }

  /** ⭐ Page pour devenir expert */
  goToBecomeExpert() {
    this.closeDropdown();
    this.router.navigate(['/demande-expertise']);
  }
}
