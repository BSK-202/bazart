import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AllCategorieAdmin } from './all-categories-admin.component';

describe('AllCategorieAdmin', () => {
  let component: AllCategorieAdmin;
  let fixture: ComponentFixture<AllCategorieAdmin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AllCategorieAdmin]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AllCategorieAdmin);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
