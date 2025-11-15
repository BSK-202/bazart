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

  // 🔹 NOUVEAU
  isExpert: boolean = false;
  isExpertActive: boolean = false;

  private routerSubscription?: Subscription;

  constructor(private router: Router) {}

  ngOnInit() {
    this.checkAuthState();

    // Refresh auth on navigation
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.checkAuthState();
      });
  }

  ngOnDestroy() {
    this.routerSubscription?.unsubscribe();
  }

  navigateToProfileSection(section: string): void {
    this.closeDropdown();
    this.router.navigate(['/profil'], {
      state: { activeTab: section },
      queryParams: { tab: section }
    });
  }

  private async checkAuthState() {
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');

    if (authToken && userData) {
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

        // Photo profil
        if (this.user.photoProfil) {
          this.userProfileImage = this.user.photoProfil;
          this.showProfileImage = true;
        } else {
          this.showProfileImage = false;
        }

        // 🔥 Vérifier si l'utilisateur est expert via API
        await this.checkExpertStatus(this.user.id!);

      } catch (e) {
        console.error('Error parsing user data:', e);
        this.logout();
      }
    } else {
      this.isAuthenticated = false;
      this.user = null;
      this.userProfileImage = '';
      this.showProfileImage = false;

      // Reset expert status
      this.isExpert = false;
      this.isExpertActive = false;
    }
  }

  /** 🔥 Appel API pour vérifier si cet utilisateur est expert */
  private async checkExpertStatus(clientId: number) {
    try {
      const response = await fetch(`http://localhost:8080/api/experts/check-expert/${clientId}`);
      const data = await response.json();

      this.isExpert = data.isExpert;
      this.isExpertActive = data.isActive;

      console.log('Expert status:', data);

    } catch (error) {
      console.error('Erreur API expert:', error);

      this.isExpert = false;
      this.isExpertActive = false;
    }
  }

  onImageError() {
    this.showProfileImage = false;
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
    this.userProfileImage = '';
    this.showProfileImage = false;

    this.isDropdownOpen = false;

    this.isExpert = false;
    this.isExpertActive = false;

    window.location.href = '/';
  }
  goToBecomeExpert() {
    this.closeDropdown();
    this.router.navigate(['/demande-expertise']);
  }

}
