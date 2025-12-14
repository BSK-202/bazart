// auth.interceptor.ts - VERSION CORRIGÉE
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    console.log('🔐 AuthInterceptor appelé pour:', request.url);

    // ✅ STRATÉGIE CORRECTE POUR LES TOKENS :

    // 1. Vérifier si c'est une route admin
    const isAdminRoute = request.url.includes('/api/admin/');

    if (isAdminRoute) {
      console.log('👑 Route admin détectée');

      // Pour les routes admin, utiliser adminToken
      const adminToken = localStorage.getItem('adminToken');

      if (adminToken) {
        console.log('✅ Token admin trouvé, ajout au header');
        request = request.clone({
          setHeaders: {
            Authorization: `Bearer ${adminToken}`
          }
        });
      } else {
        console.warn('⚠️ Route admin mais pas de adminToken trouvé');
      }
    }
    // 2. Pour les routes utilisateur normales
    else if (request.url.includes('/api/')) {
      console.log('👤 Route utilisateur normale');

      // Pour les routes utilisateur, utiliser authToken
      const authToken = localStorage.getItem('authToken');

      if (authToken) {
        console.log('✅ Token utilisateur trouvé, ajout au header');
        request = request.clone({
          setHeaders: {
            Authorization: `Bearer ${authToken}`
          }
        });
      }
    }

    // 3. Pour les routes publiques (sans /api/), ne rien ajouter

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Erreur HTTP dans interceptor:', {
          url: request.url,
          status: error.status,
          message: error.message
        });

        // Gérer les erreurs 403 (Forbidden)
        if (error.status === 403) {
          console.warn('⚠️ Accès interdit (403) pour:', request.url);

          if (isAdminRoute) {
            console.warn('⚠️ Le token admin est peut-être expiré ou invalide');
            // Optionnel: Rediriger vers login admin
            // this.router.navigate(['/admin/login']);
          }
        }

        return throwError(() => error);
      })
    );
  }
}
