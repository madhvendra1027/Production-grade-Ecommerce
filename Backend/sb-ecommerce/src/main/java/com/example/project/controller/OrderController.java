package com.example.project.controller;

import com.example.project.configuration.AppConstants;
import com.example.project.payLoad.*;
import com.example.project.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // AuthUtil and StripeService are no longer injected here. The controller no
    // longer decides who the caller is or what they are charged; OrderService derives
    // both from the security context and the persisted cart.

    @PostMapping("/orders/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod,
                                                  @RequestBody OrderRequestDTO orderRequestDTO){
         OrderDTO order = orderService.placeOrder(
                 orderRequestDTO.getAddressId(),
                 paymentMethod,
                 orderRequestDTO.getPgName(),
                 orderRequestDTO.getPgPaymentId(),
                 orderRequestDTO.getPgResponseMessage()
         );
         return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    /**
     * Returns a Stripe client secret for the caller's current cart.
     *
     * The request body no longer carries an amount. It used to, and that amount went
     * straight to Stripe, so the charge was whatever the browser said it was.
     */
    @PostMapping("/order/stripe-client-secret")
    public ResponseEntity<String> createStripeClientSecret(@RequestBody StripePaymentDTO stripePaymentDTO) {
        String clientSecret = orderService.createStripeClientSecret(stripePaymentDTO.getAddressId());
        return new ResponseEntity<>(clientSecret, HttpStatus.CREATED);
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<OrderResponse> getAllOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY_ORDER, required = false)String sort_by,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order
    )
     {
         OrderResponse orderResponse = orderService.getAllOrders(pageNumber, pageSize, sort_by, sort_order);
         return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @GetMapping("/seller/orders")
    public ResponseEntity<OrderResponse> getAllSellerOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_BY_ORDER, required = false)String sort_by,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false)String sort_order
    )
    {
        OrderResponse orderResponse = orderService.getAllSellerOrders(pageNumber, pageSize, sort_by, sort_order);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PutMapping("/admin/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId,
                                                      @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO) {
        OrderDTO order = orderService.updateOrderAsAdmin(orderId, orderStatusUpdateDTO.getStatus());
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    /**
     * Same shape as the admin endpoint but routed to updateOrderAsSeller, which
     * checks the order actually contains one of the seller's products. Both used to
     * call the identical unchecked method.
     */
    @PutMapping("/seller/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatusSeller(@PathVariable Long orderId,
                                                            @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO) {
        OrderDTO order = orderService.updateOrderAsSeller(orderId, orderStatusUpdateDTO.getStatus());
        return new ResponseEntity<>(order, HttpStatus.OK);
    }
}
