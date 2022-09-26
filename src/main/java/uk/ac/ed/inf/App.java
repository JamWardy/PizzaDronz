package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        LngLat a = new LngLat(-3.19,55.945);
        System.out.println(a.inCentralArea());
    }
}