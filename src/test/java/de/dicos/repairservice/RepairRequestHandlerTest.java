package de.dicos.repairservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RepairRequestHandlerTest {

    @Test
    void toJsonBuildsRequestFromQueryParams() {
        Map<String, List<String>> params = RepairRequestHandler.parseQuery(
                "customerNumber=42&carModel=Golf&numberPlate=B-AB-123&preferredDate=2026-10-01"
                + "&repairOperations=Bremsen:120.0&repairOperations=%C3%96lwechsel:49.9");

        String json = RepairRequestHandler.toJson(params);

        assertEquals("{\"customerNumber\":42,\"carModel\":\"Golf\",\"numberPlate\":\"B-AB-123\","
                + "\"preferredDate\":\"2026-10-01\",\"repairOperations\":"
                + "[{\"description\":\"Bremsen\",\"priceEstimation\":120.0},"
                + "{\"description\":\"Ölwechsel\",\"priceEstimation\":49.9}]}", json);
    }

    @Test
    void toJsonHandlesMissingOptionalFields() {
        Map<String, List<String>> params = RepairRequestHandler.parseQuery("customerNumber=7&numberPlate=B-XY-9");

        String json = RepairRequestHandler.toJson(params);

        assertEquals("{\"customerNumber\":7,\"carModel\":\"\",\"numberPlate\":\"B-XY-9\","
                + "\"preferredDate\":\"\",\"repairOperations\":[]}", json);
    }
}
