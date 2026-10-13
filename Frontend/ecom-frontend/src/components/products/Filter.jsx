import { MenuItem, Select } from "@mui/material";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import { useEffect, useState } from "react";
import { FiArrowDown, FiRefreshCw, FiSearch } from "react-icons/fi";
import { Tooltip } from "@mui/material";
import { Button } from "@mui/material";
import { FiArrowUp } from "react-icons/fi";
import { useSearchParams, useLocation, useNavigate } from "react-router-dom";

const Filter = ({ categories }) => {
  const [searchParams] = useSearchParams(); // This hook is used to read and modify the query parameters in the URL.
  const params = new URLSearchParams(searchParams);
  const pathname = useLocation().pathname; // This hook is used to access the current location object, which contains information about the current URL.
  const navigate = useNavigate(); // This hook is used to programmatically navigate to different routes in the application.
  const [category, setCategory] = useState("all");
  const [sortOrder, setSortOrder] = useState("asc");
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    const currentCategory = searchParams.get("category") || "all";
    const currentSortOrder = searchParams.get("sortby") || "desc";
    const currentSearchTerm = searchParams.get("keyword") || "";

    setCategory(currentCategory);
    setSortOrder(currentSortOrder);
    setSearchTerm(currentSearchTerm);
  }, [searchParams]);

  useEffect(() => {
    const handler = setTimeout(() => {
      if (searchTerm) {
        searchParams.set("keyword", searchTerm);
      } else {
        searchParams.delete("keyword");
      }
      navigate(`${pathname}?${searchParams.toString()}`);
    }, 700);

    return () => {
      clearTimeout(handler);
    };
  }, [searchParams, searchTerm, navigate, pathname]);
  const handleCategoryChange = (event) => {
    const selectedCategory = event.target.value;

    if (selectedCategory === "all") {
      params.delete("category");
    } else {
      params.set("category", selectedCategory);
    }
    navigate(`${pathname}?${params}`);
    setCategory(event.target.value);
  };

  const toggleSortOrder = () => {
    setSortOrder((prevOrder) => {
      const newOrder = prevOrder === "asc" ? "desc" : "asc";
      params.set("sortby", newOrder);
      return newOrder;
    });
    navigate(`${pathname}?${params}`);
  };

  const handleClearFilters = () => {
    navigate({ pathname: window.location.pathname });
  };

  return (
    <div className="flex lg:flex-row flex-col-reverse lg:justify-between justify-center items-center gap-4">
      <div className="relative flex items-center 2xl:w-[450px] sm:w-[420px] w-full">
        {/* SEARCH BAR */}
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search Products"
          className="border border-gray-400 text-slate-800 rounded-md py-2 pl-10 pr-4 w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <FiSearch className="absolute left-3 text-slate-800 size={20}" />
      </div>

      {/* CATEGORY SELECTION */}

      <div className="flex lg:flex-row flex-col gap-4 items-center">
        <FormControl
          className="text-slate-800 border-slate--700"
          variant="outlined"
          size="small"
        >
          <InputLabel id="category-select-label">Category</InputLabel>
          <Select
            labelId="category-select-label"
            value={category ?? ""}
            onChange={handleCategoryChange}
            label="Category"
            className="min-w-[120px] text-slate-800 border-slate-700"
          >
            <MenuItem value="all">All</MenuItem>
            {categories.map((item) => (
              <MenuItem key={item.categoryId} value={item.categoryName}>
                {item.categoryName}
              </MenuItem>
            ))}
            ;
          </Select>
        </FormControl>

        {/* SORT BUTTON & CLEAR FILTER */}
        <Tooltip title="Sorted by price: asc">
          <Button
            variant="contained"
            onClick={toggleSortOrder}
            color="primary"
            className="flex items-center gap-2 h-10"
          >
            Sort By
            {sortOrder === "asc" ? (
              <FiArrowUp size={20} />
            ) : (
              <FiArrowDown size={20} />
            )}
          </Button>
        </Tooltip>

        <button
          className="flex items-center gap-2 bg-rose-900 text-white px-3 py-2 rounded-md transition duration-300 ease-in shadown-md focus:outline-none"
          onClick={handleClearFilters}
        >
          <FiRefreshCw className="font-semibold" size={16} />
          <span className="font-semibold">Clear Filter</span>
        </button>
      </div>
    </div>
  );
};

export default Filter;
