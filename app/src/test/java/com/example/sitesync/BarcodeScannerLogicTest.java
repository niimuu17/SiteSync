package com.example.sitesync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.sitesync.hardware.TicketScannerManager;

import org.junit.Test;

public class BarcodeScannerLogicTest {

    @Test
    public void testEquipmentBarcodeScanning_RotaryHammer() {
        String scannedBarcode = "EQP-001";
        TicketScannerManager.DeliveryTicket ticket = TicketScannerManager.parseDeliveryTicket(scannedBarcode);

        assertNotNull(ticket);
        assertEquals("Bosch Rotary Hammer Drill GBH 2-26", ticket.mixId);
        assertEquals(1.0, ticket.parsedNumericQuantity, 0.001);
        assertEquals("Units", ticket.parsedUnit);
    }

    @Test
    public void testEquipmentBarcodeScanning_Compactor() {
        String scannedBarcode = "ASSET-EQP-002-PLATE-COMPACTOR";
        TicketScannerManager.DeliveryTicket ticket = TicketScannerManager.parseDeliveryTicket(scannedBarcode);

        assertNotNull(ticket);
        assertEquals("Plate Compactor 90kg Honda", ticket.mixId);
        assertEquals(1.0, ticket.parsedNumericQuantity, 0.001);
        assertEquals("Units", ticket.parsedUnit);
    }

    @Test
    public void testEquipmentBarcodeScanning_Vibrator() {
        String scannedBarcode = "EQP-003 Needle Vibrator Unit";
        TicketScannerManager.DeliveryTicket ticket = TicketScannerManager.parseDeliveryTicket(scannedBarcode);

        assertNotNull(ticket);
        assertEquals("Concrete Vibrator Needle Unit", ticket.mixId);
        assertEquals(1.0, ticket.parsedNumericQuantity, 0.001);
    }

    @Test
    public void testEquipmentBarcodeScanning_SafetyHarness() {
        String scannedBarcode = "SAFETY-HARNESS-KIT-EQP-004";
        TicketScannerManager.DeliveryTicket ticket = TicketScannerManager.parseDeliveryTicket(scannedBarcode);

        assertNotNull(ticket);
        assertEquals("Fall Arrest Safety Harness Set", ticket.mixId);
        assertEquals(1.0, ticket.parsedNumericQuantity, 0.001);
    }

    @Test
    public void testConsumablePalletBarcode_Cement() {
        String scannedBarcode = "MAT-001 | 150 Bags Portland Cement";
        TicketScannerManager.DeliveryTicket ticket = TicketScannerManager.parseDeliveryTicket(scannedBarcode);

        assertNotNull(ticket);
        assertEquals("Portland Cement (50kg Bags)", ticket.mixId);
        assertEquals(150.0, ticket.parsedNumericQuantity, 0.001);
        assertEquals("Bags", ticket.parsedUnit);
    }

    @Test
    public void testConsumablePalletBarcode_Rebar() {
        String scannedBarcode = "MAT-002 Deformed Steel Rebar 16mm Qty: 25 Tons";
        TicketScannerManager.DeliveryTicket ticket = TicketScannerManager.parseDeliveryTicket(scannedBarcode);

        assertNotNull(ticket);
        assertEquals("Structural Rebar 16mm (Tons)", ticket.mixId);
        assertEquals(25.0, ticket.parsedNumericQuantity, 0.001);
        assertEquals("Tons", ticket.parsedUnit);
    }
}
