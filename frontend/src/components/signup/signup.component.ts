import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { AuthService } from '../../services/auth.service';

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
  selectedFile: File | null = null;

  constructor(
    private router: Router,
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private async signup(clientData: any, password: string): Promise<RegisterResponse> {
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

  private async uploadProfileImage(clientId: number, file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    // Ne pas ajouter clientId dans FormData car il est déjà dans l'URL

    const response = await lastValueFrom(
      this.http.post<{fileName: string}>(
        `http://localhost:8080/api/clients/${clientId}/upload-profile-image`,
        formData
      )
    );

    return response.fileName;
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
    sessionStorage.setItem('inscriptionSuccess', 'true');
    sessionStorage.setItem('newUserEmail', this.email);
    this.router.navigate(['/connexion']);
  }

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
    this.selectedFile = null;
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Vérifier le type de fichier
      if (!file.type.match('image.*')) {
        this.error = 'Veuillez sélectionner une image valide (JPG, PNG, GIF)';
        return;
      }

      // Vérifier la taille du fichier (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.error = 'L\'image ne doit pas dépasser 5MB';
        return;
      }

      this.selectedFileName = file.name;
      this.selectedFile = file;
      this.photoprofil = file.name; // Nom temporaire pour l'affichage
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
        photoprofil: this.selectedFile ? this.selectedFile.name : '' // Nom temporaire
      };

      console.log('📝 Début de l\'inscription...');
      const response = await this.signup(clientData, this.password);
      console.log('✅ Inscription réussie, ID client:', response.idclient);

      // Upload de l'image si une image a été sélectionnée
      if (this.selectedFile && response.idclient) {
        try {
          console.log('📤 Début de l\'upload de l\'image...');
          const fileName = await this.uploadProfileImage(response.idclient, this.selectedFile);
          console.log('✅ Image uploadée avec succès:', fileName);

          // Mettre à jour le nom de fichier dans la base de données
          console.log('🔄 Mise à jour du nom de l\'image dans la BDD...');
          await this.updateProfileImageName(response.idclient, fileName);
          console.log('✅ Nom de l\'image mis à jour dans la BDD');
        } catch (uploadError: any) {
          console.error('❌ Erreur lors de l\'upload de l\'image:', uploadError);
          console.error('Détails de l\'erreur:', uploadError.status, uploadError.message);
          // Ne pas bloquer l'inscription si l'upload échoue
        }
      }

      // Inscription sur Firebase pour email verification
      try {
        await this.authService.registerWithFirebase(this.email, this.password);
        alert("Un email de vérification vous a été envoyé. Vérifiez votre boîte mail.");
      } catch (firebaseError: any) {
        console.error("Erreur Firebase (email vérif) :", firebaseError);
        alert("Attention : votre compte a bien été créé, mais l'envoi de l'email de vérification a échoué.");
      }

      this.redirectWithSuccess();

    } catch (error: any) {
      console.error('❌ Erreur lors de l\'inscription:', error);
      this.handleError(error);
    } finally {
      this.loading = false;
    }
  }

  private async updateProfileImageName(clientId: number, fileName: string): Promise<void> {
    const updateData = { photoprofil: fileName };

    await lastValueFrom(
      this.http.patch(
        `http://localhost:8080/api/clients/${clientId}/profile-image`,
        updateData
      )
    );
  }
}
