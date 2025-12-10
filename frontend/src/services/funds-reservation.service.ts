// funds-reservation.service.ts - VERSION CORRIGÉE
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DebitResponse {
  success: boolean;
  message: string;
  newBalance?: number;
  amountDebited?: number;
  clientId?: number;
}

export interface RechargeResponse {
  success: boolean;
  message: string;
  newBalance?: number;
  amountAdded?: number;
  clientId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class FundsReservationService {
  private readonly API_BASE_URL = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  // ✅ UTILISER LES NOUVEAUX ENDPOINTS ADMIN
  debiterEnchere(clientId: number, montant: number, produitId: number): Observable<DebitResponse> {
    return this.http.post<DebitResponse>(
      `${this.API_BASE_URL}/api/wallet/${clientId}/debit`,
      {
        amount: montant,
        description: `DÉBIT enchère produit ${produitId}`
      }
    );
  }

  // ✅ UTILISER LES NOUVEAUX ENDPOINTS ADMIN
  rembourserEnchere(clientId: number, montant: number, produitId: number): Observable<RechargeResponse> {
    return this.http.post<RechargeResponse>(
      `${this.API_BASE_URL}/api/wallet/${clientId}/recharge`,
      {
        amount: montant,
        description: `REMBOURSEMENT enchère produit ${produitId}`
      }
    );
  }
}
