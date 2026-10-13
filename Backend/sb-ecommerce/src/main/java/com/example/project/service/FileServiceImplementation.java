package com.example.project.service;

import com.example.project.globalExceptionHandler.APIexception;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

import static java.util.UUID.randomUUID;

@Service
public class FileServiceImplementation implements FileService{

    /**
     * Uploaded images are served back as static content from /images/**, so the
     * extension has to be constrained. Anything that a browser will render as markup
     * - .html, .svg, .xhtml - turns an upload into stored XSS on the app's own origin.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new APIexception("No image file was supplied.");
        }

        // getOriginalFilename() is nullable, and the old code called
        // originalFileName.substring(lastIndexOf('.')) on it unguarded: a null name
        // threw NPE and a name with no dot threw StringIndexOutOfBoundsException,
        // both surfacing as a 500.
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());

        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (extension == null) {
            throw new APIexception("Image file must have an extension.");
        }

        extension = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new APIexception("Unsupported image type '" + extension
                    + "'. Allowed types are " + ALLOWED_EXTENSIONS + ".");
        }

        //Generate a unique file name to avoid overriding of file with same name on each other
        String newFileName = randomUUID() + "." + extension;

        Path folder = Paths.get(path).toAbsolutePath().normalize();

        // mkdir() only created the final segment and silently returned false if a
        // parent was missing; createDirectories handles the whole chain.
        Files.createDirectories(folder);

        Path target = folder.resolve(newFileName).normalize();

        // Defence in depth. The name is a server-generated UUID so it cannot escape
        // the folder, but assert it anyway rather than trusting that invariant.
        if (!target.startsWith(folder)) {
            throw new APIexception("Invalid upload path.");
        }

        Files.copy(file.getInputStream(), target);

        //returning fileName
        return newFileName;
    }
}
