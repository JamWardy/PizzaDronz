package uk.ac.ed.inf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void testGetThreeRestaurants(){
        try {
            Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL("https://ilp-rest.azurewebsites.net/"));
            assertEquals(restaurants.length, 4);
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    public void testCentralArea(){
        LngLat pointA = new LngLat(-3.19,55.945);
        LngLat pointB = new LngLat(-3.19, 55.947);
        LngLat pointC = new LngLat(-3.18, 55.947);
        LngLat pointD = new LngLat(-3.18, 55.945);
        LngLat pointE = new LngLat(-3.18, 55.94);
        LngLat pointF = new LngLat(-3.19, 55.94);
        LngLat pointG = new LngLat(-3.195, 55.94);
        LngLat pointH = new LngLat(-3.195, 55.945);
        LngLat pointI = new LngLat(-3.195, 55.947);
        assertTrue(pointA.inCentralArea());
        assertFalse(pointB.inCentralArea());
        assertFalse(pointC.inCentralArea());
        assertFalse(pointD.inCentralArea());
        assertFalse(pointE.inCentralArea());
        assertFalse(pointF.inCentralArea());
        assertFalse(pointG.inCentralArea());
        assertFalse(pointH.inCentralArea());
        assertFalse(pointI.inCentralArea());
    }

    public void testPizzaCombination(){
        try {
            Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL("https://ilp-rest.azurewebsites.net/"));
            assertEquals(Order.getDeliveryCost(restaurants, "Margarita"), 1100);
            assertEquals(Order.getDeliveryCost(restaurants, "Meat Lover", "Vegan Delight"), 2600);
            assertEquals(Order.getDeliveryCost(restaurants, "Super Cheese", "All Shrooms", "Super Cheese"), 3800);
            assertEquals(Order.getDeliveryCost(restaurants, "Proper Pizza", "Proper Pizza", "Pineapple & Ham & Cheese", "Pineapple & Ham & Cheese"), 4700);
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    public void testCloseTo(){
        LngLat a = new LngLat(100,100);
        assertTrue(a.closeTo(a));
        LngLat b = new LngLat(100.5, 100);
        assertFalse(b.closeTo(a));
        LngLat c = new LngLat(100.0001, 100.0001);
        assertTrue(a.closeTo(c));
    }

    public void testMoves(){
        LngLat a = new LngLat(100,100);
        LngLat b = new LngLat(100.00015,100);
        LngLat c = new LngLat(100,99.99985);
        assertEquals(b, a.nextPosition(0));
        assertEquals(c, a.nextPosition(270));
    }

}
