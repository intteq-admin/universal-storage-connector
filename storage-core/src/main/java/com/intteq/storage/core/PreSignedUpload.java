package com.intteq.storage.core;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
/**
 * Represents a pre-signed upload operation.
 *
 * Contains the upload URL, generated file name,
 * public access URL, and any required request headers.
 */
public class PreSignedUpload {

    /** Pre-signed URL used for HTTP PUT upload */
    private String uploadUrl;

    /** Generated unique file name */
    private String fileName;

    /** Public URL of the uploaded file */
    private String fileUrl;

    /** Optional headers required for upload */
    private Map<String, String> headers;
}