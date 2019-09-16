/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.dialogflow.utils;


import com.google.cloud.dialogflow.v2beta1.KnowledgeBase;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesClient;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesSettings;
import com.google.cloud.dialogflow.v2beta1.ProjectName;

import java.util.ArrayList;

public class KnowledgeBaseUtils {

    /**
     * List Knowledge bases
     *
     * @param projectId Project/agent id.
     */
    public static ArrayList<String> listKnowledgeBases(String projectId,
            KnowledgeBasesSettings knowledgeBasesSettings)
            throws Exception {
        ArrayList<String> ids = new ArrayList<>();
        // Instantiates a client
        try (KnowledgeBasesClient knowledgeBasesClient =
                     KnowledgeBasesClient.create(knowledgeBasesSettings)) {
            ProjectName projectName = ProjectName.of(projectId);
            for (KnowledgeBase knowledgeBase :
                    knowledgeBasesClient.listKnowledgeBases(projectName).iterateAll()) {
                ids.add(knowledgeBase.getName());
            }
        }

        return ids;
    }
}
