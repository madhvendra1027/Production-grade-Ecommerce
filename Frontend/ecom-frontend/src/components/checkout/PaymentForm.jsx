import { Skeleton } from "@mui/material";
import {
  PaymentElement,
  useElements,
  useStripe,
} from "@stripe/react-stripe-js";
import { useState } from "react";
import { useDispatch } from "react-redux";
import { clearCart } from "../../store/actions";

const PaymentForm = ({ clientSecret, totalPrice }) => {
  const dispatch = useDispatch();
  const stripe = useStripe();
  const elements = useElements();
  const [errorMessage, setErrorMessage] = useState("");
  const handleSubmit = async (e) => {
    e.preventDefault();
    console.log("handleSubmit fired. stripe:", stripe, "elements:", elements);
    if (!stripe || !elements) {
      return;
    }

    const { error: submitError } = await elements.submit();
    const { error } = await stripe.confirmPayment({
      elements,
      clientSecret,
      confirmParams: {
        return_url: `${import.meta.env.VITE_FRONTEND_URL}/order-confirm`,
      },
    });

    if (error) {
      setErrorMessage(error.message);
      return false;
    }
  };

  const paymentElementOptions = {
    layout: "tabs",
  };

  const isLoading = !clientSecret || !stripe || !elements;
  return (
    <form onSubmit={handleSubmit} className="max-w-lg mx-auto p-4">
      <h2 className="text-xl font-semibold mb-4">Payment Information</h2>
      {isLoading ? (
        <Skeleton />
      ) : (
        <>
          {clientSecret && <PaymentElement />}
          {errorMessage && (
            <div className="text-red-500 mt-2">{errorMessage}</div>
          )}

          <button
            className="text-white w-full px-5 py-[10px] bg-black mt-2 rounded-md font-bold disabled:opacity-50 disabled:animate-pulse"
            disabled={!stripe || isLoading}
          >
            {!isLoading
              ? `Pay $${Number(totalPrice.toFixed(2))}`
              : "Processing"}
          </button>
        </>
      )}
    </form>
  );
};

export default PaymentForm;
