// commentaire.service.ts - VERSION AMÉLIORÉE
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {Observable, of} from 'rxjs';
import { AuthService } from './auth.service';
import {catchError, tap} from 'rxjs/operators';

export interface Commentaire {
  idcommentaire: number;
  contenu: string;
  date: string;
  client: {
    idclient: number;
    nom: string;
    prenom: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class CommentaireService {
  private apiUrl = 'http://localhost:8080/api/commentaires';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // ✅ CORRECTION : Inclure aussi le token JWT
  private getHeaders(): HttpHeaders {
    const currentUserId = this.authService.getCurrentUserId();
    const token = this.authService.getToken();

    console.log('🔍 Headers Commentaire - Client ID:', currentUserId);
    console.log('🔍 Headers Commentaire - Token présent:', !!token);

    let headers = new HttpHeaders();

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    if (currentUserId && currentUserId > 0) {
      headers = headers.set('X-Client-Id', currentUserId.toString());
    }

    console.log('📤 Headers finaux:', headers.keys());
    return headers;
  }


// commentaire.service.ts - CORRECTION
  getCommentairesByProduit(produitId: number): Observable<Commentaire[]> {
    const headers = this.getHeaders(); // ✅ AJOUTER LES HEADERS

    return this.http.get<Commentaire[]>(
      `${this.apiUrl}/produit/${produitId}`,
      { headers }
    ).pipe(
      tap(commentaires => {
        console.log('✅ Commentaires reçus:', commentaires);
      }),
      catchError(error => {
        console.error('❌ Erreur chargement commentaires:', error);
        return of([]);
      })
    );
  }


  addCommentaire(produitId: number, contenu: string): Observable<Commentaire> {
    const headers = this.getHeaders();

    console.log('📤 Envoi commentaire - Headers:', headers);
    console.log('📤 Headers keys:', headers.keys());
    console.log('📤 X-Client-Id header:', headers.get('X-Client-Id'));
    console.log('📤 Authorization header:', headers.get('Authorization'));

    return this.http.post<Commentaire>(
      `${this.apiUrl}/produit/${produitId}`,
      { contenu },
      { headers }
    );
  }

  deleteCommentaire(commentaireId: number): Observable<void> {
    const headers = this.getHeaders();
    return this.http.delete<void>(`${this.apiUrl}/${commentaireId}`, { headers });
  }

  getNombreCommentaires(produitId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/produit/${produitId}/count`);
  }
}
