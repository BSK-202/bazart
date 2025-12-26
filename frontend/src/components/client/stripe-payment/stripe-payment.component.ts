import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  Input,
  Output,
  EventEmitter
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { StripeService } from '../../../services/stripe.service';
import { StripeElements, StripePaymentElement } from '@stripe/stripe-js';

@Component({
  selector: 'app-stripe-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stripe-payment.component.html',
  styleUrls: ['./stripe-payment.component.css']
})
export class StripePaymentComponent implements OnInit, OnDestroy {

  @ViewChild('cardElement') cardElement!: ElementRef;

  @Input() amount: number = 0;
  @Input() clientSecret: string = '';

  @Output() onSuccess = new EventEmitter<any>();
  @Output() onCancel = new EventEmitter<void>();
  @Output() onError = new EventEmitter<string>();

  isProcessing = false;
  isInitializing = true;
  errorMessage = '';

  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;

  constructor(private stripeService: StripeService) {
  }

  async ngOnInit() {
    await this.initializePaymentForm();
  }

  ngOnDestroy() {
    if (this.paymentElement) {
      this.paymentElement.unmount();
    }
  }

  async initializePaymentForm() {
    this.isInitializing = true;
    this.errorMessage = '';

    try {
      if (!this.stripeService.isStripeReady()) {
        await this.stripeService.initializeStripe();
      }

      if (!this.clientSecret) {
        throw new Error('Client Secret manquant');
      }

      this.elements = this.stripeService.createElements(this.clientSecret);

      if (!this.elements) {
        throw new Error('Stripe Elements non initialisés');
      }

      this.paymentElement = this.elements.create('payment', {
        layout: { type: 'tabs' }
      });


      await new Promise(r => setTimeout(r, 100));
      this.paymentElement.mount(this.cardElement.nativeElement);

      this.paymentElement.on('change', (event: any) => {
        this.errorMessage = event.error ? event.error.message : '';
      });

      this.isInitializing = false;

    } catch (error: any) {
      this.errorMessage = error.message;
      this.onError.emit(this.errorMessage);
      this.isInitializing = false;
    }
  }

  async handlePayment() {
    if (this.isProcessing || this.isInitializing) return;

    this.isProcessing = true;
    this.errorMessage = '';

    try {
      if (!this.elements) {
        throw new Error('Éléments Stripe non initialisés');
      }

      if (!this.clientSecret) {
        throw new Error('Client Secret manquant');
      }

      const paymentIntent =
        await this.stripeService.confirmCardPayment(
          this.clientSecret,
          this.elements
        );

      // ✅ Vérification obligatoire
      if (!paymentIntent) {
        throw new Error('Aucune réponse de Stripe');
      }

      if (paymentIntent.status === 'succeeded') {
        this.onSuccess.emit({
          paymentIntentId: paymentIntent.id,
          amount: this.amount
        });
      } else {
        throw new Error(`Statut du paiement: ${paymentIntent.status}`);
      }

    } catch (error: any) {
      this.errorMessage = error.message || 'Erreur lors du paiement';
      this.onError.emit(this.errorMessage);
    } finally {
      this.isProcessing = false;
    }
  }
  cancel(): void {
    console.log(' Paiement annulé');
    this.onCancel.emit();
  }

}
