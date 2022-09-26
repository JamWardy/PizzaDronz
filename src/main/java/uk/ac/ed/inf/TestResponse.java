package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TestReponse class for use in TestClient
 * This code was included in the coursework specification
 */

public class TestResponse {
    @JsonProperty("greeting")
    public String greeting;
}
