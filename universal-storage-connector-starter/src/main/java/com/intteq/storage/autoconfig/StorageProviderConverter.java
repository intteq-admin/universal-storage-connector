package com.intteq.storage.autoconfig;

import com.intteq.storage.core.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Converter for binding {@code storage.provider} configuration property
 * to the {@link StorageProvider} enum.
 *
 * <p>This converter:
 * <ul>
 *   <li>Handles {@code null} and blank values safely</li>
 *   <li>Is case-insensitive</li>
 *   <li>Provides clear error messages for invalid values</li>
 * </ul>
 *
 * <p>Example supported values:
 * <pre>
 * storage.provider=S3
 * storage.provider=azure
 * storage.provider=GCS
 * </pre>
 */
@Slf4j
@Component
@ConfigurationPropertiesBinding
public class StorageProviderConverter
        implements Converter<String, StorageProvider> {

    @Override
    public StorageProvider convert(String source) {

        if (source == null || source.isBlank()) {
            log.warn(
                    "storage.provider is not configured or is blank. " +
                            "No storage provider will be activated."
            );
            return null; // allows application to start
        }

        try {
            return StorageProvider.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {

            String supportedValues =
                    Arrays.stream(StorageProvider.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    "Invalid value for storage.provider: '" + source + "'. " +
                            "Supported values are: " + supportedValues
            );
        }
    }
}