import axios from "axios";

const rawBaseUrl = import.meta.env.VITE_BACK_END_URL || "http://localhost:8080";
const cleanBaseUrl = rawBaseUrl.endsWith("/") ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

const api = axios.create({
    baseURL: `${cleanBaseUrl}/api`,
    withCredentials: true,
});

export default api;