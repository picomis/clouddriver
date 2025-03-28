/*
 * Copyright 2018 Mirantis, Inc.
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

package com.netflix.spinnaker.clouddriver.artifacts.helm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.netflix.spinnaker.clouddriver.artifacts.config.ArtifactCredentials;
import com.netflix.spinnaker.clouddriver.artifacts.config.BaseHttpArtifactCredentials;
import com.netflix.spinnaker.clouddriver.artifacts.exceptions.FailedDownloadException;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

@NonnullByDefault
@Slf4j
public class HelmArtifactCredentials extends BaseHttpArtifactCredentials<HelmArtifactAccount>
    implements ArtifactCredentials {
  public static final String CREDENTIALS_TYPE = "artifacts-helm";
  @Getter private final String name;
  @Getter private final ImmutableList<String> types = ImmutableList.of("helm/chart", "helm/index");

  @JsonIgnore private final IndexParser indexParser;

  @Override
  public boolean handlesType(String type) {
    return types.contains(type);
  }

  public HelmArtifactCredentials(HelmArtifactAccount account, OkHttpClient okHttpClient) {
    super(okHttpClient, account);
    this.name = account.getName();
    this.indexParser = new IndexParser(account.getRepository());
  }

  @Override
  public InputStream download(Artifact artifact) throws IOException {
    InputStream index = downloadIndex();

    if ("helm/index".equals(artifact.getType())) {
      return index;
    }

    List<String> urls = indexParser.findUrls(index, artifact.getName(), artifact.getVersion());
    ResponseBody downloadResponse;
    for (String url : urls) {
      try {
        downloadResponse = fetchUrl(url);
        return downloadResponse.byteStream();
      } catch (IllegalArgumentException e) {
        log.warn("Invalid url: ", url);
      }
    }
    throw new FailedDownloadException("Unable to download the contents of artifact");
  }

  public List<String> getArtifactNames() {
    InputStream index;
    List<String> names;
    try {
      index = downloadIndex();
      names = indexParser.findNames(index);
    } catch (IOException e) {
      throw new NotFoundException("Failed to download chart names for '" + name + "' account", e);
    }
    return names;
  }

  public List<String> getArtifactVersions(String artifactName) {
    InputStream index;
    List<String> versions;
    try {
      index = downloadIndex();
      versions = indexParser.findVersions(index, artifactName);
    } catch (IOException e) {
      throw new NotFoundException(
          "Failed to download chart versions for '" + name + "' account", e);
    }
    return versions;
  }

  private InputStream downloadIndex() throws IOException {
    try {
      ResponseBody indexDownloadResponse = fetchUrl(indexParser.indexPath());
      return indexDownloadResponse.byteStream();
    } catch (IOException e) {
      throw new FailedDownloadException(
          "Failed to download index.yaml file in '" + indexParser.getRepository() + "' repository",
          e);
    }
  }

  @Override
  public String getType() {
    return CREDENTIALS_TYPE;
  }
}
