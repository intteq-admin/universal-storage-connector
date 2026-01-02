package com.intteq.storage.autoconfig;

import com.intteq.storage.core.StorageProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class StorageProviderConverter implements Converter<String, StorageProvider> {
    @Override
    public StorageProvider convert(String source) {
        return StorageProvider.valueOf(source.toUpperCase());
    }
}