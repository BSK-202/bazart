import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { NotificationComponent } from '../../notification/notification.component';

interface User {
  name: string;
  email: string;
  id?: number;
  photoprofil?: string;
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

  currentRoute: string = '';
  isFavoritesActive: boolean = false;
  private routerSubscription: Subscription | undefined;

  constructor(private router: Router) {}

  ngOnInit() {
    this.checkAuthState();
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.checkAuthState();
        this.updateActiveState(event.url);
      });

    // Initialiser l'état actif
    this.updateActiveState(this.router.url);
  }

  ngOnDestroy() {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  private updateActiveState(url: string) {
    this.currentRoute = url.split('?')[0]; // Enlever les query params

    // Vérifier si on est dans la section favoris
    this.isFavoritesActive = url.includes('favorites') ||
      (this.currentRoute === '/profil' &&
        (this.router.getCurrentNavigation()?.extras?.state?.['activeTab'] === 'favorites' ||
          new URLSearchParams(window.location.search).get('tab') === 'favorites'));
  }

  navigateToProfileSection(section: string): void {
    this.closeDropdown();

    // Mettre à jour l'état actif immédiatement
    if (section === 'favorites') {
      this.isFavoritesActive = true;
    }

    // Si nous sommes déjà sur la page de profil, on utilise le state pour changer d'onglet
    if (this.router.url === '/profil' || this.router.url.startsWith('/profil')) {
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

  private async checkAuthState() {
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');

    console.log('🔍 Vérification auth state...');
    console.log('Auth Token:', authToken ? 'Present' : 'Absent');
    console.log('User Data:', userData);

    if (!authToken || !userData) {
      this.resetUserState();
      return;
    }

    try {
      const parsedUser = JSON.parse(userData);
      console.log('👤 User data parsed:', parsedUser);

      this.user = {
        name: parsedUser.nom
          ? `${parsedUser.prenom} ${parsedUser.nom}`.trim()
          : parsedUser.name || 'Utilisateur',
        email: parsedUser.email,
        id: parsedUser.id || parsedUser.userId || parsedUser.idclient,
        photoprofil: parsedUser.photoprofil
      };

      this.isAuthenticated = true;

      // Gestion de l'image de profil - CORRECTION ICI
      if (this.user.photoprofil && this.user.photoprofil.trim() !== '') {
        // Si c'est juste un nom de fichier (comme "27.jpg"), construire l'URL complète
        if (this.user.photoprofil.includes('/')) {
          // C'est déjà une URL complète
          this.userProfileImage = this.user.photoprofil;
        } else {
          // C'est juste un nom de fichier, construire l'URL complète
          this.userProfileImage = `http://localhost:8080/api/clients/images/${this.user.photoprofil}`;
        }
        this.showProfileImage = true;
        console.log('🖼️ Profile image URL:', this.userProfileImage);
        console.log('✅ Show profile image:', this.showProfileImage);
      } else {
        this.showProfileImage = false;
        this.userProfileImage = '';
        console.log('❌ No profile image available');
      }

      // Check expert status
      if (this.user.id) {
        await this.checkExpertStatus(this.user.id);
      }

    } catch (err) {
      console.error('❌ Error parsing user data:', err);
      this.logout();
    }
  }

  /** ⭐ Vérifier statut expert via API */
  private async checkExpertStatus(clientId: number) {
    try {
      console.log('🔍 Checking expert status for client:', clientId);
      const response = await fetch(`http://localhost:8080/api/experts/check-expert/${clientId}`);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      console.log('⭐ Expert status response:', data);

      this.isExpert = data.isExpert;
      this.isExpertActive = data.isActive;

      console.log('✅ Expert status - isExpert:', this.isExpert, 'isActive:', this.isExpertActive);

    } catch (error) {
      console.error('❌ Erreur API expert:', error);
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

  // Gérer l'erreur de chargement d'image
  onImageError() {
    console.log('❌ Profile image not found, using default icon');
    this.showProfileImage = false;
    this.userProfileImage = '';
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
    this.resetUserState();
    this.isDropdownOpen = false;
    window.location.href = '/';
  }

  /** ⭐ Page pour devenir expert */
  goToBecomeExpert() {
    this.closeDropdown();
    this.router.navigate(['/demande-expertise']);
  }
}
