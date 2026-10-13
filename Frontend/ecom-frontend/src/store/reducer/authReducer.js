// A reducer must not import action creators - it was importing
// selectUserCheckoutAddress from ../actions and never using it, which also set up
// a circular import between the store and the actions module.
const initialState = {
      user: null,
      address: [],
      clientSecret: null,
      // Renamed from selectedUserAddress to match the key the reducer actually
      // writes on SELECT_CHECKOUT_ADDRESS and that every component reads.
      selectedUserCheckoutAddress: null
};

export const authReducer = (state = initialState, action) => {
    switch (action.type) {
        case "LOGIN_USER":
            return {
                ...state, user: action.payload
            };

        case "USER_ADDRESS":
            return {
                ...state, address: action.payload
            };

        case "LOG_OUT":
            return {
                ...initialState,
                user: null,
            };

        case "SELECT_CHECKOUT_ADDRESS":
            return {
                ...state, selectedUserCheckoutAddress: action.payload
            };

        case "REMOVE_CHECKOUT_ADDRESS":
            return {
                ...state, selectedUserCheckoutAddress: null
            };
        
        case "CLIENT_SECRET":
            return {
                ...state, clientSecret: action.payload
            };

        case "REMOVE_CLIENT_SECRET_ADDRESS":
            return {
                // Was selectUserCheckoutAddress - a key nothing reads - so clearing
                // the selected address after a successful order had no effect.
                ...state, clientSecret: null, selectedUserCheckoutAddress: null
            };
        default:
            return state;
    }
};
