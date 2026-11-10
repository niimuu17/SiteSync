package com.example.sitesync;

import com.example.sitesync.data.local.entities.RequiredMaterialItemEntity;
import com.example.sitesync.data.local.entities.SiteChatMessageEntity;
import com.example.sitesync.hardware.ChatIntelligenceClient;
import com.example.sitesync.hardware.EmergencySpeechClassifier;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying:
 * 1. Emergency Voice Alerter classifier logic (Bengali & English keyword + LLM response parsing)
 * 2. Chat Intelligence engine (vernacular material extraction + critical site briefings)
 */
public class ChatIntelligenceAndVoiceAlerterTest {

    @Test
    public void testEmergencyClassifier_FireBengali_TinTolayAgunLagse() {
        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.evaluateLocalKeywords("tin tolay agun lagse");

        assertTrue("Expected fire emergency for Bengali phrase", result.isEmergency);
        assertEquals("FIRE", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
        assertNotNull(result.alertHeadline);
    }

    @Test
    public void testEmergencyClassifier_FireEnglish_SmokeAndFire() {
        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.evaluateLocalKeywords("heavy smoke and fire on the 3rd floor slab");

        assertTrue(result.isEmergency);
        assertEquals("FIRE", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
    }

    @Test
    public void testEmergencyClassifier_StructuralCollapse_Bhenge() {
        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.evaluateLocalKeywords("shuttering bhenge porche taratari shoran");

        assertTrue(result.isEmergency);
        assertEquals("COLLAPSE", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
    }

    @Test
    public void testEmergencyClassifier_FallHazard_ChhadThekePoreGese() {
        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.evaluateLocalKeywords("worker chhad theke pore gese ambulance daken");

        assertTrue(result.isEmergency);
        assertEquals("FALL", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
    }

    @Test
    public void testEmergencyClassifier_ElectricalShock_CurrentLagse() {
        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.evaluateLocalKeywords("electric shock lagse wire cut koro");

        assertTrue(result.isEmergency);
        assertEquals("ELECTRICAL", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
    }

    @Test
    public void testEmergencyClassifier_MedicalEmergency_RoktoPorche() {
        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.evaluateLocalKeywords("pa kete rokto porche first aid box koi");

        assertTrue(result.isEmergency);
        assertEquals("MEDICAL", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
    }

    @Test
    public void testEmergencyClassifier_CasualChatter_NotAnEmergency() {
        EmergencySpeechClassifier.EmergencyClassification result1 =
                EmergencySpeechClassifier.evaluateLocalKeywords("kalke shokale koytay asho");
        assertFalse("Casual arrival question must NOT trigger emergency", result1.isEmergency);
        assertEquals("NONE", result1.emergencyType);

        EmergencySpeechClassifier.EmergencyClassification result2 =
                EmergencySpeechClassifier.evaluateLocalKeywords("bhat khawa sesh ekhon kaj shuru korbo");
        assertFalse("Lunch break talk must NOT trigger emergency", result2.isEmergency);
        assertEquals("NONE", result2.emergencyType);
    }

    @Test
    public void testEmergencyClassifier_ParseLlmJsonOutput() {
        String mockJson = "{\n" +
                "  \"isEmergency\": true,\n" +
                "  \"emergencyType\": \"FIRE\",\n" +
                "  \"severity\": \"CRITICAL\",\n" +
                "  \"alertHeadline\": \"Active Gas Cylinder Leak on 4th Floor\",\n" +
                "  \"summary\": \"Worker reported strong gas hiss near welding station.\"\n" +
                "}";

        EmergencySpeechClassifier.EmergencyClassification result =
                EmergencySpeechClassifier.parseLlmOutput(mockJson, "gas hiss near welding", "Test Gemini Model");

        assertTrue(result.isEmergency);
        assertEquals("FIRE", result.emergencyType);
        assertEquals("CRITICAL", result.severity);
        assertEquals("Active Gas Cylinder Leak on 4th Floor", result.alertHeadline);
    }

    @Test
    public void testChatIntelligence_ExtractMaterialsFromBengaliChat() {
        List<SiteChatMessageEntity> chatList = new ArrayList<>();
        long now = System.currentTimeMillis();

        chatList.add(new SiteChatMessageEntity("1", "PRJ-1", "user1", "Rafiq", "WORKER",
                "10 bag cement lagbe 4th floor plaster er jonno", now, false));
        chatList.add(new SiteChatMessageEntity("2", "PRJ-1", "user2", "Karim", "WORKER",
                "300 kg rod lagbe lintel beam er binding e", now + 1000, false));
        chatList.add(new SiteChatMessageEntity("3", "PRJ-1", "user3", "Jabbar", "WORKER",
                "bhat khawa sesh amra shuru korsi", now + 2000, false));

        ChatIntelligenceClient.ChatIntelligenceResult result =
                ChatIntelligenceClient.computeLocalExtraction("PRJ-1", chatList);

        assertNotNull(result);
        assertEquals(3, result.messagesAnalyzed);
        assertTrue("Should have extracted at least 2 material items", result.requiredMaterials.size() >= 2);

        RequiredMaterialItemEntity cementItem = null;
        RequiredMaterialItemEntity rodItem = null;

        for (RequiredMaterialItemEntity item : result.requiredMaterials) {
            if ("CEMENT".equalsIgnoreCase(item.getCategory())) {
                cementItem = item;
            } else if ("STEEL".equalsIgnoreCase(item.getCategory())) {
                rodItem = item;
            }
        }

        assertNotNull("Cement item must be recognized", cementItem);
        assertEquals(10.0, cementItem.getQuantity(), 0.01);
        assertEquals("BAG", cementItem.getUnit());

        assertNotNull("Rod item must be recognized", rodItem);
        assertEquals(300.0, rodItem.getQuantity(), 0.01);
        assertEquals("KG", rodItem.getUnit());
    }

    @Test
    public void testChatIntelligence_ExtractCriticalSiteBriefings() {
        List<SiteChatMessageEntity> chatList = new ArrayList<>();
        long now = System.currentTimeMillis();

        chatList.add(new SiteChatMessageEntity("1", "PRJ-1", "user1", "Salam", "SUPERVISOR",
                "generator breakdown hoise, electric mixer bondho ache", now, false));
        chatList.add(new SiteChatMessageEntity("2", "PRJ-1", "user2", "Engineer Fahim", "ENGINEER",
                "kalke sokale RAJUK inspection audit team ashbe, sob plan ready rakhen", now + 1000, false));

        ChatIntelligenceClient.ChatIntelligenceResult result =
                ChatIntelligenceClient.computeLocalExtraction("PRJ-1", chatList);

        assertNotNull(result);
        assertTrue("Should have extracted briefings", result.importantBriefings.size() >= 2);

        boolean foundEquipmentFailure = false;
        boolean foundInspection = false;

        for (ChatIntelligenceClient.ImportantBriefingNotice notice : result.importantBriefings) {
            if ("EQUIPMENT_FAILURE".equals(notice.category)) {
                foundEquipmentFailure = true;
            } else if ("INSPECTION_AUDIT".equals(notice.category)) {
                foundInspection = true;
            }
        }

        assertTrue("Must extract equipment failure notice", foundEquipmentFailure);
        assertTrue("Must extract inspection notice", foundInspection);
    }

    @Test
    public void testChatIntelligence_ParseLlmJsonContract() {
        String mockLlmJson = "{\n" +
                "  \"requiredMaterials\": [\n" +
                "    {\n" +
                "      \"itemName\": \"BSRM 16mm Deformed Bar\",\n" +
                "      \"category\": \"STEEL\",\n" +
                "      \"quantity\": 2.5,\n" +
                "      \"unit\": \"TON\",\n" +
                "      \"urgency\": \"HIGH\",\n" +
                "      \"requestedBy\": \"Mistri Motiur\",\n" +
                "      \"rawSentence\": \"2.5 ton 16mm rod lagbe column er jonno\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"importantBriefings\": [\n" +
                "    {\n" +
                "      \"title\": \"Water Pipe Leakage on 2nd Floor\",\n" +
                "      \"details\": \"Main 2 inch PVC riser cracked during shuttering vibration.\",\n" +
                "      \"category\": \"SAFETY_HAZARD\",\n" +
                "      \"severity\": \"WARNING\",\n" +
                "      \"reportedBy\": \"Plumber Babul\",\n" +
                "      \"suggestedAction\": \"Shut off pump immediately and replace elbow joint.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        ChatIntelligenceClient.ChatIntelligenceResult result =
                ChatIntelligenceClient.parseJsonResponse("PRJ-101", mockLlmJson, 15);

        assertNotNull(result);
        assertEquals(15, result.messagesAnalyzed);
        assertEquals(1, result.requiredMaterials.size());
        assertEquals("BSRM 16mm Deformed Bar", result.requiredMaterials.get(0).getItemName());
        assertEquals(2.5, result.requiredMaterials.get(0).getQuantity(), 0.01);
        assertEquals("TON", result.requiredMaterials.get(0).getUnit());

        assertEquals(1, result.importantBriefings.size());
        assertEquals("Water Pipe Leakage on 2nd Floor", result.importantBriefings.get(0).title);
        assertEquals("WARNING", result.importantBriefings.get(0).severity);
    }
}
