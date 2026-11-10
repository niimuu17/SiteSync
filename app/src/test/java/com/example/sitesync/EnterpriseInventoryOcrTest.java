package com.example.sitesync;

import com.example.sitesync.data.local.entities.InventoryItem;
import com.example.sitesync.data.local.entities.StockTransaction;
import com.example.sitesync.data.network.ConstructionMarketPriceClient;
import com.example.sitesync.hardware.ChallanOcrVisionClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Enterprise Tests for Challan OCR Vision & Brave Market Intelligence Engine.
 */
public class EnterpriseInventoryOcrTest {

    @Test
    public void testMarketPriceVariance_FairMarket() {
        // Cement benchmark 560 BDT, billed 565 BDT (+0.89%) -> FAIR_MARKET
        ConstructionMarketPriceClient.PriceEvaluation eval =
                ConstructionMarketPriceClient.computeEvaluation("Shah Cement", 565.0, 560.0, "Test");

        assertEquals(ConstructionMarketPriceClient.PriceStatus.FAIR_MARKET, eval.status);
        assertEquals("Fair Market Price", eval.statusSummary);
    }

    @Test
    public void testMarketPriceVariance_OverbilledInflation() {
        // Rebar benchmark 92,000 BDT, billed 108,000 BDT (+17.4%) -> OVER_BILLED
        ConstructionMarketPriceClient.PriceEvaluation eval =
                ConstructionMarketPriceClient.computeEvaluation("BSRM 500W 16mm Rebar", 108000.0, 92000.0, "Test");

        assertEquals(ConstructionMarketPriceClient.PriceStatus.OVER_BILLED, eval.status);
        assertTrue(eval.variancePct > 8.0);
        assertTrue(eval.statusSummary.contains("Over Market Rate"));
    }

    @Test
    public void testMarketPriceVariance_BulkDiscount() {
        // Sylhet sand benchmark 48 BDT, billed 40 BDT (-16.7%) -> DISCOUNTED_BULK
        ConstructionMarketPriceClient.PriceEvaluation eval =
                ConstructionMarketPriceClient.computeEvaluation("Sylhet Sand", 40.0, 48.0, "Test");

        assertEquals(ConstructionMarketPriceClient.PriceStatus.DISCOUNTED_BULK, eval.status);
        assertTrue(eval.variancePct < -8.0);
        assertTrue(eval.statusSummary.contains("Bulk Discount"));
    }

    @Test
    public void testBaselineRateLookup() {
        double cementRate = ConstructionMarketPriceClient.getBaselineRate("shah cement special", "BAG");
        assertEquals(565.0, cementRate, 0.01);

        double rebarRate = ConstructionMarketPriceClient.getBaselineRate("bsrm rebar", "TON");
        assertEquals(93500.0, rebarRate, 0.01);

        double brickRate = ConstructionMarketPriceClient.getBaselineRate("first class auto brick", "PCS");
        assertTrue(brickRate >= 12.0 && brickRate <= 16.0);
    }

    @Test
    public void testOcrTextHeuristicParsing() {
        // Simulates realistic printed/handwritten delivery challan text
        String sampleChallanText = "MEGHNA CEMENT & STEEL MILLS LTD\n" +
                "Delivery Challan No: CH-74892\n" +
                "Date: 10/11/2026\n" +
                "Delivery to: Metro Central Tower Site A\n" +
                "Item 1: Portland Composite Cement - 200 bags @ 560\n" +
                "Item 2: BSRM 500W Deformed Bar - 5.5 ton @ 92000\n" +
                "Item 3: Sylhet Sand 2.5 FM - 600 cft @ 48\n";

        ChallanOcrVisionClient.ParsedChallan parsed =
                ChallanOcrVisionClient.parseChallanFromTextHeuristics(sampleChallanText);

        assertTrue("Valid challan text must pass validation gate", parsed.isValidReceipt);
        assertEquals("CH-74892", parsed.challanNumber);
        assertEquals(3, parsed.items.size());
        assertEquals(646800.0, parsed.calculateTotalAmount(), 0.01);
    }

