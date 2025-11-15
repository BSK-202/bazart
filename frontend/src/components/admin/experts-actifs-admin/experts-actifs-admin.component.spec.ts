import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpertsActifsAdmin } from './experts-actifs-admin.component';

describe('ExpertsActifsAdmin', () => {
  let component: ExpertsActifsAdmin;
  let fixture: ComponentFixture<ExpertsActifsAdmin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpertsActifsAdmin]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExpertsActifsAdmin);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
