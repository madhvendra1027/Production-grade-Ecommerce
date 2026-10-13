import { useForm } from "react-hook-form";
import InputField from "../../shared/InputField";
import { useEffect, useState } from "react";
import Button from "@mui/material/Button";
import { useDispatch } from "react-redux";
import {
  addNewCategoryFromDashboard,
  updateCategoryFromDashboard,
} from "../../../store/actions";
import toast from "react-hot-toast";
import Spinners from "../../shared/Spinners";

const AddCategoryForm = ({ setOpen, category, update = false }) => {
  const [loader, setLoader] = useState(false);
  const dispatch = useDispatch();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm({
    mode: "onTouched",
  });

  const saveCategoryHandler = (data) => {
    if (!update) {
      // Previously built sendData with categoryId: selectedCategory.id, where
      // selectedCategory was a useState that nothing ever set - so this threw on
      // every submit. A new category has no id; only the name is needed.
      dispatch(
        addNewCategoryFromDashboard(
          { categoryName: data.categoryName },
          toast,
          reset,
          setLoader,
          setOpen,
        ),
      );
    } else {
      // Was { ...data, id: product.id } - `product` is not in scope in this
      // component (the prop is `category`), so editing threw a ReferenceError. It
      // then dispatched updateAdminProductFromDashboard, the *product* action.
      dispatch(
        updateCategoryFromDashboard(
          {
            categoryName: data.categoryName,
            categoryId: category?.categoryId ?? category?.id,
          },
          toast,
          reset,
          setLoader,
          setOpen,
        ),
      );
    }
  };

  useEffect(() => {
    if (update && category) {
      // Was categories?.categoryName, reading a name off the categories *array*
      // rather than the category being edited, so the field opened empty.
      setValue("categoryName", category.categoryName);
    }
  }, [update, category, setValue]);

  return (
    <div className="py-5 relative h-full">
      {/* onSubmit belongs on the form, not the wrapping div - it was on the div,
          so the submit handler never ran through react-hook-form's validation. */}
      <form className="space-y-4" onSubmit={handleSubmit(saveCategoryHandler)}>
        <div className="flex md:flex-row flex-col gap-4 w-full">
          <InputField
            label="Category Name"
            required
            id="categoryName"
            type="text"
            message="This field is required"
            placeholder="Category Name"
            register={register}
            errors={errors}
          />
        </div>

          <div className="flex w-full justify-between items-center absolute bottom-14">
            <Button
              disabled={loader}
              onClick={() => setOpen(false)}
              variant="outlined"
              className="text-white py-[10px] px-4 text-sm font-medium"
            >
              Cancel
            </Button>

            <Button
              disabled={loader}
              type="submit"
              variant="contained"
              color="primary"
              className="bg-custom-blue text-white py-[10px] px-4 text-sm font-medium"
            >
              {loader ? (
                <div className="flex gap-2 items-center">
                  <Spinners /> Loading...
                </div>
              ) : (
                "Save"
              )}
            </Button>
          </div>
      </form>
    </div>
  );
};

export default AddCategoryForm;
