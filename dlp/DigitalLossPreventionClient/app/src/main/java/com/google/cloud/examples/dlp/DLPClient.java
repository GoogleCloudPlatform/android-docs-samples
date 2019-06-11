/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.dlp;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.dlp.v2.DlpServiceSettings;
import com.google.common.collect.ImmutableList;
import com.google.privacy.dlp.v2.ByteContentItem;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.Finding;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.InspectContentRequest;
import com.google.privacy.dlp.v2.InspectContentResponse;
import com.google.privacy.dlp.v2.Likelihood;
import com.google.privacy.dlp.v2.ProjectName;
import com.google.privacy.dlp.v2.RedactImageRequest;
import com.google.privacy.dlp.v2.RedactImageResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Calls the DLP API using the Google Cloud client library.
 */
@WorkerThread
public class DLPClient {

    private final DlpServiceClient mClient;
    private final String mProjectId;

    /**
     * Create a new client.
     * <p>
     * Ensure that you have enabled the DLP API for the given project.
     *
     * @param credentials authentication credentials
     * @param projectId   project ID
     */
    public DLPClient(@NonNull GoogleCredentials credentials, @NonNull String projectId) {
        mProjectId = projectId;

        // create the client
        try {
            mClient = DlpServiceClient.create(DlpServiceSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create client", e);
        }
    }

    /**
     * Releases the underlying gRPC resources
     */
    public void shutdown() {
        mClient.shutdown();
    }

    /**
     * Example showing how to inspect a string to identify names, phone number, and
     * credit card numbers.
     *
     * @param string text to analyze
     * @return a string describing what was identified in the string
     */
    @NonNull
    public String inspectString(@NonNull String string) {
        // set sources of information to identify
        List<InfoType> infoTypes = ImmutableList.of(
                InfoType.newBuilder().setName("PERSON_NAME").build(),
                InfoType.newBuilder().setName("CREDIT_CARD_NUMBER").build(),
                InfoType.newBuilder().setName("PHONE_NUMBER").build());

        // configuration for the request
        InspectConfig inspectConfig =
                InspectConfig.newBuilder()
                        .addAllInfoTypes(infoTypes)
                        .setMinLikelihood(Likelihood.LIKELIHOOD_UNSPECIFIED)
                        .setLimits(InspectConfig.FindingLimits.newBuilder()
                                .setMaxFindingsPerRequest(0)
                                .build())
                        .setIncludeQuote(true)
                        .build();

        // the content to be analyzed
        ContentItem contentItem = ContentItem.newBuilder()
                .setByteItem(ByteContentItem.newBuilder()
                        .setType(ByteContentItem.BytesType.TEXT_UTF8)
                        .setData(ByteString.copyFromUtf8(string))
                        .build())
                .build();

        // create a request
        InspectContentRequest request =
                InspectContentRequest.newBuilder()
                        .setParent(ProjectName.of(mProjectId).toString())
                        .setInspectConfig(inspectConfig)
                        .setItem(contentItem)
                        .build();

        // call the API
        InspectContentResponse response = mClient.inspectContent(request);

        // format response into a string
        StringBuilder sb = new StringBuilder("Findings:");
        if (response.getResult().getFindingsCount() > 0) {
            for (Finding finding : response.getResult().getFindingsList()) {
                sb.append("\n")
                        .append("  Quote: ").append(finding.getQuote())
                        .append("  Info type: ").append(finding.getInfoType().getName())
                        .append("  Likelihood: ").append(finding.getLikelihood());
            }
        } else {
            sb.append("No findings.");
        }
        return sb.toString();
    }

    /**
     * Example showing how to inspect an image to identify names, phone number, and
     * credit card numbers.
     *
     * @param inputImage image to analyze (PNG)
     * @return a redacted image
     */
    @NonNull
    public ByteString redactPhoto(@NonNull ByteString inputImage) {
        // set sources of information to identify
        List<InfoType> infoTypes = ImmutableList.of(
                InfoType.newBuilder().setName("PERSON_NAME").build(),
                InfoType.newBuilder().setName("CREDIT_CARD_NUMBER").build(),
                InfoType.newBuilder().setName("PHONE_NUMBER").build());

        // set sources of information that should be redacted (to match infoTypes)
        List<RedactImageRequest.ImageRedactionConfig> imageRedactionConfigs =
                infoTypes.stream().map(infoType ->
                        RedactImageRequest.ImageRedactionConfig.newBuilder()
                                .setInfoType(infoType)
                                .build())
                        .collect(Collectors.toList());

        // configuration for the request
        InspectConfig inspectConfig =
                InspectConfig.newBuilder()
                        .addAllInfoTypes(infoTypes)
                        .setMinLikelihood(Likelihood.POSSIBLE)
                        .build();

        // content to be redacted
        ByteContentItem byteContentItem =
                ByteContentItem.newBuilder()
                        .setType(ByteContentItem.BytesType.IMAGE_PNG)
                        .setData(inputImage)
                        .build();

        // create a request
        RedactImageRequest redactImageRequest =
                RedactImageRequest.newBuilder()
                        .setParent(ProjectName.of(mProjectId).toString())
                        .addAllImageRedactionConfigs(imageRedactionConfigs)
                        .setByteItem(byteContentItem)
                        .setInspectConfig(inspectConfig)
                        .build();

        // call the API and return the redacted image
        RedactImageResponse redactImageResponse = mClient.redactImage(redactImageRequest);
        return redactImageResponse.getRedactedImage();
    }

}
