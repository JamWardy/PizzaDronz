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
        String date = args[0];
        String baseUrlStr = args[1];
        System.out.println(args[1]);
        if (!baseUrlStr.endsWith("/")){
            baseUrlStr += "/";
        }
        try {
            URL baseUrl = new URL(baseUrlStr);
            Order[] orders = Order.getOrders(baseUrl, date);
            for (Order order: orders){
                System.out.println(order.isCardExpiryValid(date));
            }
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }
    }
}