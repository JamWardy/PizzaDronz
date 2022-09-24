package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
        * A very simple c l i e n t to GET JSON data from a remote s e r v e r
        */
public class TestClient {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(" T e s t c l i e n t Base−URL Echo−Parameter");
            System.err.println("you must supply the base address of the ILP REST Service \n" +
                    " e . g . http : / / r e s t s e r v i c e . somewhere and a s t r i n g to be echoed");
            System.exit(1);
        }
        try {
            String baseUrl = args[0];
            String echoBasis = args[1];
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            // we c a l l the t e s t endpoint and pass in some t e s t data which w i l l be echoed
            URL url = new URL(baseUrl + "test/" + echoBasis);
            /**
             *
             the Jackson JSON l i b r a r y provides helper methods which can d i r e c t l y
             *
             take a URL, perform the GET request convert the r e s u l t to the s p e c i f i e d c l a s s
             */
            TestResponse response = new ObjectMapper().readValue(new URL(baseUrl + "test/" + echoBasis), TestResponse.class);
            /**
             *
             some er ror checking − only needed f o r the sample ( i f the JSON data i s
             * not c o r r e c t usually an exception i s thrown )
             */
            if (!response.greeting.endsWith(echoBasis)) {
                throw new RuntimeException("wrong echo returned ");
            }
            System.out.println("The server responded as JSON−greeting : \n\n" + response.greeting);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
