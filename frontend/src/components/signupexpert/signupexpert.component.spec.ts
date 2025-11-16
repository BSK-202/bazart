import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SingupexpertComponent } from './signupexpert.component';

describe('SingupexpertComponent', () => {
  let component: SingupexpertComponent;
  let fixture: ComponentFixture<SingupexpertComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SingupexpertComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SingupexpertComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