    @Test
    public void testReceiptValidation_RejectsNonReceiptText() {
        // Text from a random non-receipt photo (e.g. signpost, office whiteboard, book page)
        String randomPhotoText = "CONFERENCE ROOM B\n" +
                "Weekly Team Standup Meeting\n" +
                "Agenda:\n" +
                "1. Review Q3 architectural blueprints\n" +
                "2. Coordinate crane schedule\n" +
                "3. Coffee break at 11:30 AM\n";

        ChallanOcrVisionClient.ParsedChallan parsed =
                ChallanOcrVisionClient.parseChallanFromTextHeuristics(randomPhotoText);

        assertFalse("Non-receipt text must be strictly rejected", parsed.isValidReceipt);
        assertTrue("Items list must be empty for non-receipts (no dummy items injected)", parsed.items.isEmpty());
        assertNotNull("Rejection reason must be provided", parsed.rejectionReason);
        assertTrue("Rejection reason should describe issue", parsed.rejectionReason.contains("does not appear to be a construction delivery challan"));
    }

    @Test
    public void testReceiptValidation_RejectsEmptyOrBlankText() {
        ChallanOcrVisionClient.ParsedChallan emptyResult =
                ChallanOcrVisionClient.parseChallanFromTextHeuristics("");

        assertFalse("Empty OCR output must be rejected", emptyResult.isValidReceipt);
        assertTrue("No dummy items should be created on empty scan", emptyResult.items.isEmpty());
        assertNotNull(emptyResult.rejectionReason);

        ChallanOcrVisionClient.ParsedChallan nullResult =
                ChallanOcrVisionClient.parseChallanFromTextHeuristics(null);
        assertFalse("Null OCR output must be rejected", nullResult.isValidReceipt);
        assertTrue(nullResult.items.isEmpty());
    }

    @Test
    public void testReceiptValidation_BanglaChallanAccepted() {
        String banglaChallanText = "মেসার্স ঢাকা ট্রেডার্স\n" +
                "ডেলিভারী চালান নং: CH-8821\n" +
                "তারিখ: ১২/১১/২০২৬\n" +
                "সিমেন্ট ১০০ ব্যাগ\n" +
                "রড ৫ টন\n";

        ChallanOcrVisionClient.ParsedChallan parsed =
                ChallanOcrVisionClient.parseChallanFromTextHeuristics(banglaChallanText);

        assertTrue("Bangla challan text must pass validation gate", parsed.isValidReceipt);
        assertFalse("Items should be extracted from Bangla challan", parsed.items.isEmpty());
        assertEquals("CH-8821", parsed.challanNumber);
    }

    @Test
    public void testInventoryItemCalculations() {
        InventoryItem item = new InventoryItem(
                "Shah Cement Special",
                "CEMENT",
                "BAG",
                1000.0,
                300.0,
                560.0,
                200.0,
                System.currentTimeMillis()
        );

        assertEquals(700.0, item.getCurrentBalance(), 0.01);
        assertEquals(392000.0, item.getTotalValuation(), 0.01);
        assertEquals(392000.0, item.getValuation(), 0.01);
        assertFalse(item.isLowStock());

        // Now simulate stock out exceeding reorder level
        item.setTotalStockOut(850.0);
        assertEquals(150.0, item.getCurrentBalance(), 0.01);
        assertTrue(item.isLowStock());
    }

    @Test
    public void testStockTransactionEntity() {
        StockTransaction tx = new StockTransaction(
                101L,
                "BSRM 500W 16mm",
                "Site Alpha",
                StockTransaction.TYPE_STOCK_IN,
                10.0,
                "TON",
                92000.0,
                920000.0,
                "CH-1102",
                "BSRM Steels Ltd",
                System.currentTimeMillis(),
                "Direct mill delivery"
        );

        assertTrue(tx.isStockIn());
        assertEquals(920000.0, tx.getTotalCost(), 0.01);
        assertEquals("CH-1102", tx.getChallanNumber());
    }

