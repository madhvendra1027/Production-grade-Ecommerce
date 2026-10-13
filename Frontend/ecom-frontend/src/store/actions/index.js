import api from "../../api/api";
import toast from "react-hot-toast";

/**
 * Reads the admin flag from the store rather than taking it as an argument.
 *
 * Several actions refetch a list after mutating it and used to call e.g.
 * dispatch(dashboardProductsAction()) with no arguments at all. isAdmin then came
 * through as undefined, so the refetch always hit the /seller/* endpoint - an
 * admin would delete a product and watch the table reload as empty.
 */
const isAdminUser = (getState) =>
  Boolean(getState()?.auth?.user?.roles?.includes("ROLE_ADMIN"));

const withQuery = (queryString) => (queryString ? `?${queryString}` : "");

export const fetchProducts = (queryString) => async (dispatch) => {
  try {
    dispatch({ type: "IS_FETCHING" });

    const { data } = await api.get(`/public/products?${queryString}`);

    dispatch({
      type: "FETCH_PRODUCTS",
      payload: data.content,
      pageNumber: data.pageNumber,
      pageSize: data.pageSize,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      lastPage: data.lastPage,
    });
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to Fetch",
    });
  }
};

export const fetchCategories = (queryString) => async (dispatch) => {
  try {
    dispatch({ type: "CATEGORY_LOADER" });

    const { data } = await api.get(`/public/categories${withQuery(queryString)}`);

    dispatch({
      type: "FETCH_CATEGORIES",
      payload: data.content,
      pageNumber: data.pageNumber,
      pageSize: data.pageSize,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      lastPage: data.lastPage,
    });
    // Was dispatch({ type: "IS_ERROR" }) on the success path, which left the UI
    // showing an error state after a category fetch that actually worked.
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to Fetch categories",
    });
  }
};

export const addToCart =
  (data, qty = 1, toast) =>
  (dispatch, getState) => {
    //Find the product
    const { products } = getState().products;
    const getProduct = products.find(
      (item) => item.productId === data.productId,
    );

    //Check for stocks
    const isQuantityExist = getProduct.quantity >= qty;

    //If in stock -> add
    if (isQuantityExist) {
      dispatch({ type: "ADD_CART", payload: { ...data, quantity: qty } });
      toast.success(`${data?.productName} added to the cart`);
      localStorage.setItem("cartItems", JSON.stringify(getState().carts.cart));
    } else {
      //error
      toast.error("Out of Stock");
    }
  };

export const increaseCartQuantity =
  (data, toast, currentQuantity, setCurrentQuantity) =>
  (dispatch, getState) => {
    //Find the product
    const { products } = getState().products;
    console.log(data);
    const getProduct = products.find(
      (item) => item.productId === data.productId,
    );

    if (!getProduct) {
      toast.error("Product not found");
      return;
    }

    const isQuantityExist = getProduct.quantity >= currentQuantity + 1;

    if (isQuantityExist) {
      const newQuantity = currentQuantity + 1;
      setCurrentQuantity(newQuantity);

      dispatch({
        type: "ADD_CART",
        // Was newQuantity + 1, double-counting the increment: the local state went
        // to n+1 while the store was told n+2, so the cart showed a phantom extra
        // unit and the displayed total was wrong.
        payload: { ...data, quantity: newQuantity },
      });

      localStorage.setItem("cartItems", JSON.stringify(getState().carts.cart));
    } else {
      toast.error("Quantity reached to limit");
    }
  };

export const decreaseCartQuantity =
  (data, newQuantity) => (dispatch, getState) => {
    dispatch({
      type: "ADD_CART",
      payload: { ...data, quantity: newQuantity },
    });
    localStorage.setItem("cartItems", JSON.stringify(getState().carts.cart));
  };

export const removeFromCart = (data, toast) => (dispatch, getState) => {
  dispatch({ type: "REMOVE_CART", payload: data });
  toast.success(`${data.productName} removed from cart`);
  localStorage.setItem("cartItems", JSON.stringify(getState().carts.cart));
};


export const clearCart = () => (dispatch) => {
    dispatch({ type: "CLEAR_CART" });
    localStorage.removeItem("cartItems");
};


