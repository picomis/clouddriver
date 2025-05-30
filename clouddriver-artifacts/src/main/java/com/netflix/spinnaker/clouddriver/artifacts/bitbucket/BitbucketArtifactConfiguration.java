/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.artifacts.bitbucket;

import com.netflix.spinnaker.credentials.CredentialsTypeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("artifacts.bitbucket.enabled")
@EnableConfigurationProperties(BitbucketArtifactProviderProperties.class)
@RequiredArgsConstructor
@Slf4j
class BitbucketArtifactConfiguration {
  private final BitbucketArtifactProviderProperties bitbucketArtifactProviderProperties;

  @Bean
  public CredentialsTypeProperties<BitbucketArtifactCredentials, BitbucketArtifactAccount>
      bitbucketCredentialsProperties(OkHttpClient okHttpClient) {
    return CredentialsTypeProperties
        .<BitbucketArtifactCredentials, BitbucketArtifactAccount>builder()
        .type(BitbucketArtifactCredentials.CREDENTIALS_TYPE)
        .credentialsClass(BitbucketArtifactCredentials.class)
        .credentialsDefinitionClass(BitbucketArtifactAccount.class)
        .defaultCredentialsSource(bitbucketArtifactProviderProperties::getAccounts)
        .credentialsParser(bc -> new BitbucketArtifactCredentials(bc, okHttpClient))
        .build();
  }
}