    @Test
    public void testReceiptDeduplicationFingerprint() {
        ChallanOcrVisionClient.ParsedChallan challan1 = new ChallanOcrVisionClient.ParsedChallan();
        challan1.challanNumber = "CH-94812";
        challan1.supplierName = "Meghna Cement & Steel Mills Ltd";
        challan1.items.add(new ChallanOcrVisionClient.ChallanItem("Shah Cement Special", "CEMENT", 120.0, "BAG", 560.0, 67200.0));
        challan1.items.add(new ChallanOcrVisionClient.ChallanItem("BSRM 500W 16mm Rebar", "STEEL", 4.5, "TON", 92000.0, 414000.0));

        ChallanOcrVisionClient.ParsedChallan challan2 = new ChallanOcrVisionClient.ParsedChallan();
        challan2.challanNumber = "ch-94812 "; // Whitespace and case difference
        challan2.supplierName = " Meghna Cement & Steel Mills Ltd ";
        challan2.items.add(new ChallanOcrVisionClient.ChallanItem("Shah Cement Special", "CEMENT", 120.0, "BAG", 560.0, 67200.0));
        challan2.items.add(new ChallanOcrVisionClient.ChallanItem("BSRM 500W 16mm Rebar", "STEEL", 4.5, "TON", 92000.0, 414000.0));

        String fp1 = challan1.getFingerprint();
        String fp2 = challan2.getFingerprint();

        assertNotNull("Fingerprint must be non-null", fp1);
        assertFalse("Fingerprint must not be empty", fp1.isEmpty());
        assertEquals("Normalized receipts must produce identical fingerprints to prevent double entry", fp1, fp2);

        // A different receipt with different items must have a distinct fingerprint
        ChallanOcrVisionClient.ParsedChallan challan3 = new ChallanOcrVisionClient.ParsedChallan();
        challan3.challanNumber = "CH-99999";
        challan3.supplierName = "Bashundhara Cement Ltd";
        challan3.items.add(new ChallanOcrVisionClient.ChallanItem("Bashundhara Cement", "CEMENT", 50.0, "BAG", 540.0, 27000.0));

        String fp3 = challan3.getFingerprint();
        assertFalse("Different receipts must yield different fingerprints", fp1.equals(fp3));
    }

    @Test
    public void testRecordedReceiptEntityAndDuplicateResult() {
        com.example.sitesync.data.local.entities.RecordedReceipt receipt =
                new com.example.sitesync.data.local.entities.RecordedReceipt(
                        "CH-94812",
                        "Meghna Cement & Steel Mills Ltd",
                        "Metro Central Tower",
                        "2026-11-10",
                        500400.0,
                        3,
                        System.currentTimeMillis(),
                        "fp_abc123",
                        "img_hash_xyz",
                        "Challan OCR Delivery"
                );

        assertEquals("CH-94812", receipt.getChallanNumber());
        assertEquals(500400.0, receipt.getTotalAmount(), 0.01);
        assertEquals(3, receipt.getItemCount());
        assertNotNull(receipt.getFormattedRecordedDate());

        com.example.sitesync.data.repository.InventoryRepository.DuplicateCheckResult dupResult =
                com.example.sitesync.data.repository.InventoryRepository.DuplicateCheckResult.duplicate(
                        "Receipt CH-94812 already recorded",
                        "CH-94812",
                        "Meghna Cement & Steel Mills Ltd",
                        System.currentTimeMillis(),
                        500400.0
                );

        assertTrue("Duplicate flag must be true", dupResult.isDuplicate);
        assertEquals("CH-94812", dupResult.matchedChallanNumber);
        assertTrue(dupResult.reason.contains("already recorded"));

        com.example.sitesync.data.repository.InventoryRepository.DuplicateCheckResult uniqueResult =
                com.example.sitesync.data.repository.InventoryRepository.DuplicateCheckResult.unique();
        assertFalse("Unique result must not be flagged as duplicate", uniqueResult.isDuplicate);
    }
}
