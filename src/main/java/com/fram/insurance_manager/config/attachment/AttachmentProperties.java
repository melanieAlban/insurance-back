package com.fram.insurance_manager.config.attachment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attachment")
public class AttachmentProperties {
    private List<String> allowedTypes;
    private String maxFileSize;
    private String directoryPath;
}
