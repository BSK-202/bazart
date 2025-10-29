// app.config.ts - VERSION CORRIGÉE
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { routes } from './app.routes';

// ✅ CORRECTION: Importer AuthInterceptor (la classe)
import { AuthInterceptor } from '../services/auth.interceptor';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { faHeart as faHeartSolid } from '@fortawesome/free-solid-svg-icons';
import { faHeart as faHeartRegular, faComment } from '@fortawesome/free-regular-svg-icons';

// === POUR FIREBASE ===
import { provideFirebaseApp, initializeApp } from '@angular/fire/app';
import { provideAuth, getAuth } from '@angular/fire/auth';
import { environment } from '../environments/environment';
// ============================

console.log('Firebase config used:', environment.firebase);

library.add(faHeartSolid, faHeartRegular, faComment);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),

    // ✅ CORRECTION: Utiliser AuthInterceptor (la classe)
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },

    // === POUR FIREBASE ===
    provideFirebaseApp(() => initializeApp(environment.firebase)),
    provideAuth(() => getAuth()),
    // ============================
    importProvidersFrom(FontAwesomeModule)
  ]
};
