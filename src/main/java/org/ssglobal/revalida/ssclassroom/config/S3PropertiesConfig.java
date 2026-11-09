package org.ssglobal.revalida.ssclassroom.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class S3PropertiesConfig {

  private String endpoint;
  private String region;
  private String accessKey;
  private String secretKey;
  private Map<String, String> buckets;

}
