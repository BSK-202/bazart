// auth.service.ts - VERSION FINALE CORRIGÉE
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Auth, createUserWithEmailAndPassword, sendEmailVerification, signInWithEmailAndPassword } from '@angular/fire/auth';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authTokenKey = 'authToken';
  private userDataKey = 'userData';

  constructor(private router: Router,  private firebaseAuth: Auth) {}

  login(token: string, client: {
    idclient: number;
    nom: string;
    prenom: string;
    email: string;
    tel: string;
    pays: string;
    ville: string;
    photoprofil: string;
    photoProfil?: string; // 🆕 Ajouter le champ avec majuscule
    roles: string[];
    enabled: boolean;
  }): void {
    console.log('Login - saving token and user data to localStorage');

    // 🆕 CORRECTION : Construire l'URL complète de l'image


    const userDataToStore = {
      ...client,
      // Si le backend n'envoie pas photoProfil, le construire nous-mêmes
      photoProfil: client.photoProfil || this.buildProfileImageUrl(client.photoprofil, client.idclient)
    };



    localStorage.setItem(this.authTokenKey, token);
    localStorage.setItem(this.userDataKey, JSON.stringify(userDataToStore)); // ✅ Sauvegarde les données AVEC l'URL
    localStorage.removeItem('currentUser');

    console.log('✅ User data stored with profile image:', userDataToStore.photoProfil);
  }

// 🆕 Méthode pour construire l'URL de l'image de profil
  private buildProfileImageUrl(fileName: string, userId: number): string | null {
    if (!fileName) return null;

    // Si c'est déjà une URL complète, la retourner telle quelle
    if (fileName.startsWith('http')) {
      return fileName;
    }

    // Construire l'URL via le backend Spring Boot
    return `http://localhost:8080/api/clients/images/${fileName}`;
  }
// auth.service.ts - AJOUTER LA MÉTHODE
  logout(): void {
    console.log('Logout - removing token and user data');
    localStorage.removeItem(this.authTokenKey);
    localStorage.removeItem(this.userDataKey);

    // ✅ NE PAS supprimer les données admin quand un client se déconnecte
    // localStorage.removeItem('admin_data'); // ❌ À RETIRER

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
  loginAdmin(token: string, admin: any) {
    localStorage.setItem(this.authTokenKey, token);
    localStorage.setItem(this.userDataKey, JSON.stringify(admin));
    localStorage.removeItem('client_token');
  }
  // ✅ AJOUTER CETTE MÉTHODE MANQUANTE
  redirectToLogin(message?: string) {
    this.router.navigate(['/connexion'], {
      queryParams: {
        returnUrl: this.router.url,
        message: message || 'Veuillez vous connecter'
      }
    });
  }
  // ===== Firebase Registration & Verification =====
  async registerWithFirebase(email: string, password: string): Promise<void> {
    const userCredential = await createUserWithEmailAndPassword(this.firebaseAuth, email, password);
    await sendEmailVerification(userCredential.user);
  }

  async loginAndCheckVerified(email: string, password: string): Promise<void> {
    const userCredential = await signInWithEmailAndPassword(this.firebaseAuth, email, password);
    if (!userCredential.user.emailVerified) {
      throw new Error('Please verify your email before logging in.');
    }
  }


}
