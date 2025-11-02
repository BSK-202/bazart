// auth.interceptor.ts - VERSION AMÉLIORÉE
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

  // auth.interceptor.ts - VERSION CORRIGÉE
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    console.log('🔐 AuthInterceptor appelé pour:', request.url);

    // ✅ NE PAS ajouter le token JWT pour les routes admin
    if (request.url.includes('/api/admins/')) {
      console.log('🔐 Route admin - pas de token JWT');
      return next.handle(request);
    }

    const token = this.authService.getToken();

    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request);
  }
}
