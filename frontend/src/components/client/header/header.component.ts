import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';

interface User {
  name: string;
  email: string;
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
  private routerSubscription: Subscription | undefined;

  constructor(private router: Router) {}

  ngOnInit() {
    this.checkAuthState();
    // Vérifier l'état d'authentification à chaque changement de route
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

  private checkAuthState() {
    const authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('userData');
    console.log("dtaStorage;",userData);

    if (authToken && userData) {
      try {
        const parsedUser = JSON.parse(userData);

        // Adapter selon les propriétés du backend
        this.user = {
          name: parsedUser.nom
            ? `${parsedUser.prenom} ${parsedUser.nom}`.trim()
            : parsedUser.name || 'Utilisateur',
          email: parsedUser.email
        };

        this.isAuthenticated = true;
      } catch (e) {
        this.logout();
      }
    } else {
      this.isAuthenticated = false;
      this.user = null;
    }
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
