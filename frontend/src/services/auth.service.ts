
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import {
  Auth,
  createUserWithEmailAndPassword,
  sendEmailVerification,
  signInWithEmailAndPassword,
  User,
  onAuthStateChanged  // 🆕 IMPORT AJOUTÉ
} from '@angular/fire/auth';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authTokenKey = 'authToken';
  private userDataKey = 'userData';

  constructor(private router: Router,  private firebaseAuth: Auth , private http: HttpClient) {
    this.startAutomaticEmailVerification(); // 🆕 DÉMARRAGE AUTOMATIQUE
  }

  // 🆕 MÉTHODE SIMPLIFIÉE POUR LA VÉRIFICATION AUTOMATIQUE
  private startAutomaticEmailVerification(): void {
    console.log('🚀 Démarrage de la vérification automatique email...');

    // Écouter les changements d'état d'authentification
    onAuthStateChanged(this.firebaseAuth, async (user) => {
      if (user) {
        console.log('👤 Utilisateur détecté:', user.email);
        await this.checkAndUpdateEmailVerificationForUser(user);
      } else {
        console.log('🔒 Aucun utilisateur connecté');
      }
    });

    // Vérifier périodiquement (sécurité supplémentaire)
    setInterval(async () => {
      const user = this.firebaseAuth.currentUser;
      if (user) {
        console.log('⏰ Vérification périodique pour:', user.email);
        await this.checkAndUpdateEmailVerificationForUser(user);
      }
    }, 30000); // Toutes les 30 secondes
  }

  // 🆕 MÉTHODE SIMPLIFIÉE POUR VÉRIFIER UN UTILISATEUR
  private async checkAndUpdateEmailVerificationForUser(user: User): Promise<void> {
    try {
      // Recharger les données fraîches
      await user.reload();
      const refreshedUser = this.firebaseAuth.currentUser;

      if (refreshedUser && refreshedUser.emailVerified) {
        console.log('🎉 Email vérifié détecté pour:', refreshedUser.email);

        // Mettre à jour le backend
        await this.updateEmailVerifiedInBackend(refreshedUser.email!);
        console.log('✅ Backend mis à jour avec succès');
      } else {
        console.log('⏳ Email pas encore vérifié:', user.email);
      }
    } catch (error) {
      console.error('❌ Erreur lors de la vérification:', error);
    }
  }

  // 🆕 MÉTHODE AMÉLIORÉE POUR METTRE À JOUR LE BACKEND
  private async updateEmailVerifiedInBackend(email: string): Promise<void> {
    try {
      console.log('🔄 Mise à jour du backend pour email:', email);

      await this.http.put(
        `http://localhost:8080/api/clients/verify-email/${encodeURIComponent(email)}`,
        {}
      ).toPromise();

      console.log('✅ Backend mis à jour avec succès pour:', email);
    } catch (error) {
      console.error('❌ Erreur lors de la mise à jour du backend:', error);
      // Ne pas throw l'erreur pour éviter de bloquer le processus
    }
  }

  // 🔄 MODIFICATION DE LA MÉTHODE EXISTANTE (optionnel - pour compatibilité)
  async checkAndUpdateEmailVerification(): Promise<boolean> {
    const user = this.firebaseAuth.currentUser;
    if (!user) {
      return false;
    }

    await this.checkAndUpdateEmailVerificationForUser(user);
    return user.emailVerified;
  }

  // ... TOUTES VOS AUTRES MÉTHODES RESTENT IDENTIQUES ...
  login(token: string, client: {
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
  }): void {
    console.log('Login - saving token and user data to localStorage');
    localStorage.setItem(this.authTokenKey, token);
    localStorage.setItem(this.userDataKey, JSON.stringify(client));
  }

  logout(): void {
    console.log('Logout - removing token and user data');
    localStorage.removeItem(this.authTokenKey);
    localStorage.removeItem(this.userDataKey);
    this.router.navigate(['/connexion']);
  }

  isLoggedIn(): boolean {
    const hasToken = !!localStorage.getItem(this.authTokenKey);
    console.log('isLoggedIn - localStorage has token:', hasToken);
    return hasToken;
  }

  getToken(): string | null {
    return localStorage.getItem(this.authTokenKey);
  }

  getUser(): any | null {
    const userData = localStorage.getItem(this.userDataKey);
    return userData ? JSON.parse(userData) : null;
  }

  getCurrentUserId(): number | null {
    const user = this.getUser();
    console.log('🔍 getCurrentUserId - User data:', user);
    if (user && user.idclient) {
      console.log(' ID utilisateur trouvé:', user.idclient);
      return user.idclient;
    } else {
      console.log('Aucun ID utilisateur trouvé');
      return null;
    }
  }

  redirectToLogin(message?: string) {
    this.router.navigate(['/connexion'], {
      queryParams: {
        returnUrl: this.router.url,
        message: message || 'Veuillez vous connecter'
      }
    });
  }

  async registerWithFirebase(email: string, password: string): Promise<User> {
    try {
      console.log('📝 Début de l\'inscription Firebase pour:', email);
      const userCredential = await createUserWithEmailAndPassword(this.firebaseAuth, email, password);
      console.log('✅ Compte Firebase créé, envoi de l\'email de vérification...');

      await sendEmailVerification(userCredential.user);
      console.log('✅ Email de vérification envoyé');

      return userCredential.user;
    } catch (error: any) {
      console.error('❌ Erreur lors de l\'inscription Firebase:', error);
      throw error;
    }
  }

  async loginAndCheckVerified(email: string, password: string): Promise<User> {
    try {
      console.log('🔐 Tentative de connexion pour:', email);
      const userCredential = await signInWithEmailAndPassword(this.firebaseAuth, email, password);
      const user = userCredential.user;

      if (!user.emailVerified) {
        console.warn('⚠️ Email non vérifié pour:', email);
        throw new Error('Veuillez vérifier votre email avant de vous connecter.');
      }

      console.log('✅ Connexion réussie et email vérifié');
      return user;
    } catch (error: any) {
      console.error('❌ Erreur lors de la connexion:', error);
      throw error;
    }
  }

  // 🆕 MÉTHODE : Vérifier périodiquement l'état de vérification (gardée pour compatibilité)
  startEmailVerificationCheck(email: string): Observable<boolean> {
    return new Observable(observer => {
      const checkInterval = setInterval(async () => {
        try {
          const user = this.firebaseAuth.currentUser;
          if (user) {
            await user.reload();
            if (user.emailVerified) {
              console.log('🎉 Email vérifié détecté!');

              // Mettre à jour le backend
              await this.updateEmailVerifiedInBackend(email);

              observer.next(true);
              observer.complete();
              clearInterval(checkInterval);
            }
          }
        } catch (error) {
          console.error('Erreur lors de la vérification:', error);
          observer.error(error);
          clearInterval(checkInterval);
        }
      }, 3000);

      setTimeout(() => {
        clearInterval(checkInterval);
        observer.complete();
      }, 10 * 60 * 1000);
    });
  }

  // 🆕 MÉTHODE : Forcer la vérification (pour les tests)
  async forceEmailVerificationCheck(): Promise<boolean> {
    const user = this.firebaseAuth.currentUser;
    if (user) {
      try {
        await this.checkAndUpdateEmailVerificationForUser(user);
        return user.emailVerified;
      } catch (error) {
        console.error('Erreur lors de la vérification forcée:', error);
      }
    }
    return false;
  }
}
