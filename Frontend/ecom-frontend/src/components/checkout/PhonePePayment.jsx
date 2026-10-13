import React from 'react'

const PhonePePayment = () => {
  return (
      <div className='h-96 flex justify-center items-center'>
        <Alert
          variant="filled"
          severity="warning"
          variant="filled"
          style={{ maxWidth: "400px" }}
        >
          <AlertTitle>PayPal Method Unavailble</AlertTitle>
          PhonePe payment is not implemented yet.{" "}
        </Alert>
      </div>
    );
};

export default PhonePePayment;