export const authenticateSingInUser =
  (sendData, toast, reset, navigate, setLoader) => async (dispatch) => {
    try {
      setLoader(true);
      const { data } = await api.post("/auth/signin", sendData);
      dispatch({ type: "LOGIN_USER", payload: data });
      localStorage.setItem("auth", JSON.stringify(data));
      reset();
      toast.success("Login Success");
      navigate("/");
    } catch (error) {
      console.log(error);
      toast.error(error?.response?.data?.message || "Internal Server Error");
    } finally {
      setLoader(false);
    }
  };

export const registerNewUser =
  (sendData, toast, reset, navigate, setLoader) => async (dispatch) => {
    try {
      setLoader(true);
      const { data } = await api.post("/auth/signup", sendData);
      reset();
      toast.success(data?.message || "User Registered Successfully");
      navigate("/login");
    } catch (error) {
      console.log(error);
      toast.error(
        error?.response?.data?.message ||
          error?.response?.data?.password ||
          "Internal Server Error",
      );
    } finally {
      setLoader(false);
    }
  };

export const logOutUser = (navigate) => async (dispatch) => {
  // Tell the server to clear the JWT cookie. Without this call the cookie stayed
  // valid until it expired, so "logging out" only cleared local state - going
  // back to the app, or anyone else on that machine, was still authenticated.
  try {
    await api.post("/auth/signout");
  } catch (error) {
    // Clearing local state matters more than the round trip succeeding, so a
    // failure here (offline, already-expired token) must not block logout.
    console.warn("Sign-out request failed; clearing local session anyway.", error);
  }

  dispatch({ type: "LOG_OUT" });
  localStorage.removeItem("auth");
  localStorage.removeItem("cartItems");
  localStorage.removeItem("CHECKOUT_ADDRESS");
  localStorage.removeItem("client-secret");
  navigate("/login");
};

export const addUpdateUserAddress =
  (sendData, toast, addressId, setOpenAddressModal) =>
  async (dispatch, getState) => {
    const { user } = getState().auth;
    dispatch({ type: "BUTTON_LOADER" });
    try {
      if (!addressId) {
        const { data } = await api.post("/addresses", sendData);
      } else {
        await api.put(`/addresses/${addressId}`, sendData);
      }
      toast.success("Address saved successfully");
      dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
      console.log(error);
      toast.error(error?.response?.data?.message || "Internal Server Error");
      dispatch({ type: "IS_ERROR", payload: null });
    } finally {
      setOpenAddressModal(false);
    }
  };

export const clearCheckoutAddress = () => async (dispatch, getState) => {
  return {
    type: "REMOVE_CHECKOUT_ADDRESS",
  };
};

export const getUserAddresses = (queryString) => async (dispatch) => {
  try {
    dispatch({ type: "IS_FETCHING" });
    const { data } = await api.get(`/addresses`);
    dispatch({
      type: "USER_ADDRESS",
      payload: data,
    });
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to fetch addresses",
    });
  }
};

export const selectUserCheckoutAddress = (address) => {
  localStorage.setItem("CHECKOUT_ADDRESS", JSON.stringify(address));
  return {
    type: "SELECT_CHECKOUT_ADDRESS",
    payload: address,
  };
};

export const deleteUserAddress =
  (toast, addressId, setOpenDeleteModal) => async (dispatch, getState) => {
    try {
      dispatch({ type: "BUTTON_LOADER" });
      await api.delete(`/addresses/${addressId}`);
      dispatch({ type: "IS_SUCCESS" });
      dispatch(getUserAddresses());
      toast.success("Address deleted successfully");
    } catch (error) {
      dispatch({
        type: "IS_ERROR",
        payload: error?.response?.data?.message || "Some Error Occured",
      });
    } finally {
      setOpenDeleteModal(false);
    }
  };

export const addPaymentMethod = (method) => {
  return {
    type: "ADD_PAYMENT_METHOD",
    payload: method,
  };
};

export const createUserCart = (sendCartItems) => async (dispatch) => {
  try {
    dispatch({ type: "IS_FETCHING" });
    await api.post("/cart/create", sendCartItems);
    await dispatch(getUserCart());
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to create cart items",
    });
  }
};

