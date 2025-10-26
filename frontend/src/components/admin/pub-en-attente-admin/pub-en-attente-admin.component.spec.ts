import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PubEnAttente } from './pub-en-attente-admin.component';

describe('PubEnAttente', () => {
  let component: PubEnAttente;
  let fixture: ComponentFixture<PubEnAttente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PubEnAttente]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PubEnAttente);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
