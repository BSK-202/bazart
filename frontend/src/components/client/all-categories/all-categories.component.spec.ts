import { ComponentFixture, TestBed } from '@angular/core/testing';

// @ts-ignore
import { AllCategoriesComponentComponent } from './all-categories.component';

describe('AllCategoriesComponentComponent', () => {
  let component: AllCategoriesComponentComponent;
  let fixture: ComponentFixture<AllCategoriesComponentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AllCategoriesComponentComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AllCategoriesComponentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
