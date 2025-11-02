// wallet.service.ts - VERSION CORRIGÉE
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WalletBalanceResponse {
  success: boolean;
  message: string;
  balance: number;
  userId: number;
}

export interface RechargeResponse {
  success: boolean;
  message: string;
  newBalance: number;
  amountAdded: number;
  userId: number;
}

export interface HistoryItem {
  id: number;
  operationType: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string;
  reference: string;
  operationDate: string;
}

export interface HistoryResponse {
  success: boolean;
  message: string;
  history: HistoryItem[];
  count: number;
  userId: number;
  operationType?: string;
}

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private apiUrl = 'http://localhost:8080/api/wallet';

  constructor(private http: HttpClient) {}
// Dans wallet.service.ts - vérification
  private getAuthHeaders(): HttpHeaders {
    // ✅ Utiliser la même clé que auth.service
    const token = localStorage.getItem('authToken');

    console.log('🔐 WalletService - Token:', token);
    console.log('🔐 WalletService - Toutes les clés localStorage:');
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      console.log(`   - ${key}: ${localStorage.getItem(key!)}`);
    }

    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    return headers;
  }

  getBalance(): Observable<WalletBalanceResponse> {
    const headers = this.getAuthHeaders();
    console.log('📊 Headers pour balance:', headers);
    return this.http.get<WalletBalanceResponse>(`${this.apiUrl}/balance`, { headers });
  }

  recharge(amount: number): Observable<RechargeResponse> {
    const headers = this.getAuthHeaders();
    return this.http.post<RechargeResponse>(
      `${this.apiUrl}/recharge`,
      { amount },
      { headers }
    );
  }

  getHistory(): Observable<HistoryResponse> {
    const headers = this.getAuthHeaders();
    return this.http.get<HistoryResponse>(`${this.apiUrl}/history`, { headers });
  }

  getHistoryByType(operationType: string): Observable<HistoryResponse> {
    const headers = this.getAuthHeaders();
    return this.http.get<HistoryResponse>(`${this.apiUrl}/history/${operationType}`, { headers });
  }

  debit(amount: number, description: string): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.post(
      `${this.apiUrl}/debit`,
      { amount, description },
      { headers }
    );
  }
}
