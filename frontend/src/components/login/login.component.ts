// login.component.ts - VERSION CORRIGÉE
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClientModule, HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  client: {  // CHANGÉ: 'user' → 'client'
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
    private authService: AuthService
  ) {}

  ngOnInit() {
    // Vérifier s'il y a un message de succès d'inscription
    const inscriptionSuccess = sessionStorage.getItem('inscriptionSuccess');
    const newUserEmail = sessionStorage.getItem('newUserEmail');

    if (inscriptionSuccess && newUserEmail) {
      this.successMessage = `Inscription réussie ! Vous pouvez maintenant vous connecter avec l'email: ${newUserEmail}`;
      this.email = newUserEmail; // Pré-remplir l'email

      // Nettoyer le sessionStorage
      sessionStorage.removeItem('inscriptionSuccess');
      sessionStorage.removeItem('newUserEmail');
    }
  }

  onSubmit(event: Event) {
    event.preventDefault();
    this.error = '';
    this.loading = true;

    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    this.http.post<AuthResponse>(
      'http://localhost:8080/api/auth/login',
      {
        email: this.email,
        password: this.password
      },
      { headers: headers }
    ).subscribe({
      next: (response) => {
        console.log('✅ Connexion réussie:', response);

        // Stocker le token ET les informations du client
        this.authService.login(response.accessToken, response.client);

        // Rediriger vers la page d'accueil
        this.router.navigate(['/domaines']).then(() => {
          // Recharger la page pour mettre à jour l'état d'authentification
          window.location.reload();
        });
      },
      error: (error) => {
        console.error('❌ Erreur de connexion:', error);

        if (error.status === 401) {
          this.error = 'Email ou mot de passe incorrect';
        } else if (error.status === 0) {
          this.error = 'Impossible de contacter le serveur. Vérifiez que Spring Boot est démarré.';
        } else {
          this.error = 'Une erreur est survenue lors de la connexion';
        }

        this.loading = false;
      }
    });
  }
}
