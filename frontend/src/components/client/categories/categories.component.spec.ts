import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoriesComponent } from './categories.component';

describe('CategoriesComponent', () => {
  let component: CategoriesComponent;
  let fixture: ComponentFixture<CategoriesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriesComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CategoriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display categories', () => {
    const categoryElements = fixture.nativeElement.querySelectorAll('.category-item');
    expect(categoryElements.length).toBe(component.categories.length);
  });

  it('should display category names', () => {
    const firstCategoryName = fixture.nativeElement.querySelector('.category-name');
    expect(firstCategoryName.textContent).toContain('Tapis & Textiles');
  });
});
