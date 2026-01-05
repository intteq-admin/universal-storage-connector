package com.intteq.storage.core.util;


/**
 * Utility methods for working with MIME (media) types.
 *
 * <p>This class provides safe helpers for extracting file extensions from
 * {@code Content-Type} values commonly used in HTTP and object storage systems.
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Strips MIME parameters (e.g. {@code "; charset=utf-8"})</li>
 *   <li>Returns a leading dot (e.g. {@code ".png"})</li>
 *   <li>Falls back to {@code ".bin"} for null or invalid input</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * MimeTypeUtil.toExtension("image/png");                     // ".png"
 * MimeTypeUtil.toExtension("text/html; charset=utf-8");     // ".html"
 * MimeTypeUtil.toExtension("application/json;charset=UTF-8"); // ".json"
 * MimeTypeUtil.toExtension(null);                            // ".bin"
 * }</pre>
 *
 * <p>This utility is intended to be shared across all storage providers
 * (Azure Blob Storage, Google Cloud Storage, Amazon S3) to ensure consistent
 * object naming behavior.
 */
public final class MimeTypeUtil {

    private MimeTypeUtil() {
        // Utility class; prevent instantiation
    }

    /**
     * Extracts a file extension from a MIME {@code Content-Type} value.
     *
     * <p>If the content type contains parameters (for example,
     * {@code "text/html; charset=utf-8"}), only the subtype portion
     * ({@code "html"}) is used.
     *
     * <p>If the content type is {@code null}, empty, or does not contain a
     * subtype, this method returns {@code ".bin"}.
     *
     * @param contentType the MIME content type (e.g. {@code "image/png"},
     *                    {@code "text/html; charset=utf-8"})
     * @return a file extension starting with a dot (e.g. {@code ".png"}),
     *         or {@code ".bin"} if the content type is invalid
     */
    public static String toExtension(String contentType) {
        if (contentType == null || !contentType.contains("/")) {
            return ".bin";
        }

        String subtype = contentType.substring(contentType.indexOf('/') + 1);

        // Strip MIME parameters (e.g. "; charset=utf-8")
        int paramIndex = subtype.indexOf(';');
        if (paramIndex > 0) {
            subtype = subtype.substring(0, paramIndex).trim();
        }

        if (subtype.isEmpty()) {
            return ".bin";
        }

        return "." + subtype;
    }
}
