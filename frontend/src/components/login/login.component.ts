// login.component.ts - VERSION SIMPLIFIÉE ET EFFICACE
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClientModule, HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { AdminAuthService } from '../../services/admin-auth.service';

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  client: {
    idclient: number;
    nom: string;
    prenom: string;
    email: string;
    tel: string;
    pays: string;
    ville: string;
    photoprofil: string;
    roles: string[];
    enabled: boolean;
  };
}

interface AdminResponse {
  id: number;
  email: string;
  nom?: string;
  prenom?: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    HttpClientModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  email: string = '';
  password: string = '';
  error: string = '';
  loading: boolean = false;
  successMessage: string = '';

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService,
    private adminAuthService: AdminAuthService
  ) {}

  ngOnInit() {
    const inscriptionSuccess = sessionStorage.getItem('inscriptionSuccess');
    const newUserEmail = sessionStorage.getItem('newUserEmail');

    if (inscriptionSuccess && newUserEmail) {
      this.successMessage = `Inscription réussie ! Vous pouvez maintenant vous connecter avec l'email: ${newUserEmail}`;
      this.email = newUserEmail;
      sessionStorage.removeItem('inscriptionSuccess');
      sessionStorage.removeItem('newUserEmail');
    }
  }

  onSubmit(event: Event) {
    event.preventDefault();
    this.error = '';
    this.loading = true;

    console.log('🔐 Tentative de connexion avec:', this.email);

    // ✅ ESSAYER D'ABORD COMME ADMIN
    this.tryAdminLogin();
  }

  private tryAdminLogin() {
    console.log('🚀 Essai de connexion ADMIN...');

    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    this.http.post<AdminResponse>(
      'http://localhost:8080/api/admins/login',
      {
        email: this.email,
        motDePasse: this.password
      },
      { headers }
    ).subscribe({
      next: (response) => {
        console.log('✅ Connexion ADMIN réussie:', response);
        this.loading = false;

        // Stocker les infos admin
        this.adminAuthService.loginAdmin(response);

        this.router.navigate(['/admin/domaines']);
      },
      error: (adminError) => {
        console.log('❌ Échec connexion admin, tentative client...');

        // ✅ SI ÉCHEC ADMIN, ESSAYER COMME CLIENT
        this.tryClientLogin();
      }
    });
  }

  private tryClientLogin() {
    console.log('🚀 Essai de connexion CLIENT...');

    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    this.http.post<AuthResponse>(
      'http://localhost:8080/api/auth/login',
      {
        email: this.email,
        password: this.password
      },
      { headers }
    ).subscribe({
      next: (response) => {
        console.log('✅ Connexion CLIENT réussie:', response);
        this.loading = false;

        this.authService.login(response.accessToken, response.client);

        this.router.navigate(['/domaines']).then(() => {
          window.location.reload();
        });
      },
      error: (clientError) => {
        this.loading = false;
        console.error('❌ Échec connexion client aussi:', clientError);

        // ✅ LES DEUX ONT ÉCHOUÉ - AFFICHER ERREUR
        this.handleFinalError();
      }
    });
  }

  private handleFinalError() {
    this.loading = false;
    this.error = 'Email ou mot de passe incorrect';
    console.log('🔍 Les deux types de connexion ont échoué pour:', this.email);
  }
}
