import { useSearchParams } from "react-router-dom";
import { getOrdersForDashboard } from "../../store/actions";
import { useDispatch, useSelector } from "react-redux";
import { useEffect } from "react";

const useOrderFilter = () => {
    const[searchParams] = useSearchParams();
    const dispatch = useDispatch();
    const { user } = useSelector((state) => state.auth);
    const isAdmin = user && user?.roles.includes("ROLE_ADMIN");
    useEffect(() => {
        const params = new URLSearchParams();
        const currentPage = searchParams.get("page")
              ? Number(searchParams.get("page"))
              : 1;

        params.set("pageNumber", currentPage - 1);

        const queryString = params.toString();
        console.log("QUERY STRING", queryString);

        dispatch(getOrdersForDashboard(queryString, isAdmin));
        
    }, [dispatch, searchParams]);
}

export default useOrderFilter;