import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DomaineAdmin } from './domaine-admin.component';

describe('DomaineAdmin', () => {
  let component: DomaineAdmin;
  let fixture: ComponentFixture<DomaineAdmin>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DomaineAdmin]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DomaineAdmin);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
