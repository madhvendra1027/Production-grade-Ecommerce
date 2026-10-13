import React, { useEffect } from 'react'
import InputField from '../shared/InputField';
import { useForm } from 'react-hook-form';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AiOutlineLogin } from 'react-icons/ai';
import { useDispatch, useSelector } from 'react-redux';
import Spinners from '../shared/Spinners';
import { FaAddressCard } from 'react-icons/fa';
import toast from 'react-hot-toast';
import { addUpdateUserAddress } from '../../store/actions';
const AddAddressForm = ({ address, setOpenAddressModal }) => {
  const dispatch = useDispatch();
  const { btnLoader } = useSelector((state) => state.errors)
  const {
        register,
        handleSubmit,
        reset,
        setValue,
        formState: { errors },
      } = useForm({
        mode: "onTouched",
      });

  const onSaveAddressHandler = async (data) => {
              dispatch(addUpdateUserAddress(
                 data,
                 toast,
                 address?.addressId,
                 setOpenAddressModal,
              ));
      };

  useEffect(() => {
    if(address?.addressId){
      setValue("buildingName", address?.buildingName);
      setValue("streetName", address?.streetName);
      setValue("city", address?.city);
      setValue("state", address?.state);
      setValue("pincode", address?.pincode);
      setValue("country", address?.country);
}
  }, [address]);

  return (
    <div className="">
      <form
        onSubmit={handleSubmit(onSaveAddressHandler)}
        className=""
      >
        <div className="flex flex-col items-center justify-center mb-4 font-seminold text-2xl text-slate-800 py-2 px-4">
          <FaAddressCard className="mr-2 text-2xl" />
          {!address?.addressId ? 
          "Add Address" :
          "Update Address"
          }
          <h1 className="text-slate-800 text-center font-montserrat lg:text-3xl text-2xl font-bold">
          </h1>
        </div>
        <div className="flex flex-col gap-4">
          <InputField
            label="Building Name"
            required
            id="buildingName"
            type="text"
            message="*Building name is required"
            placeholder="Enter Buidling Name"
            register={register}
            errors={errors}
          />

          <InputField
            label="Street"
            required
            id="streetName"
            type="text"
            message="*Street name is required"
            placeholder="Enter your Street name"
            register={register}
            errors={errors}
          /> 

          <InputField
            label="City"
            required
            id="city"
            type="text"
            message="*City is required"
            placeholder="Enter your City name"
            register={register}
            errors={errors}
          />

            <InputField
            label="State"
            required
            id="state"
            type="text"
            message="*State is required"
            placeholder="Enter State"
            register={register}
            errors={errors}
          />

          <InputField
            label="PinCode"
            required
            id="pincode"
            type="text"
            message="Pincode is required"
            placeholder="Enter Pincode"
            register={register}
            errors={errors}
          />

          <InputField
            label="Country"
            required
            id="country"
            type="text"
            message="*Country name is required"
            placeholder="Enter Country"
            register={register}
            errors={errors}
          />
        </div>

        <button
          disabled={btnLoader}
          className="text-white bg-custom-blue px-4 py-2 rounded-md mt-4"
          type="submit"
        >{btnLoader ? <><Spinners/>Loading...</> : <>Save</>}
        </button>

      </form>
    </div>
  );
}

export default AddAddressForm;