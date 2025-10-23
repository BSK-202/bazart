// interaction.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {environment} from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class InteractionService {

  private apiUrl = `${environment.apiUrl}/interactions`;  // ✅ Utilise l'URL de l'environment

  constructor(private http: HttpClient) {}

  // ✅ Récupérer dynamiquement l'ID de l'utilisateur connecté depuis localStorage
  private get currentClientId(): number {
    const user = localStorage.getItem('userData'); // ou 'currentUser' selon ton app
    if (user) {
      try {
        const parsedUser = JSON.parse(user);
        return parsedUser.idclient || 0; // adapte selon ton modèle
      } catch (error) {
        console.error('Erreur de parsing du localStorage:', error);
        return 0;
      }
    }
    return 0;
  }

  toggleLike(produitId: number): Observable<any> {
    console.log('🔄 Envoi like pour produit:', produitId);

    const headers = new HttpHeaders({
      'X-Client-Id': this.currentClientId.toString()
    });

    return this.http.post(`${this.apiUrl}/toggle/${produitId}`, {}, { headers }).pipe(
      tap(response => {
        console.log('✅ Réponse like reçue:', response);
      })
    );
  }

  checkLike(produitId: number): Observable<boolean> {
    const headers = new HttpHeaders({
      'X-Client-Id': this.currentClientId.toString()
    });

    return this.http.get<boolean>(`${this.apiUrl}/check/${produitId}`, { headers });
  }

  getLikeCount(produitId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count/${produitId}`);
  }
}
