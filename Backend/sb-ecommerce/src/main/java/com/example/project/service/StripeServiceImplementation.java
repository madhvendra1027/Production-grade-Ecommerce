package com.example.project.service;

import com.example.project.model.Address;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class StripeServiceImplementation implements StripeService {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init(){
        // Fail at startup rather than at the first checkout attempt. Previously a
        // missing STRIPE_SECRET_KEY just assigned null to Stripe.apiKey and the app
        // booted happily.
        if (!StringUtils.hasText(stripeApiKey)) {
            throw new IllegalStateException(
                    "stripe.secret.key is not configured. Set the STRIPE_SECRET_KEY environment variable.");
        }
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public PaymentIntent createPaymentIntent(long amountInMinorUnits,
                                             String currency,
                                             String customerEmail,
                                             String customerName,
                                             Address address,
                                             String description,
                                             Map<String, String> metadata) throws StripeException {

        if (amountInMinorUnits <= 0) {
            throw new IllegalArgumentException("Charge amount must be greater than zero.");
        }

        Customer customer = findOrCreateCustomer(customerEmail, customerName, address);

        PaymentIntentCreateParams.Builder params =
                PaymentIntentCreateParams.builder()
                        // Amount is computed by the caller from the persisted cart, never
                        // taken from the browser.
                        .setAmount(amountInMinorUnits)
                        .setCurrency(currency)
                        .setCustomer(customer.getId())
                        .setDescription(description)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        );

        if (metadata != null && !metadata.isEmpty()) {
            params.putAllMetadata(metadata);
        }

        return PaymentIntent.create(params.build());
    }

    @Override
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    private Customer findOrCreateCustomer(String email, String name, Address address) throws StripeException {
        // Retrieve and check if customer exists
        CustomerSearchParams searchParams =
                CustomerSearchParams.builder()
                        // Escape quotes: this value is interpolated into Stripe's search
                        // query language, so an address like a'b@example.com would
                        // otherwise break or alter the query.
                        .setQuery("email:'" + escapeForSearch(email) + "'")
                        .build();

        CustomerSearchResult customers = Customer.search(searchParams);

        if (!customers.getData().isEmpty()) {
            return customers.getData().get(0);
        }

        CustomerCreateParams.Builder customerParams = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name);

        // The old version dereferenced the client-supplied address unconditionally and
        // would NPE on a checkout with no address selected.
        if (address != null) {
            customerParams.setAddress(
                    CustomerCreateParams.Address.builder()
                            .setLine1(address.getStreetName())
                            .setCity(address.getCity())
                            .setState(address.getState())
                            .setPostalCode(address.getPincode())
                            .setCountry(address.getCountry())
                            .build()
            );
        }

        return Customer.create(customerParams.build());
    }

    private String escapeForSearch(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
