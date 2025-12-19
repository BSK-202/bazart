import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

interface User {
  name: string;
  email: string;
  id?: number;
  photoprofil?: string;
}

interface UserState {
  isAuthenticated: boolean;
  user: User | null;
  userProfileImage: string;
  showProfileImage: boolean;
  isExpert: boolean;
  isExpertActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UserStateService {
  private initialState: UserState = {
    isAuthenticated: false,
    user: null,
    userProfileImage: '',
    showProfileImage: false,
    isExpert: false,
    isExpertActive: false
  };

  private stateSubject = new BehaviorSubject<UserState>(this.initialState);
  public state$ = this.stateSubject.asObservable();

  // Mettre à jour l'état
  updateState(newState: Partial<UserState>): void {
    const currentState = this.stateSubject.value;
    const updatedState = { ...currentState, ...newState };
    this.stateSubject.next(updatedState);
    console.log('🔄 User state updated:', updatedState);
  }

  // Réinitialiser l'état
  resetState(): void {
    this.stateSubject.next(this.initialState);
    console.log('🔄 User state reset');
  }

  // Getters pour accéder directement aux valeurs
  getCurrentState(): UserState {
    return this.stateSubject.value;
  }

  get isAuthenticated(): boolean {
    return this.stateSubject.value.isAuthenticated;
  }

  getUser(): User | null {
    return this.stateSubject.value.user;
  }
}
