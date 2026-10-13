import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useSelector } from "react-redux";
import { Outlet } from "react-router-dom";

/**
 * Route guard with three modes:
 *   publicPage  - for /login and /register: bounce authenticated users home
 *   adminOnly   - for /admin/**: require ROLE_ADMIN, or ROLE_SELLER on its subset
 *   default     - require any authenticated user
 */
const PrivateRoute = ({ publicPage = false, adminOnly = false }) => {
    const { user } = useSelector((state) => state.auth);
    const isAdmin = Boolean(user?.roles?.includes("ROLE_ADMIN"));
    const isSeller = Boolean(user?.roles?.includes("ROLE_SELLER"));
    const location = useLocation();

    if (publicPage) {
        return user ? <Navigate to="/" replace /> : <Outlet />;
    }

    // Authentication first, for every protected mode.
    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (adminOnly) {
        if (isAdmin) {
            return <Outlet />;
        }

        if (isSeller) {
            const sellerAllowedPaths = ["/admin/orders", "/admin/products"];
            const sellerAllowed = sellerAllowedPaths.some(path =>
                location.pathname.startsWith(path)
            );
            return sellerAllowed ? <Outlet /> : <Navigate to="/" replace />;
        }

        return <Navigate to="/" replace />;
    }

    // Any authenticated user may proceed.
    //
    // The previous version ran `if (!isAdmin && !isSeller) return <Navigate to="/" />`
    // here, outside the adminOnly branch, so it applied to the ordinary protected
    // routes as well. A customer holding only ROLE_USER was redirected away from
    // /checkout and /order-confirm - meaning no regular user could ever complete a
    // purchase. It went unnoticed because the seeded `admin` account carried all
    // three roles.
    return <Outlet />;
};

export default PrivateRoute;
