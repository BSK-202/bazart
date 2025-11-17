import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AdminAuthGuard implements CanActivate {

  constructor(private router: Router) {}

  canActivate(): boolean {
    const adminToken = localStorage.getItem('adminToken');
    const adminData = localStorage.getItem('adminData');

    if (adminToken && adminData) {
      console.log('✅ Guard admin - Accès autorisé');
      return true;
    } else {
      console.log('❌ Guard admin - Accès refusé, redirection vers /connexion');
      this.router.navigate(['/connexion']);
      return false;
    }
  }
}
