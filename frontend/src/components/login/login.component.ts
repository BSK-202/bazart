// login.component.ts - VERSION CORRIGÉE
import {Component, OnInit, inject, runInInjectionContext, Injector} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClientModule, HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

// IMPORT CORRECT POUR FIREBASE
import { Auth, signInWithEmailAndPassword } from '@angular/fire/auth';

interface Client {
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
}

interface AuthResponse {
  accessToken: string;
  tokenType: string;
  client: Client;
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
  // INJECTION CORRECTE DE AUTH
  private auth: Auth = inject(Auth);
  private router = inject(Router);
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private injector = inject(Injector); // Ajouter Injector

  email: string = '';
  password: string = '';
  error: string = '';
  loading: boolean = false;
  successMessage: string = '';

  ngOnInit() {
    const inscriptionSuccess = sessionStorage.getItem('inscriptionSuccess');
    const newUserEmail = sessionStorage.getItem('newUserEmail');

    if (inscriptionSuccess && newUserEmail) {
      this.successMessage = `Inscription réussie ! Un email de vérification a été envoyé à ${newUserEmail}. Veuillez vérifier votre adresse email avant de vous connecter.`;
      this.email = newUserEmail;

      sessionStorage.removeItem('inscriptionSuccess');
      sessionStorage.removeItem('newUserEmail');
    }
  }

  async onSubmit(event: Event) {
    event.preventDefault();
    this.error = '';
    this.loading = true;

    try {
      console.log('🟡 Tentative de connexion Firebase avec:', this.email);

      // UTILISER runInInjectionContext POUR APPELER FIREBASE
      const userCredential = await runInInjectionContext(this.injector, () => {
        return signInWithEmailAndPassword(
          this.auth,
          this.email,
          this.password
        );
      });

      const user = userCredential.user;

      if (!user.emailVerified) {
        this.error = "Merci de vérifier votre adresse email avant de vous connecter.";
        this.loading = false;
        return;
      }

      // Login backend
      const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
      this.http.post<AuthResponse>(
        'http://localhost:8080/api/auth/login',
        { email: this.email, password: this.password },
        { headers: headers }
      ).subscribe({
        next: (response) => {
          console.log('✅ Connexion réussie:', response);

          // Stocker token et info client
          this.authService.login(response.accessToken, response.client);

          // Rediriger vers domaines
          this.router.navigate(['/domaines']).then(() => {
            window.location.reload();
          });
        },
        error: (error) => {
          console.error('❌ Erreur backend:', error);
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

    } catch (error: any) {
      console.error('❌ Erreur Firebase:', error);
      if (error.code === 'auth/wrong-password' || error.code === 'auth/user-not-found') {
        this.error = 'Email ou mot de passe incorrect';
      } else if (error.code === 'auth/invalid-email') {
        this.error = 'Format d\'email invalide';
      } else {
        this.error = 'Erreur de connexion: ' + error.message;
      }
      this.loading = false;
    }
  }
}
