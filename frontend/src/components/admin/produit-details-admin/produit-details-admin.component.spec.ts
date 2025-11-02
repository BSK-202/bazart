import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProduitDetailsAdminComponent } from './produit-details-admin.component';

describe('ProduitDetailsAdminComponent', () => {
  let component: ProduitDetailsAdminComponent;
  let fixture: ComponentFixture<ProduitDetailsAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProduitDetailsAdminComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProduitDetailsAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
