export const resolveImageUrl = (imageUrl) => {
  if (!imageUrl) return "";
  const rawBackend = import.meta.env.VITE_BACK_END_URL || "http://localhost:8080";
  const backendUrl = rawBackend.endsWith("/") ? rawBackend.slice(0, -1) : rawBackend;

  if (imageUrl.startsWith("http://localhost:8080")) {
    return imageUrl.replace("http://localhost:8080", backendUrl);
  }

  if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
    return imageUrl;
  }

  const cleanPath = imageUrl.startsWith("/") ? imageUrl.slice(1) : imageUrl;
  if (cleanPath.startsWith("images/")) {
    return `${backendUrl}/${cleanPath}`;
  }

  return `${backendUrl}/images/${cleanPath}`;
};
