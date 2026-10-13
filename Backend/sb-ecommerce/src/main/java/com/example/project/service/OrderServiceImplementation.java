package com.example.project.service;

import com.example.project.configuration.AppConstants;
import com.example.project.globalExceptionHandler.APIexception;
import com.example.project.globalExceptionHandler.ResourceNotFoundException;
import com.example.project.model.*;
import com.example.project.payLoad.OrderDTO;
import com.example.project.payLoad.OrderItemsDTO;
import com.example.project.payLoad.OrderResponse;
import com.example.project.repositories.*;
import com.example.project.util.AuthUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImplementation implements OrderService{

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImplementation.class);

    private static final String STRIPE_STATUS_SUCCEEDED = "succeeded";

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    /** Currency is a server-side setting; it was previously supplied by the browser. */
    @Value("${app.payment.currency:usd}")
    private String paymentCurrency;

    // ------------------------------------------------------------------
    // Payment intent creation
    // ------------------------------------------------------------------

    @Override
    public String createStripeClientSecret(Long addressId) {
        User user = authUtil.loggedInUser();
        Cart cart = requireCart(user.getEmail());

        long amountInMinorUnits = cartTotalInMinorUnits(cart);
        if (amountInMinorUnits <= 0) {
            throw new APIexception("Cannot start a payment for an empty cart.");
        }

        // addressId is optional here - it only decorates the Stripe customer record -
        // but if one is supplied it must belong to the caller.
        Address address = addressId == null ? null : requireOwnedAddress(addressId, user);

        // Recorded on the intent so placeOrder can confirm the charge it is being
        // handed was created for this user.
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", String.valueOf(user.getUserId()));
        metadata.put("cartId", String.valueOf(cart.getCartId()));

        try {
            PaymentIntent intent = stripeService.createPaymentIntent(
                    amountInMinorUnits,
                    paymentCurrency,
                    user.getEmail(),
                    user.getUserName(),
                    address,
                    "Order for " + user.getEmail(),
                    metadata);
            return intent.getClientSecret();
        } catch (StripeException e) {
            logger.error("Failed to create Stripe PaymentIntent for user {} : {}",
                    user.getUserId(), e.getMessage());
            throw new APIexception("Could not start the payment. Please try again.");
        }
    }

    // ------------------------------------------------------------------
    // Order placement
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public OrderDTO placeOrder(Long addressId, String paymentMethod, String pgName,
                               String pgPaymentId, String pgResponseMessage) {

        // Identity comes from the security context, not from a controller argument.
        User user = authUtil.loggedInUser();
        String emailId = user.getEmail();

        Cart cart = requireCart(emailId);

        List<CartItems> cartItems = cartItemRepository.findAllByCartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            // Was a null check on cart.getCartItems(), which is never null for a
            // managed entity, so an empty cart sailed through and produced a
            // zero-item order.
            throw new APIexception("Cart is empty");
        }

        Address address = requireOwnedAddress(addressId, user);

        // Authoritative total, recomputed from the cart rows.
        long expectedAmount = cartTotalInMinorUnits(cart);
        double orderTotal = expectedAmount / 100.0;
        if (expectedAmount <= 0) {
            throw new APIexception("Cart total must be greater than zero.");
        }

        PaymentIntent intent = verifyPayment(paymentMethod, pgPaymentId, user, expectedAmount);

        // Reserve stock before writing the order. If this throws, the whole
        // transaction rolls back and no order is created.
        reserveStock(cartItems, pgPaymentId);

        Payment payment = new Payment(
                paymentMethod,
                pgPaymentId,
                // Status and message now come from Stripe, not from the request body.
                intent.getStatus(),
                StringUtils.hasText(pgResponseMessage) ? pgResponseMessage : "Verified with Stripe",
                pgName);

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(orderTotal);
        // Only reached once the charge has been confirmed as succeeded. This used to
        // be set unconditionally, before any verification.
        order.setOrderStatus(AppConstants.ORDER_STATUS_ACCEPTED);
        order.setAddress(address);

        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        List<OrderItems> orderItems = new ArrayList<>();
        for (CartItems cartItem : cartItems) {
            OrderItems orderItem = new OrderItems();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }
        orderItems = orderItemsRepository.saveAll(orderItems);

        // Empty the cart in one statement. The old code called
        // cartService.deleteProductFromCart inside a forEach over the very collection
        // being modified, and each call recomputed the cart total.
        cartItemRepository.deleteAllByCartId(cart.getCartId());
        cart.setTotalPrice(0.00);
        cartRepository.save(cart);

        return toOrderDTO(savedOrder, orderItems, address.getId());
    }

    /**
     * Confirms with Stripe that the charge really happened, for the right amount, for
     * this user, and has not already been used.
     *
     * Everything here used to be taken on trust from the request body: pgPaymentId,
     * pgStatus and pgResponseMessage were written to the Payment row as-is, so any
     * authenticated user could POST a made-up payment id with
     * pgStatus:"succeeded" and receive a confirmed order without paying.
     */
    private PaymentIntent verifyPayment(String paymentMethod, String pgPaymentId,
                                        User user, long expectedAmount) {

        if (!AppConstants.PAYMENT_METHOD_ONLINE.equalsIgnoreCase(paymentMethod)) {
            // PayPal and PhonePe are unimplemented placeholders in the UI. Refusing
            // them is safer than accepting an order no gateway ever verified.
            throw new APIexception("Unsupported payment method : " + paymentMethod);
        }

        if (!StringUtils.hasText(pgPaymentId)) {
            throw new APIexception("Missing payment reference.");
        }

        if (paymentRepository.existsByPgPaymentId(pgPaymentId)) {
            throw new APIexception("This payment has already been used for an order.");
        }

        PaymentIntent intent;
        try {
            intent = stripeService.retrievePaymentIntent(pgPaymentId);
        } catch (StripeException e) {
            logger.warn("Stripe lookup failed for intent {} : {}", pgPaymentId, e.getMessage());
            throw new APIexception("Payment could not be verified.");
        }

        if (intent == null || !STRIPE_STATUS_SUCCEEDED.equalsIgnoreCase(intent.getStatus())) {
            throw new APIexception("Payment has not completed successfully.");
        }

        if (intent.getAmount() == null || intent.getAmount() != expectedAmount) {
            // Catches both a tampered amount and a cart that changed after the intent
            // was created.
            logger.warn("Payment amount mismatch for intent {} : charged {}, cart {}",
                    pgPaymentId, intent.getAmount(), expectedAmount);
            throw new APIexception("Payment amount does not match the cart total. "
                    + "Please refresh your cart and start the payment again.");
        }

        String intentUserId = intent.getMetadata() == null
                ? null
                : intent.getMetadata().get("userId");

        if (!String.valueOf(user.getUserId()).equals(intentUserId)) {
            // Stops one user presenting another user's successful payment.
            logger.warn("Intent {} belongs to user {} but was presented by user {}",
                    pgPaymentId, intentUserId, user.getUserId());
            throw new AccessDeniedException("This payment does not belong to the current user.");
        }

        return intent;
    }

    /**
     * Decrements stock under a row lock, re-checking availability inside the lock.
     *
     * The old loop read product.getQuantity() from an unlocked entity and wrote back
     * quantity-n, so two concurrent checkouts could each sell the last unit.
     */
    private void reserveStock(List<CartItems> cartItems, String pgPaymentId) {
        for (CartItems item : cartItems) {
            Long productId = item.getProduct().getProductId();

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            if (product.getQuantity() < item.getQuantity()) {
                // The payment has already succeeded at this point - the browser
                // confirms with Stripe before calling this endpoint - so this order
                // needs a refund. Logged loudly because there is no automated refund
                // path yet; see the note in the summary.
                logger.error("PAID BUT UNFULFILLABLE: intent {} succeeded but product {} has "
                                + "only {} of {} units required. A refund is needed.",
                        pgPaymentId, productId, product.getQuantity(), item.getQuantity());
                throw new APIexception("Only " + product.getQuantity() + " unit(s) of "
                        + product.getProductName() + " remain. Your payment will be refunded.");
            }

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageDetails = pageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Order> pageOrders = orderRepository.findAll(pageDetails);
        return toOrderResponse(pageOrders);
    }

    @Override
    public OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageDetails = pageable(pageNumber, pageSize, sortBy, sortOrder);
        User seller = authUtil.loggedInUser();

        // Filtering now happens in the query, so the page metadata describes the
        // seller's orders rather than all orders.
        Page<Order> pageOrders = orderRepository.findOrdersBySellerId(seller.getUserId(), pageDetails);
        return toOrderResponse(pageOrders);
    }

    // ------------------------------------------------------------------
    // Status updates
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public OrderDTO updateOrderAsAdmin(Long orderId, String status) {
        return applyStatus(requireOrder(orderId), status);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderAsSeller(Long orderId, String status) {
        User seller = authUtil.loggedInUser();

        if (!orderItemsRepository.existsByOrderIdAndSellerId(orderId, seller.getUserId())) {
            // Previously absent: the seller endpoint called the same updateOrder as
            // the admin one, so any seller could set the status of any order.
            throw new AccessDeniedException("This order does not contain any of your products.");
        }

        return applyStatus(requireOrder(orderId), status);
    }

    private OrderDTO applyStatus(Order order, String status) {
        if (!StringUtils.hasText(status) || !AppConstants.ALLOWED_ORDER_STATUSES.contains(status)) {
            throw new APIexception("Invalid order status : " + status
                    + ". Allowed values are " + AppConstants.ALLOWED_ORDER_STATUSES + ".");
        }

        order.setOrderStatus(status);
        Order saved = orderRepository.save(order);

        Long addressId = saved.getAddress() == null ? null : saved.getAddress().getId();
        return toOrderDTO(saved, saved.getOrderItems(), addressId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
    }

    private Cart requireCart(String emailId) {
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }
        return cart;
    }

    /**
     * Loads an address and asserts the caller owns it. placeOrder previously accepted
     * any addressId, so an order could be shipped to a stranger's address - and the
     * address id was also a window into other users' records.
     */
    private Address requireOwnedAddress(Long addressId, User user) {
        if (addressId == null) {
            throw new APIexception("An address must be selected.");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        if (address.getUser() == null || !address.getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("This address does not belong to the current user.");
        }

        return address;
    }

    /**
     * Cart total in the currency's smallest unit.
     *
     * Note: totals are stored as double, so this rounds rather than truncates. Money
     * really ought to be BigDecimal throughout; see the summary.
     */
    private long cartTotalInMinorUnits(Cart cart) {
        Double total = cartItemRepository.sumCartTotal(cart.getCartId());
        return Math.round((total == null ? 0.0 : total) * 100.0);
    }

    private Pageable pageable(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNumber, pageSize, sortByAndOrder);
    }

    private OrderResponse toOrderResponse(Page<Order> pageOrders) {
        List<OrderDTO> orderDTOs = pageOrders.getContent().stream()
                .map(order -> toOrderDTO(order, order.getOrderItems(),
                        order.getAddress() == null ? null : order.getAddress().getId()))
                .toList();

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setContent(orderDTOs);
        orderResponse.setPageNumber(pageOrders.getNumber());
        orderResponse.setPageSize(pageOrders.getSize());
        orderResponse.setTotalElements(pageOrders.getTotalElements());
        orderResponse.setTotalPages(pageOrders.getTotalPages());
        orderResponse.setLastPage(pageOrders.isLast());
        return orderResponse;
    }

    /**
     * ModelMapper does not reliably populate OrderDTO.orderItems or addressId, and the
     * old code called orderDTO.getOrderItems().add(...) on a list that could be null.
     * Both are set explicitly here.
     */
    private OrderDTO toOrderDTO(Order order, List<OrderItems> orderItems, Long addressId) {
        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

        List<OrderItemsDTO> itemDTOs = new ArrayList<>();
        if (orderItems != null) {
            for (OrderItems item : orderItems) {
                itemDTOs.add(modelMapper.map(item, OrderItemsDTO.class));
            }
        }

        orderDTO.setOrderItems(itemDTOs);
        orderDTO.setAddressId(addressId);
        return orderDTO;
    }
}
