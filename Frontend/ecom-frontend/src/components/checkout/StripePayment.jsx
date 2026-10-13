import { loadStripe } from '@stripe/stripe-js';
import { useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux';
import PaymentForm from './PaymentForm';
import { createStripePaymentSecret } from '../../store/actions';
import { Elements } from '@stripe/react-stripe-js';
import { Skeleton } from '@mui/material';

const stripePromise = loadStripe( import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY );

const StripePayment = () => {
  const dispatch = useDispatch();
  const { clientSecret } = useSelector((state) => state.auth);
  const { totalPrice } = useSelector((state) => state.carts);
  const { isLoading } = useSelector((state) => state.errors);
  const { selectedUserCheckoutAddress } = useSelector((state) => state.auth);

  const addressId = selectedUserCheckoutAddress?.addressId;

  useEffect(() => {
    if (!addressId) return;

    // The request body is now just the address id.
    //
    // It used to carry amount: Number(totalPrice) * 100 along with the currency,
    // email and name, and the backend passed that amount straight to Stripe - so
    // the charge was whatever this component said it was. The server now computes
    // the amount from the persisted cart and verifies it again when the order is
    // placed.
    dispatch(createStripePaymentSecret({ addressId }));

    // Recreated whenever the cart total or address changes. The old effect ran only
    // `if (!clientSecret)`, and the secret was cached in localStorage, so a stale
    // intent survived cart edits. With server-side verification that stale intent
    // now fails the amount check at checkout instead of silently undercharging.
  }, [dispatch, totalPrice, addressId]);

  if (!addressId) {
    return (
      <div className='max-w-lg mx-auto text-center text-slate-600 py-8'>
        Select a delivery address to continue.
      </div>
    );
  }

  if (isLoading || !clientSecret) {
    return (
      <div className='max-w-lg mx-auto'>
         <Skeleton />
      </div>
    )
  }

  return (
      <Elements stripe={stripePromise} options={{ clientSecret }}>
         <PaymentForm clientSecret={clientSecret} totalPrice={totalPrice} />
      </Elements>
    );
};

export default StripePayment
