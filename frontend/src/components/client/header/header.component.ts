import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';

interface User {
  name: string;
  email: string;
  id?: number;
  photoProfil?: string; // URL complète de l'image
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  isAuthenticated = false;
  user: User | null = null;
  isDropdownOpen = false;
  userProfileImage: string = '';
  showProfileImage: boolean = false;
  private routerSubscription: Subscription | undefined;

  constructor(private router: Router) {}

  ngOnInit() {
    this.checkAuthState();
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.checkAuthState();
      });
  }

  ngOnDestroy() {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }


  navigateToProfileSection(section: string): void {
    this.closeDropdown();

    // Si nous sommes déjà sur la page de profil, on utilise le state pour changer d'onglet
    if (this.router.url === '/profil' || this.router.url.startsWith('/profil')) {
      // Émettre un événement ou utiliser un service pour communiquer avec le composant profil
      this.router.navigate(['/profil'], {
        state: { activeTab: section },
        queryParams: { tab: section }
      });
    } else {
      // Si nous ne sommes pas sur le profil, on navigue vers le profil avec l'état
      this.router.navigate(['/profil'], {
        state: { activeTab: section },
        queryParams: { tab: section }
      });
    }
  }

  private checkAuthState() {
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');
    console.log("dataStorage:", userData);

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

        if (this.user.photoProfil) {
          this.userProfileImage = this.user.photoProfil;
          this.showProfileImage = true;
          console.log('🖼️ Profile image URL from auth service:', this.userProfileImage);
        } else {
          this.showProfileImage = false;
          console.log('❌ No profile image URL found');
        }

        this.isAuthenticated = true;
      } catch (e) {
        console.error('Error parsing user data:', e);
        this.logout();
      }
    } else {
      this.isAuthenticated = false;
      this.user = null;
      this.userProfileImage = '';
      this.showProfileImage = false;
    }
  }

  // Gérer l'erreur de chargement d'image
  onImageError() {
    console.log('Profile image not found, using default icon');
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
    window.location.href = '/';
  }
}
