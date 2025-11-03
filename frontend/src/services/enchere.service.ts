// enchere.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Enchere {
  showDefaultAvatar: any;
  idEnchere: number;
  encherisseurId: number;
  encherisseurNom: string;
  encherisseurPrenom: string;
  produitId: number;
  produitNom: string;
  montant: number;
  dateEnchere: string;
  isLeading: boolean;
  // Ajouter les propriétés calculées pour le template
  bidder?: string;
  amount?: number;
  time?: string;
}

export interface EnchereActuelle {
  montantActuel: number;
}

@Injectable({
  providedIn: 'root'
})
export class EnchereService {
  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // Placer une enchère
  placerEnchere(produitId: number, clientId: number, montant: number): Observable<Enchere> {
    return this.http.post<Enchere>(
      `${this.API_BASE_URL}/api/encheres/produit/${produitId}/client/${clientId}`,
      { montant }
    );
  }

  // Obtenir l'historique des enchères
  getHistoriqueEncheres(produitId: number): Observable<Enchere[]> {
    return this.http.get<Enchere[]>(
      `${this.API_BASE_URL}/api/encheres/produit/${produitId}/historique`
    );
  }

  // Obtenir l'enchère actuelle
  getEnchereActuelle(produitId: number): Observable<EnchereActuelle> {
    return this.http.get<EnchereActuelle>(
      `${this.API_BASE_URL}/api/encheres/produit/${produitId}/montant-actuel`
    );
  }

  // Obtenir le nombre d'enchères
  getNombreEncheres(produitId: number): Observable<{count: number}> {
    return this.http.get<{count: number}>(
      `${this.API_BASE_URL}/api/encheres/produit/${produitId}/count`
    );
  }


}