export const getUserCart = () => async (dispatch, getState) => {
  try {
    dispatch({ type: "IS_FETCHING" });
    const { data } = await api.get("/carts/users/cart");

    dispatch({
      type: "GET_USER_CART_PRODUCTS",
      payload: data.products,
      totalPrice: data.totalPrice,
      cartId: data.cartId,
    });
    localStorage.setItem("cartItems", JSON.stringify(getState().carts.cart));
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to fetch cart items",
    });
  }
};

export const createStripePaymentSecret =
  (sendData) => async (dispatch, getState) => {
    try {
      dispatch({ type: "IS_FETCHING" });
      const { data } = await api.post("/order/stripe-client-secret", sendData);
      dispatch({ type: "CLIENT_SECRET", payload: data });
      localStorage.setItem("client-secret", JSON.stringify(data));
      dispatch({ type: "IS_SUCCESS" });
    } catch (error) {
      console.log(error);
      toast.error(
        error?.response?.data?.message || "Failed to create client-secret",
      );
    }
  };

export const stripePaymentConfirmation =
  (sendData, setErrorMessage, setLoading, toast) =>
  async (dispatch, getState) => {
    try {
      const response = await api.post(
        "/orders/users/payments/online",
        sendData,
      );
      console.log(response);
      if (response.data) {
        console.log("IN IF");
        localStorage.removeItem("CHECKOUT_ADDRESS");
        localStorage.removeItem("cartItems");
        localStorage.removeItem("client-secret");
        dispatch({ type: "REMOVE_CLIENT_SECRET_ADDRESS" });
        dispatch({ type: "CLEAR_CART" });
        toast.success("Order Accepted");
      } else {
        setErrorMessage("Payment Failed, Please try again.");
      }
    } catch (error) {
      setErrorMessage("Payment Failed, Please try again.");
    }
  };

export const analyticsAction = () => async (dispatch, getState) => {
  try {
    dispatch({ type: "IS_FETCHING" });
    const { data } = await api.get("/admin/app/analytics");
    dispatch({
      type: "FETCH_ANALYTICS",
      payload: data,
    });
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to fetch analytics",
    });
  }
};

export const getOrdersForDashboard = (queryString, isAdmin) => async (dispatch) => {
  try {
    dispatch({ type: "IS_FETCHING" });
    const endpoint = isAdmin ? "/admin/orders" : "/seller/orders";
    const { data } = await api.get(`${endpoint}${withQuery(queryString)}`);

    dispatch({
      type: "GET_ADMIN_ORDERS",
      payload: data.content,
      pageNumber: data.pageNumber,
      pageSize: data.pageSize,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      lastPage: data.lastPage,
    });
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload: error?.response?.data?.message || "Failed to Fetch orders",
    });
  }
};

export const updateOrderStatusFromDashboard =
  (orderId, orderStatus, toast, setLoader, isAdmin) => async (dispatch, getState) => {
    dispatch({ type: "BUTTON_LOADER" });
    try {
      setLoader(true);
      const endpoint = isAdmin ? "/admin/orders/" : "/seller/orders/";
      const { data } = await api.put(`${endpoint}${orderId}/status`, {
        status: orderStatus,
      });
      toast.success(data.message || "Order status updated successfully");
      // Was dispatch(getOrdersForDashboard()) with no arguments, so an admin's
      // refetch silently went to /seller/orders.
      await dispatch(getOrdersForDashboard("", isAdminUser(getState)));
    } catch (error) {
      console.log(error);
      toast.error(error?.response?.data?.message || "Internal Server Error");
    } finally {
      setLoader(false);
    }
  };

export const dashboardProductsAction = (queryString, isAdmin) => async (dispatch) => {
  try {
    dispatch({ type: "IS_FETCHING" });

    const endpoint = isAdmin ? "/admin/products" : "/seller/products";
    const { data } = await api.get(`${endpoint}${withQuery(queryString)}`);

    dispatch({
      type: "FETCH_PRODUCTS",
      payload: data.content,
      pageNumber: data.pageNumber,
      pageSize: data.pageSize,
      totalElements: data.totalElements,
      totalPages: data.totalPages,
      lastPage: data.lastPage,
    });
    dispatch({ type: "IS_SUCCESS" });
  } catch (error) {
    dispatch({
      type: "IS_ERROR",
      payload:
        error?.response?.data?.message || "Failed to fetch dashboard products",
    });
  }
};

