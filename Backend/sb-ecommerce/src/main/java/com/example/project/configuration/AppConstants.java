package com.example.project.configuration;

import java.util.Set;

public class AppConstants {
     public static final String PAGE_NUMBER = "0";
     public static final String PAGE_SIZE = "8";
     public static final String SORT_BY_CATEGORY = "categoryID";
     public static final String SORT_BY_PRODUCT = "productId";
     public static final String SORT_ORDER = "asc";
     public static final String SORT_BY_ORDER = "totalAmount";
     public static final String SORT_USERS_BY = "userId";

     /** Status applied to a new order once its payment has been verified. */
     public static final String ORDER_STATUS_ACCEPTED = "Accepted";

     /**
      * Statuses an admin or seller may set. Mirrors ORDER_STATUSES in
      * UpdateOrderForm.jsx. The status update endpoints previously wrote whatever
      * string arrived in the request body straight onto the order.
      */
     public static final Set<String> ALLOWED_ORDER_STATUSES = Set.of(
             "Pending",
             "Processing",
             "Shipped",
             "Delivered",
             "Cancelled",
             "Accepted"
     );

     /** The only payment method with a working backend implementation. */
     public static final String PAYMENT_METHOD_ONLINE = "online";
}
