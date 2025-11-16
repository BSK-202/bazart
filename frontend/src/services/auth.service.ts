import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import {
  Auth,
  createUserWithEmailAndPassword,
  sendEmailVerification,
  signInWithEmailAndPassword,
  User,
  onAuthStateChanged
} from '@angular/fire/auth';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authTokenKey = 'authToken';
  private userDataKey = 'userData';

  constructor(
    private router: Router,
    private firebaseAuth: Auth,
    private http: HttpClient
  ) {
    this.startAutomaticEmailVerification();
  }

  // ---------------------------------------
  // 1️⃣ VERIFICATION AUTOMATIQUE EMAIL
  // ---------------------------------------
  private startAutomaticEmailVerification(): void {
    console.log('🚀 Email auto-check started...');

    onAuthStateChanged(this.firebaseAuth, async (user) => {
      if (user) {
        console.log('👤 User detected:', user.email);
        await this.checkAndUpdateEmailVerificationForUser(user);
      } else {
        console.log('🔒 No user connected');
      }
    });

    setInterval(async () => {
      const user = this.firebaseAuth.currentUser;
      if (user) {
        console.log('⏰ Periodic check for:', user.email);
        await this.checkAndUpdateEmailVerificationForUser(user);
      }
    }, 30000);
  }

  private async checkAndUpdateEmailVerificationForUser(user: User): Promise<void> {
    try {
      await user.reload();
      const refreshedUser = this.firebaseAuth.currentUser;

      if (refreshedUser?.emailVerified) {
        console.log('🎉 Email verified:', refreshedUser.email);
        await this.updateEmailVerifiedInBackend(refreshedUser.email!);
      } else {
        console.log('⏳ Email not verified:', user.email);
      }
    } catch (error) {
      console.error('❌ Error during email verification:', error);
    }
  }

  private async updateEmailVerifiedInBackend(email: string): Promise<void> {
    try {
      await this.http.put(
        `http://localhost:8080/api/clients/verify-email/${encodeURIComponent(email)}`,
        {}
      ).toPromise();
      console.log('✅ Backend updated for:', email);
    } catch (error) {
      console.error('❌ Backend update error:', error);
    }
  }

  // ---------------------------------------
  // 2️⃣ LOGIN + LOCALSTORAGE
  // ---------------------------------------
  login(
    token: string,
    client: {
      idclient: number;
      nom: string;
      prenom: string;
      email: string;
      tel: string;
      pays: string;
      ville: string;
      photoProfil?: string;
      roles: string[];
      enabled: boolean;
    }
  ): void {

    const finalPhoto = this.buildProfileImageUrl(client.photoProfil, client.idclient);

    const userDataToStore = {
      ...client,
      photoProfil: finalPhoto
    };

    localStorage.setItem(this.authTokenKey, token);
    localStorage.setItem(this.userDataKey, JSON.stringify(userDataToStore));
    localStorage.removeItem('currentUser');

    console.log('👤 User logged in:', userDataToStore);
  }

  logout(): void {
    localStorage.removeItem(this.authTokenKey);
    localStorage.removeItem(this.userDataKey);
    this.router.navigate(['/connexion']);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.authTokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.authTokenKey);
  }

  getUser(): any | null {
    const data = localStorage.getItem(this.userDataKey);
    return data ? JSON.parse(data) : null;
  }

  getCurrentUserId(): number | null {
    const user = this.getUser();
    return user?.idclient ?? null;
  }

  redirectToLogin(message?: string) {
    this.router.navigate(['/connexion'], {
      queryParams: {
        returnUrl: this.router.url,
        message: message || 'Veuillez vous connecter'
      }
    });
  }

  // ---------------------------------------
  // 3️⃣ FIREBASE REGISTER & LOGIN
  // ---------------------------------------
  async registerWithFirebase(email: string, password: string): Promise<User> {
    const userCredential = await createUserWithEmailAndPassword(
      this.firebaseAuth,
      email,
      password
    );

    await sendEmailVerification(userCredential.user);
    return userCredential.user;
  }

  async loginAndCheckVerified(email: string, password: string): Promise<User> {
    const userCredential = await signInWithEmailAndPassword(
      this.firebaseAuth,
      email,
      password
    );

    const user = userCredential.user;

    if (!user.emailVerified) {
      throw new Error('Veuillez vérifier votre email avant de vous connecter.');
    }

    return user;
  }

  // ---------------------------------------
  // 4️⃣ UTILITAIRE IMAGE
  // ---------------------------------------
  private buildProfileImageUrl(fileName?: string, userId?: number): string | null {
    if (!fileName) return null;
    if (fileName.startsWith('http')) return fileName;

    return `http://localhost:8080/api/clients/images/${fileName}`;
  }

  // ---------------------------------------
  // 5️⃣ OBSERVABLE CHECK (OPTIONNEL)
  // ---------------------------------------
  startEmailVerificationCheck(email: string): Observable<boolean> {
    return new Observable((observer) => {
      const interval = setInterval(async () => {
        const user = this.firebaseAuth.currentUser;
        if (user) {
          await user.reload();
          if (user.emailVerified) {
            await this.updateEmailVerifiedInBackend(email);
            observer.next(true);
            observer.complete();
            clearInterval(interval);
          }
        }
      }, 3000);

      setTimeout(() => {
        clearInterval(interval);
        observer.complete();
      }, 10 * 60 * 1000);
    });
  }

  async forceEmailVerificationCheck(): Promise<boolean> {
    const user = this.firebaseAuth.currentUser;
    if (user) {
      await this.checkAndUpdateEmailVerificationForUser(user);
      return user.emailVerified;
    }
    return false;
  }
}
