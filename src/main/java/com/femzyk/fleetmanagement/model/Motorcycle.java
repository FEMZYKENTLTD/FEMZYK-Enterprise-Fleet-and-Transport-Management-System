package com.femzyk.fleetmanagement.model;

/** Motorcycle. Type attributes retained from the original system: wheels and style. */
public class Motorcycle extends Vehicle {

    private int numberOfWheels = 2;
    private String style = "STANDARD"; // SPORT, CRUISER, OFF_ROAD, STANDARD

    @Override public VehicleType getVehicleType() { return VehicleType.MOTORCYCLE; }
    @Override public String getKeyAttribute() { return numberOfWheels + " wheels, " + style.toLowerCase().replace('_', '-'); }

    public int getNumberOfWheels() { return numberOfWheels; }
    public void setNumberOfWheels(int numberOfWheels) { this.numberOfWheels = numberOfWheels; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    @Override public Double getAttributeNumber() { return (double) numberOfWheels; }
    @Override public void setAttributeNumber(Double value) { if (value != null) numberOfWheels = value.intValue(); }
    @Override public String getAttributeText() { return style; }
    @Override public void setAttributeText(String value) { if (value != null) style = value; }
}
