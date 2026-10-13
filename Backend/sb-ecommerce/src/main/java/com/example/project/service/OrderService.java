package com.example.project.service;

import com.example.project.payLoad.OrderDTO;
import com.example.project.payLoad.OrderResponse;

public interface OrderService {

    /**
     * Places an order for the authenticated user's cart.
     *
     * emailId and pgStatus are no longer parameters: the email comes from the security
     * context (it used to be whatever the controller passed) and the payment status is
     * read back from Stripe instead of being asserted by the client.
     */
    OrderDTO placeOrder(Long addressId, String paymentMethod, String pgName,
                        String pgPaymentId, String pgResponseMessage);

    /**
     * Creates a Stripe PaymentIntent for the authenticated user's current cart and
     * returns its client secret. The amount is computed here, server-side.
     */
    String createStripeClientSecret(Long addressId);

    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    /** Admin may change the status of any order. */
    OrderDTO updateOrderAsAdmin(Long orderId, String status);

    /** Seller may only change the status of orders containing their own products. */
    OrderDTO updateOrderAsSeller(Long orderId, String status);
}
