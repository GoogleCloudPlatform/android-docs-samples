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
    public static ArrayList<String> listKnowledgeBases(String projectId, KnowledgeBasesSettings knowledgeBasesSettings) throws Exception {
        ArrayList<String> ids = new ArrayList<>();
        // Instantiates a client
        try (KnowledgeBasesClient knowledgeBasesClient = KnowledgeBasesClient.create(knowledgeBasesSettings)) {
            ProjectName projectName = ProjectName.of(projectId);
            for (KnowledgeBase knowledgeBase :
                    knowledgeBasesClient.listKnowledgeBases(projectName).iterateAll()) {
                ids.add(knowledgeBase.getName());
            }
        }

        return ids;
    }
}
