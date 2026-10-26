
package com.example.project.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.project.globalExceptionHandler.APIexception;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static java.util.UUID.randomUUID;

@Service
public class FileServiceImplementation implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp");

    private final Cloudinary cloudinary;

    @Value("${IMAGE_STORAGE:${image.storage:local}}")
    private String storageType;

    public FileServiceImplementation(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(String path, MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new APIexception("No image file was supplied.");
        }

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null
                        ? ""
                        : file.getOriginalFilename()
        );

        String extension =
                StringUtils.getFilenameExtension(originalFileName);

        if (extension == null) {
            throw new APIexception(
                    "Image file must have an extension."
            );
        }

        extension = extension.toLowerCase(Locale.ROOT);
        System.out.println("========== IMAGE STORAGE MODE: "
                + storageType + " ==========");
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new APIexception(
                    "Unsupported image type '" + extension
                            + "'. Allowed types are "
                            + ALLOWED_EXTENSIONS + "."

            );

        }

        // Production: upload the image to Cloudinary.
        if ("cloudinary".equalsIgnoreCase(storageType)) {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", "ecommerce/products"

                    )
            );

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null) {
                throw new IOException(
                        "Cloudinary upload did not return a secure URL."
                );
            }

            // Store the full HTTPS URL in the product's image field.
            return secureUrl.toString();
        }

        // Local development: preserve the existing filesystem behavior.
        String newFileName = randomUUID() + "." + extension;

        Path folder = Paths.get(path)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(folder);

        Path target = folder.resolve(newFileName).normalize();

        if (!target.startsWith(folder)) {
            throw new APIexception("Invalid upload path.");
        }

        Files.copy(file.getInputStream(), target);

        // Store only the filename for local images.
        return newFileName;
    }
}
