import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpertsInactifsComponent } from './experts-inactif-admin.component';

describe('ExepertsInactifAdmin', () => {
  let component: ExpertsInactifsComponent;
  let fixture: ComponentFixture<ExpertsInactifsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpertsInactifsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExpertsInactifsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
