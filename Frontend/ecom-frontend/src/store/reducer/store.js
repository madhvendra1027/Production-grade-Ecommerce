import { configureStore } from '@reduxjs/toolkit';
import { productReducer } from './ProductReducer';
import { errorReducer } from './errorReducer';
import { cartReducer } from './cartReducer';
import { authReducer } from './authReducer';
import { paymentMethodReducer } from './paymentMethodReducer';
import { adminReducer } from './adminReducer';
import { orderReducer } from './orderReducer';

const user = localStorage.getItem("auth")
      ? JSON.parse(localStorage.getItem("auth"))
      : null;

const cartItems = localStorage.getItem("cartItems")
      ? JSON.parse(localStorage.getItem("cartItems"))
      : [];

const selectedUserCheckoutAddress = localStorage.getItem("CHECKOUT_ADDRESS")
      ? JSON.parse(localStorage.getItem("CHECKOUT_ADDRESS"))
      : null;

// The key here must match what components read: every consumer uses
// state.auth.selectedUserCheckoutAddress, but this was seeded as
// `selectUserCheckoutAddress` (no "ed"), so a checkout address saved to
// localStorage was never restored after a page refresh. It also defaulted to []
// rather than null, so `selectedAddress?.addressId` on an empty array read as
// undefined instead of failing a null check.
const intialState = {
       auth: { user: user, selectedUserCheckoutAddress },
       carts: { cart: cartItems },
};

export const store = configureStore({
       
       reducer: {
              products: productReducer,
              errors: errorReducer,
              carts: cartReducer,
              auth: authReducer,
              payment: paymentMethodReducer,
              admin: adminReducer,
              order: orderReducer,
       },
       preloadedState: intialState,
});

export default store;