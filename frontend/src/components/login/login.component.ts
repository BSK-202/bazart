import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClientModule, HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { getAuth, signInWithEmailAndPassword } from 'firebase/auth';

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

interface Admin {
  id: number;
  email: string;
  motDePasse: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HttpClientModule],
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
    const inscriptionSuccess = sessionStorage.getItem('inscriptionSuccess');
    const newUserEmail = sessionStorage.getItem('newUserEmail');

    if (inscriptionSuccess && newUserEmail) {
      this.successMessage = `Inscription réussie ! Un email de vérification a été envoyé à ${newUserEmail}. Veuillez vérifier votre adresse email avant de vous connecter.`;
      this.email = newUserEmail;
      sessionStorage.removeItem('inscriptionSuccess');
      sessionStorage.removeItem('newUserEmail');
    }
  }

  onSubmit(event: Event) {
    event.preventDefault();
    this.error = '';
    this.loading = true;

    console.log('🔹 Tentative de connexion avec email:', this.email);

    // Étape 1: Vérifier Firebase D'ABORD pour les clients
    this.checkClientLoginFirst();
  }

  private checkClientLoginFirst() {
    console.log('1️⃣ Vérification CLIENT Firebase en premier...');
    const auth = getAuth();

    signInWithEmailAndPassword(auth, this.email, this.password)
      .then((userCredential) => {
        const user = userCredential.user;
        console.log('✅ Firebase authentification réussie - C\'est un CLIENT');

        if (!user.emailVerified) {
          this.error = "Merci de vérifier votre adresse email avant de vous connecter.";
          this.loading = false;
          return;
        }

        // C'est un client Firebase - créer un client temporaire
        this.createClientFromFirebase(user);

      })
      .catch((firebaseError) => {
        console.error('❌ Erreur Firebase - Ce n\'est pas un client, vérification ADMIN...', firebaseError);

        // Si ce n'est pas un client Firebase, vérifier si c'est un admin
        this.checkAdminLogin();
      });
  }

  private createClientFromFirebase(user: any) {
    console.log('🔄 Création client depuis Firebase...');

    const tempClient: Client = {
      idclient: Date.now(), // ID temporaire
      nom: user.displayName?.split(' ')[0] || 'Utilisateur',
      prenom: user.displayName?.split(' ')[1] || '',
      email: user.email || this.email,
      tel: '',
      pays: '',
      ville: '',
      photoprofil: user.photoURL || '',
      roles: ['CLIENT'],
      enabled: true
    };

    // Utiliser login de votre AuthService existant
    this.authService.login('firebase-token', tempClient);

    console.log('✅ CLIENT connecté - Redirection vers /domaines');

    // REDIRECTION VERS INTERFACE CLIENT
    this.router.navigate(['/domaines']).then(() => {
      window.location.reload();
    });
    this.loading = false;

    // Essayer Spring Boot en arrière-plan (optionnel)
    this.trySpringBootSync();
  }

  private checkAdminLogin() {
    console.log('2️⃣ Vérification ADMIN local...');
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    this.http.post<Admin>(
      'http://localhost:8080/api/admins/login',
      { email: this.email, motDePasse: this.password },
      { headers }
    ).subscribe({
      next: (adminResponse) => {
        console.log('✅ ADMIN connecté avec succès');
        this.authService.loginAdmin('admin-token', adminResponse);

        console.log('🔄 Redirection vers /domaines-admin');
        this.router.navigate(['/domaines-admin']).then(() => {
          window.location.reload();
        });
        this.loading = false;
      },
      error: (adminError) => {
        console.error('❌ Ce n\'est ni un client ni un admin');

        // Aucun des deux - erreur d'authentification
        if (adminError.status === 401 || adminError.status === 404) {
          this.error = 'Email ou mot de passe incorrect';
        } else if (adminError.status === 0) {
          this.error = 'Serveur indisponible. Veuillez réessayer plus tard.';
        } else {
          this.error = 'Identifiants incorrects';
        }
        this.loading = false;
      }
    });
  }

  private trySpringBootSync() {
    // Tentative de synchronisation avec Spring Boot en arrière-plan
    console.log('🔄 Tentative de synchronisation avec Spring Boot...');
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    this.http.post<AuthResponse>(
      'http://localhost:8080/api/auth/login',
      { email: this.email, password: this.password },
      { headers }
    ).subscribe({
      next: (response) => {
        console.log('✅ Synchronisation Spring Boot réussie');
        // Mettre à jour avec les vraies données Spring Boot
        this.authService.login(response.accessToken, response.client);
      },
      error: (error) => {
        console.log('ℹ️ Spring Boot non disponible - continuation avec Firebase');
        // On continue avec Firebase, pas d'erreur pour l'utilisateur
      }
    });
  }
}