export const updateAdminProductFromDashboard =
  (sendData, toast, reset, setLoader, setOpen) => async (dispatch, getState) => {
    try {
      setLoader(true);
      // Was sendData.id. ProductDTO's field is productId, and nothing populates an
      // `id` property, so this PUT went to /admin/products/undefined every time -
      // a 400 from the path variable converter, never an update.
      const productId = sendData.productId ?? sendData.id;
      if (!productId) {
        toast.error("Product id is missing");
        return;
      }

      await api.put(`/admin/products/${productId}`, sendData);
      toast.success("Product update successful");
      reset();
      setOpen(false);
      await dispatch(dashboardProductsAction("", isAdminUser(getState)));
    } catch (error) {
      toast.error(
        error?.response?.data?.message || "Product update failed",
      );
    } finally {
      setLoader(false);
    }
  };

export const addNewProductFromDashboard =
  (sendData, toast, reset, setLoader, setOpen) =>
  async (dispatch, getState) => {
    try {
      setLoader(true);
      await api.post(
        `/admin/categories/${sendData.categoryId}/product`,
        sendData,
      );
      toast.success("Product created successfully");
      reset();
      setOpen(false);
      await dispatch(dashboardProductsAction("", isAdminUser(getState)));
    } catch (error) {
      console.error(error);
      toast.error(
        error?.response?.data?.message || "Product creation failed",
      );
    } finally {
      setLoader(false);
    }
  };

export const addNewCategoryFromDashboard =
  (sendData, toast, reset, setLoader, setOpen) =>
  async (dispatch) => {
    try {
      setLoader(true);
      // Creating a category is POST /admin/categories with just a categoryName.
      //
      // This action was a copy of addNewProductFromDashboard: it posted to
      // /admin/categories/{categoryId}/product, so clicking "Add Category"
      // attempted to create a *product*. The later edit changed the tail to
      // /categories, which matches no mapping at all. It also interpolated
      // sendData.categoryId - an id that cannot exist yet when creating something.
      await api.post(`/admin/categories`, { categoryName: sendData.categoryName });
      toast.success("Category created successfully");
      reset();
      setOpen(false);
      // Refetch categories, not products.
      await dispatch(fetchCategories());
    } catch (error) {
      console.error(error);
      toast.error(
        error?.response?.data?.message ||
          error?.response?.data?.categoryName ||
          "Category creation failed",
      );
    } finally {
      setLoader(false);
    }
  };

export const updateCategoryFromDashboard =
  (sendData, toast, reset, setLoader, setOpen) =>
  async (dispatch) => {
    try {
      setLoader(true);
      const categoryId = sendData.categoryId ?? sendData.id;
      if (!categoryId) {
        toast.error("Category id is missing");
        return;
      }

      // Previously the update branch of AddCategoryForm called
      // updateAdminProductFromDashboard, i.e. editing a category hit the product
      // endpoint.
      await api.put(`/admin/categories/${categoryId}`, {
        categoryName: sendData.categoryName,
      });
      toast.success("Category updated successfully");
      reset();
      setOpen(false);
      await dispatch(fetchCategories());
    } catch (error) {
      console.error(error);
      toast.error(
        error?.response?.data?.message || "Category update failed",
      );
    } finally {
      setLoader(false);
    }
  };

export const deleteProduct =
  (setLoader, productId, toast, setOpenDeleteModal) =>
  async (dispatch, getState) => {
    try {
      setLoader(true);
      await api.delete(`/admin/products/${productId}`);
      await dispatch(dashboardProductsAction("", isAdminUser(getState)));
      toast.success("Product deleted successfully");
      setOpenDeleteModal(false);
    } catch (error) {
      toast.error(error?.response?.data?.message || "Some Error Occured");
    } finally {
      // Was only cleared on the success path, so a failed delete left the modal
      // spinner running forever.
      setLoader(false);
    }
  };

export const updateProductImageFromDashboard =
  (formData, productId, toast, setLoader, setOpen) => async (dispatch, getState) => {
    try {
      setLoader(true);
      await api.put(`/admin/products/${productId}/image`, formData);
      toast.success("Image upload successful");
      setOpen(false);
      await dispatch(dashboardProductsAction("", isAdminUser(getState)));
    } catch (error) {
      toast.error(
        error?.response?.data?.message || "Product Image upload failed",
      );
    } finally {
      setLoader(false);
    }
  };
