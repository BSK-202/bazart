// admin-auth.service.ts - VERSION CORRIGÉE
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AdminAuthService {
  private adminTokenKey = 'adminToken';
  private adminDataKey = 'adminData';

  constructor(private router: Router) {}

  loginAdmin(adminData: any): void {
    localStorage.setItem(this.adminTokenKey, 'admin-authenticated');
    localStorage.setItem(this.adminDataKey, JSON.stringify(adminData));

    // ✅ NETTOYER les données client pour éviter les conflits
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('userData');

    console.log('🔐 Admin connecté:', adminData.email);
  }

  logoutAdmin(): void {
    localStorage.removeItem(this.adminTokenKey);
    localStorage.removeItem(this.adminDataKey);
    this.router.navigate(['/connexion']);
  }

  isAdminLoggedIn(): boolean {
    const token = localStorage.getItem(this.adminTokenKey);
    const data = localStorage.getItem(this.adminDataKey);
    return !!(token && data);
  }

  getAdminData(): any {
    const data = localStorage.getItem(this.adminDataKey);
    return data ? JSON.parse(data) : null;
  }

  getAdminEmail(): string | null {
    const data = this.getAdminData();
    return data ? data.email : null;
  }
}
