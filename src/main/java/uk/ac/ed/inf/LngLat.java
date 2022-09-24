package uk.ac.ed.inf;

public record LngLat(double longitude, double latitude){
    public LngLat(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public boolean inCentralArea(){
        return true;
    }
}
