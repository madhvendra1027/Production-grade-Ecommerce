import { useEffect, useState } from "react";
import { MdShoppingCart } from "react-icons/md";
import { useDispatch, useSelector } from "react-redux";
import Loader from "../../shared/Loader";
import { FaBoxOpen } from "react-icons/fa";
import { DataGrid } from "@mui/x-data-grid";
import {
  adminCategoryTableColumn,
} from "../../../helper/tableColumn";
import Modal from "../../shared/Modal";
import AddCategoryForm from "./AddCategoryForm";
import { fetchCategories } from "../../../store/actions";

const Category = () => {
  const [selectedCategory, setSelectedCategory] = useState("");
  const { categories, pagination } = useSelector((state) => state.products);
  const { isLoading, errorMessage } = useSelector((state) => state.errors);
  const emptyCategories = !categories || categories?.length === 0;
  const [openAddModal, setOpenAddModal] = useState(false);
  const [openUpdateModal, setOpenUpdateModal] = useState(false);
  const dispatch = useDispatch();

  // This page never loaded any data: nothing dispatched fetchCategories, so the
  // grid always rendered the "No categories created yet" empty state regardless
  // of what was in the database.
  useEffect(() => {
    dispatch(fetchCategories());
  }, [dispatch]);

  const tableRecords = categories?.map((item) => {
    return {
      id: item.categoryId,
      categoryName: item.categoryName,
    };
  });

  const [currentPage, setCurrentPage] = useState(
    pagination?.pageNumber + 1 || 1,
  );

  const handleEdit = (category) => {
    setSelectedCategory(category);
    setOpenUpdateModal(true);
  };

  // Still unimplemented: deleting a category needs a confirm dialog and an action
  // calling DELETE /api/admin/categories/{id}. Note that Category.products is
  // mapped with CascadeType.ALL on the backend, so deleting a category deletes
  // every product in it.
  const handleDelete = () => {};

  const handlePaginationChange = (paginationModel) => {};
  return (
    <div>
      <div className="pt-6 pb-10 flex justify-end">
        <button
          onClick={() => setOpenAddModal(true)}
          className="bg-custom-blue hover:bg-blue-800 text-white font-semibold py-2 px-4 flex items-center gap-2 rounded-md shadow-md transition-colors hover:text-slate-300 duration-300"
        >
          <MdShoppingCart className="text-xl" />
          Add Categories
        </button>
      </div>

      {!emptyCategories && (
        <h1 className="text-slate-800 text-3xl text-center font-bold pb-6 uppercase">
          Add Categories
        </h1>
      )}
      {isLoading ? (
        <Loader />
      ) : (
        <>
          {emptyCategories ? (
            <div className="flex flex-col items-center justify-center text-gray-600">
              <FaBoxOpen size={50} className="mb-3" />
              <h2 className="text-2xl font-semibold">
                No categories created yet
              </h2>
            </div>
          ) : (
            <div className="max-w-full">
              <DataGrid
                className="w-full"
                rows={tableRecords}
                columns={adminCategoryTableColumn(handleEdit, handleDelete)}
                paginationMode="server"
                rowCount={pagination?.totalElements || 0}
                initialState={{
                  pagination: {
                    paginationModel: {
                      pageSize: pagination?.pageSize || 10,
                      page: currentPage - 1,
                    },
                  },
                }}
                onPaginationModelChange={handlePaginationChange}
                disableRowSelectionOnClick
                disableColumnResize
                pageSizeOptions={[pagination?.pageSize || 10]}
                pagination
                paginationOptions={{
                  showFirstButton: true,
                  showLastButton: true,
                  hideNextButton: currentPage === pagination?.totalPages,
                }}
              />
            </div>
          )}
        </>
      )}

      <Modal
           open={openUpdateModal || openAddModal}
           setOpen={openUpdateModal ? setOpenUpdateModal : setOpenAddModal}
           title={openUpdateModal ? "Update Category" : "Add Category"}
           >
            <AddCategoryForm
                           setOpen={openUpdateModal ? setOpenUpdateModal : setOpenAddModal}
                           category={selectedCategory}
                           update={openUpdateModal}/>
           </Modal>
    </div>
  );
};

export default Category;
