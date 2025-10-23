import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
interface ClientDto {
  idclient?: number;
  nom: string;
  prenom: string;
  email: string;
  tel: string;
  pays: string;
  ville: string;
  photoprofil: string;
  roles?: string[];
  enabled?: boolean;
}
interface RegisterResponse {
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
@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  // Informations personnelles
  nom: string = '';
  prenom: string = '';
  email: string = '';
  tel: string = '';
  pays: string = '';
  ville: string = '';
  photoprofil: string = '';
  // Mot de passe
  password: string = '';
  confirmPassword: string = '';
  error: string = '';
  loading: boolean = false;
  selectedFileName: string = '';
  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService
  ) {}
  private signup(clientData: any, password: string): Promise<RegisterResponse> {
    const registerData = {
      nom: clientData.nom,
      prenom: clientData.prenom,
      email: clientData.email,
      password: password,
      tel: clientData.tel,
      pays: clientData.pays,
      ville: clientData.ville,
      photoprofil: clientData.photoprofil
    };
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    return lastValueFrom(
      this.http.post<RegisterResponse>(
        'http://localhost:8080/api/auth/register',
        registerData,
        { headers }
      )
    );
  }
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }
  private handleError(error: any): void {
    if (error.status === 400) {
      if (error.error && error.error.message) {
        this.error = error.error.message;
      } else {
        this.error = 'Données invalides. Vérifiez les informations saisies.';
      }
    } else if (error.status === 409) {
      this.error = 'Un compte avec cet email existe déjà.';
    } else if (error.status === 0) {
      this.error = 'Impossible de se connecter au serveur. Vérifiez que le serveur est démarré.';
    } else if (error.error && error.error.message) {
      this.error = error.error.message;
    } else {
      this.error = 'Une erreur est survenue lors de l\'inscription. Veuillez réessayer.';
    }
  }
  private redirectWithSuccess() {
    // Stocker un message de succès temporaire
    sessionStorage.setItem('inscriptionSuccess', 'true');
    sessionStorage.setItem('newUserEmail', this.email);
    this.router.navigate(['/connexion']);
  }
  // Méthode pour réinitialiser le formulaire
  resetForm() {
    this.nom = '';
    this.prenom = '';
    this.email = '';
    this.tel = '';
    this.pays = '';
    this.ville = '';
    this.photoprofil = '';
    this.password = '';
    this.confirmPassword = '';
    this.error = '';
    this.selectedFileName = '';
  }
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Vérifier le type de fichier
      if (!file.type.match('image.*')) {
        this.error = 'Veuillez sélectionner une image valide';
        return;
      }
      // Vérifier la taille du fichier (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.error = 'L\'image ne doit pas dépasser 5MB';
        return;
      }
      this.selectedFileName = file.name;
      this.photoprofil = file.name;
      this.error = '';
    }
  }
  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    // Validation basique
    if (this.password !== this.confirmPassword) {
      this.error = 'Les mots de passe ne correspondent pas';
      return;
    }
    // Validation des champs requis
    if (!this.nom || !this.prenom || !this.email || !this.pays || !this.ville || !this.password) {
      this.error = 'Veuillez remplir tous les champs obligatoires';
      return;
    }
    // Validation de l'email
    if (!this.isValidEmail(this.email)) {
      this.error = 'Veuillez saisir une adresse email valide';
      return;
    }
    // Validation de la longueur du mot de passe
    if (this.password.length < 6) {
      this.error = 'Le mot de passe doit contenir au moins 6 caractères';
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      // Appel à l'API d'inscription
      const clientData = {
        nom: this.nom,
        prenom: this.prenom,
        email: this.email,
        tel: this.tel,
        pays: this.pays,
        ville: this.ville,
        photoprofil: this.photoprofil
      };
      // ICI on appelle l'API avec les données en JSON
      const response = await this.signup(clientData, this.password);

      // Inscription réussie - redirection vers la page de connexion
      console.log('Inscription réussie:', response);
      // === NOUVEAU : Inscription sur Firebase pour email verification ===
      try {
        await this.authService.registerWithFirebase(this.email, this.password);
        // Optionnel : affiche un message de succès supplémentaire
        alert("Un email de vérification vous a été envoyé. Vérifiez votre boîte mail.");
      } catch (firebaseError: any) {
        // Ne bloque PAS l'inscription backend si Firebase échoue, mais informe l'utilisateur
        console.error("Erreur Firebase (email vérif) :", firebaseError);
        alert("Attention : votre compte a bien été créé, mais l'envoi de l'email de vérification a échoué.");
      }

      this.redirectWithSuccess();

    } catch (error: any) {
      console.error('Erreur lors de l\'inscription:', error);
      this.handleError(error);
    } finally {
      this.loading = false;
    }
  }
}
