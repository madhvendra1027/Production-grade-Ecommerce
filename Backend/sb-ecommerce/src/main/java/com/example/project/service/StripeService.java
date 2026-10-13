package com.example.project.service;

import com.example.project.model.Address;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import java.util.Map;

public interface StripeService {

    /**
     * Creates a PaymentIntent for an amount the caller has already computed from
     * server-side state.
     *
     * @param amountInMinorUnits charge amount in the currency's smallest unit (cents)
     * @param metadata           attached to the intent so the amount can later be tied
     *                           back to the user and cart it was created for
     */
    PaymentIntent createPaymentIntent(long amountInMinorUnits,
                                      String currency,
                                      String customerEmail,
                                      String customerName,
                                      Address address,
                                      String description,
                                      Map<String, String> metadata) throws StripeException;

    /** Fetches the authoritative state of an intent from Stripe. */
    PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException;
}
