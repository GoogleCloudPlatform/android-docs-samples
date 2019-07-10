package com.example.mlapitest.utils;


import com.google.cloud.dialogflow.v2beta1.KnowledgeBase;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBaseName;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesClient;
import com.google.cloud.dialogflow.v2beta1.KnowledgeBasesSettings;
import com.google.cloud.dialogflow.v2beta1.ProjectName;

import java.util.ArrayList;

public class KnowledgebaseManagement {

    // [START dialogflow_list_knowledge_base]
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
    // [END dialogflow_list_knowledge_base]

    // [START dialogflow_create_knowledge_base]
    /**
     * Create a Knowledge base
     *
     * @param projectId Project/agent id.
     * @param displayName Name of the knowledge base.
     */
    public static void createKnowledgeBase(String projectId, String displayName) throws Exception {
        // Instantiates a client
        try (KnowledgeBasesClient knowledgeBasesClient = KnowledgeBasesClient.create()) {

            KnowledgeBase knowledgeBase = KnowledgeBase.newBuilder().setDisplayName(displayName).build();
            ProjectName projectName = ProjectName.of(projectId);
            KnowledgeBase response = knowledgeBasesClient.createKnowledgeBase(projectName, knowledgeBase);
            System.out.format("Knowledgebase created:\n");
            System.out.format("Display Name: %s \n", response.getDisplayName());
            System.out.format("Knowledge ID: %s \n", response.getName());
        }
    }
    // [END dialogflow_create_knowledge_base]

    // [START dialogflow_get_knowledge_base]

    /**
     * @param knowledgeBaseId Knowledge base id.
     * @param projectId Project/agent id.
     * @throws Exception
     */
    public static void getKnowledgeBase(String projectId, String knowledgeBaseId) throws Exception {

        // Instantiates a client
        try (KnowledgeBasesClient knowledgeBasesClient = KnowledgeBasesClient.create()) {
            KnowledgeBaseName knowledgeBaseName = KnowledgeBaseName.of(projectId, knowledgeBaseId);
            KnowledgeBase response = knowledgeBasesClient.getKnowledgeBase(knowledgeBaseName);
            System.out.format("Got Knowledge Base:\n");
            System.out.format(" - Display Name: %s\n", response.getDisplayName());
            System.out.format(" - Knowledge ID: %s\n", response.getName());
        }
    }
    // [END dialogflow_get_knowledge_base]
    // [START dialogflow_delete_knowledge_base]

    /**
     * @param knowledgeBaseId Knowledge base id.
     * @param projectId Project/agent id.
     * @throws Exception
     */
    public static void deleteKnowledgeBase(String projectId, String knowledgeBaseId)
            throws Exception {
        // Instantiates a client
        try (KnowledgeBasesClient knowledgeBasesClient = KnowledgeBasesClient.create()) {
            KnowledgeBaseName knowledgeBaseName = KnowledgeBaseName.of(projectId, knowledgeBaseId);
            knowledgeBasesClient.deleteKnowledgeBase(knowledgeBaseName);
            System.out.format("KnowledgeBase has been deleted.\n");
        }
    }
    // [END dialogflow_delete_knowledge_base]

    // [START run_application]
}
