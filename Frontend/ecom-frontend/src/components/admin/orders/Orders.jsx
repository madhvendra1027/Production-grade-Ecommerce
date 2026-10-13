import { FaShoppingCart } from "react-icons/fa";
import OrderTable from "./OrderTable";
import { useSelector } from "react-redux";
import useOrderFilter from "../../hooks/useOrderFilter";
const Orders = () => {
  // const adminOrder = [ { "orderId": 17, "email": "user1@example.com", "totalAmount": 405, "orderStatus": "Order Accepted!", "addressId": 5} ];
  // const pagination = { pageNumber: 0, pageSize: 50, totalElements: 11, totalPages: 1, lastPage: true}

  const { adminOrder, pagination } = useSelector((state) => state.order);
  const emptyOrder = !adminOrder || adminOrder?.length === 0;

  useOrderFilter();
  return (
    <div className="pb-6 pt-20">
      {emptyOrder ? (
        <div className="flex flex-col items-center justify-center text-gray-600 py-10">
          <FaShoppingCart size={50} className="mb-3" />
          <h2 className="text-2xl font-semibold">No Orders Placed yet</h2>
        </div>
      ) : (
        <OrderTable adminOrder={adminOrder} pagination={pagination}/>
      )}
    </div>
  );
};

export default Orders;
