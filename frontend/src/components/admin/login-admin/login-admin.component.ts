import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface AdminResponse {
  id: number;
  email: string;

}
@Component({
  selector: 'app-login-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login-admin.component.html',
  styleUrls: ['./login-admin.component.css']
})

export class LoginAdminComponent {
  loginData = {
    email: '',
    motDePasse: ''
  };

  isLoading = false;
  errorMessage = '';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  onSubmit() {
    // Validation des champs
    if (!this.loginData.email || !this.loginData.motDePasse) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    console.log('🔐 Tentative de connexion admin:', this.loginData.email);

    this.http.post<AdminResponse>('http://localhost:8080/api/admins/login', this.loginData)
      .subscribe({
        next: (response) => {
          this.isLoading = false;

          if (response) {
            console.log('✅ Connexion admin réussie:', response);

            // Stocker les informations de l'admin
            localStorage.setItem('adminToken', 'admin-authenticated');
            localStorage.setItem('adminData', JSON.stringify(response));

            // Redirection vers la page des domaines admin
            this.router.navigate(['/domaines-admin']);
          } else {
            this.errorMessage = 'Email ou mot de passe incorrect';
          }
        },
        error: (error) => {
          this.isLoading = false;
          console.error('❌ Erreur de connexion admin:', error);

          if (error.status === 401 || error.status === 404) {
            this.errorMessage = 'Email ou mot de passe incorrect';
          } else {
            this.errorMessage = 'Erreur de connexion. Veuillez réessayer.';
          }
        }
      });
  }
}

