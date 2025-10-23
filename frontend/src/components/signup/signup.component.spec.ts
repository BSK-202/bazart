import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { SignupComponent } from './signup.component';

describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [SignupComponent, HttpClientTestingModule],
      providers: [
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty values', () => {
    expect(component.nom).toBe('');
    expect(component.prenom).toBe('');
    expect(component.email).toBe('');
    expect(component.tel).toBe('');
    expect(component.pays).toBe('');
    expect(component.ville).toBe('');
    expect(component.photoprofil).toBe('');
    expect(component.password).toBe('');
    expect(component.confirmPassword).toBe('');
    expect(component.error).toBe('');
    expect(component.loading).toBeFalse();
  });

  it('should validate required fields', () => {
    component.nom = 'Test';
    component.prenom = 'User';
    component.email = 'test@example.com';
    // Ne pas remplir pays et ville (champs requis)

    component.onSubmit(new Event('submit'));

    expect(component.error).toBe('Veuillez remplir tous les champs obligatoires');
  });

  it('should validate password mismatch', () => {
    component.nom = 'Test';
    component.prenom = 'User';
    component.email = 'test@example.com';
    component.pays = 'Maroc';
    component.ville = 'Casablanca';
    component.password = 'password123';
    component.confirmPassword = 'different';

    component.onSubmit(new Event('submit'));

    expect(component.error).toBe('Les mots de passe ne correspondent pas');
  });

  it('should validate password length', () => {
    component.nom = 'Test';
    component.prenom = 'User';
    component.email = 'test@example.com';
    component.pays = 'Maroc';
    component.ville = 'Casablanca';
    component.password = '123';
    component.confirmPassword = '123';

    component.onSubmit(new Event('submit'));

    expect(component.error).toBe('Le mot de passe doit contenir au moins 6 caractères');
  });

  it('should validate email format', () => {
    component.nom = 'Test';
    component.prenom = 'User';
    component.email = 'invalid-email';
    component.pays = 'Maroc';
    component.ville = 'Casablanca';
    component.password = 'password123';
    component.confirmPassword = 'password123';

    component.onSubmit(new Event('submit'));

    expect(component.error).toBe('Veuillez entrer une adresse email valide');
  });
});
