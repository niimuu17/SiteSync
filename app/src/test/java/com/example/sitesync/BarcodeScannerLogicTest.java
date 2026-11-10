package com.example.sitesync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.sitesync.hardware.ChallanOcrVisionClient;

import org.junit.Test;

public class BarcodeScannerLogicTest {

    @Test
    public void testChallanOcrTextParsing_MaterialsAndEquipment() {
        ChallanOcrVisionClient.ParsedChallan challan = new ChallanOcrVisionClient.ParsedChallan();
        challan.supplierName = "BSRM Steels Ltd";
        challan.challanNumber = "CH-90812";
        challan.date = "10/11/2026";
        challan.items.add(new ChallanOcrVisionClient.ChallanItem("BSRM 500W Deformed Bar 16mm", "STEEL", 10.0, "TON", 92000.0, 920000.0));

        assertNotNull(challan);
        assertEquals("CH-90812", challan.challanNumber);
        assertEquals(1, challan.items.size());
        assertEquals(920000.0, challan.calculateTotalAmount(), 0.01);
    }
}
