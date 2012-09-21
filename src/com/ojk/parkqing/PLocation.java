package com.ojk.parkqing;

public class PLocation {
	private long id;
	private String name;
	private int latitude;
	private int longitude;
	private static final int MULTIPLIER = 10000;

	public PLocation(long id, String name, int lon, int lat) {
		this.id = id;
		this.name = name;
		this.longitude = lon;
		this.latitude = lat;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public static int coordToInt(double val) {
		return (int) (val * MULTIPLIER); 
	}
	
	public static double intToCoord(int val) {
		return ((double) val) / MULTIPLIER;
	}
	
	public double getLongitudeDouble() {
		return intToCoord(longitude);
	}
	
	public int getLongitude() {
		return longitude;
	}

	public void setLongitude(int lon) {
		this.longitude = lon;
	}

	public double getLatitudeDouble() {
		return intToCoord(latitude);
	}
	
	public int getLatitude() {
		return latitude;
	}

	public void setLatitude(int lat) {
		this.latitude = lat;
	}

	public String getName() {
		return name;
	}

	public void setName(String aname) {
		this.name = aname;
	}

	// Will be used by the ArrayAdapter in the ListView
	@Override
	public String toString() {
		return getName() + ": " + "( " + getLongitudeDouble() + ", " + getLatitudeDouble() + " )";
		
	}
	
	public String getCoordString() {
		return getLongitudeDouble() + "," + getLatitudeDouble();
	}
}
