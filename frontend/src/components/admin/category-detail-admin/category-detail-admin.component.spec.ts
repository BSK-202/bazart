import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryDetailAdminComponent } from './category-detail-admin.component';

describe('CategoryDetailAdminComponent', () => {
  let component: CategoryDetailAdminComponent;
  let fixture: ComponentFixture<CategoryDetailAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryDetailAdminComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CategoryDetailAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
