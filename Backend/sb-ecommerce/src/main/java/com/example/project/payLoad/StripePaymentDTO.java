package com.example.project.payLoad;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for creating a Stripe PaymentIntent.
 *
 * Deliberately minimal. This used to carry amount, currency, email, name and a full
 * Address entity, all supplied by the browser, and the amount was passed straight
 * into PaymentIntentCreateParams. The charge is now derived from the caller's
 * server-side cart and the customer details come from the authenticated session, so
 * none of those fields belong in the request any more.
 *
 * addressId is only used to attach a billing address to the Stripe customer, and is
 * validated as belonging to the caller before use.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StripePaymentDTO {
    private Long addressId;
}
