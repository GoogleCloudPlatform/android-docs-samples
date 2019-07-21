package com.google.cloud.examples.dialogflow.utils;


import com.google.cloud.dialogflow.v2beta1.KnowledgeBase;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesClient;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesSettings;
import com.google.cloud.dialogflow.v2beta1.ProjectName;

import java.util.ArrayList;

public class KnowledgebaseUtils {

    /**
     * List Knowledge bases
     *
     * @param projectId Project/agent id.
     */
    public static ArrayList<String> listKnowledgeBases(String projectId, KnowledgeBasesSettings knowledgeBasesSettings) throws Exception {
        ArrayList<String> ids = new ArrayList<>();
        // Instantiates a client
        try (KnowledgeBasesClient knowledgeBasesClient = KnowledgeBasesClient.create(knowledgeBasesSettings)) {
            // Set the entity type name using the projectID (my-project-id) and entityTypeId (KIND_LIST)
            ProjectName projectName = ProjectName.of(projectId);
            for (KnowledgeBase knowledgeBase :
                    knowledgeBasesClient.listKnowledgeBases(projectName).iterateAll()) {
                System.out.format(" - Display Name: %s\n", knowledgeBase.getDisplayName());
                System.out.format(" - Knowledge ID: %s\n", knowledgeBase.getName());
                ids.add(knowledgeBase.getName());
            }
        }

        return ids;
    }
}
