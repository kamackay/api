package com.keithmackay.api.model;

import java.io.Serializable;
import java.util.List;
import java.sql.Timestamp;


public class SensorData implements Serializable {

  private String makeAndModel;
  private int destinationYear;
  private List<Double> fluxCapacitorReadings;
  private Timestamp lastCheckIn;
  private boolean safetyBeltsOn;

  public String getMakeAndModel() {
    return makeAndModel;
  }

  public void setMakeAndModel(String makeAndModel) {
    this.makeAndModel = makeAndModel;
  }

  public List<Double> getFluxCapacitorReadings() {
    return fluxCapacitorReadings;
  }

  public void setFluxCapacitorReadings(List<Double> fluxCapacitorReadings) {
    this.fluxCapacitorReadings = fluxCapacitorReadings;
  }

  public int getDestinationYear() {
    return destinationYear;
  }

  public void setDestinationYear(int destinationYear) {
    this.destinationYear = destinationYear;
  }

  public Timestamp getLastCheckIn() {
    return lastCheckIn;
  }

  public void setLastCheckIn(Timestamp lastCheckIn) {
    this.lastCheckIn = lastCheckIn;
  }

  public boolean isSafetyBeltsOn() {
    return safetyBeltsOn;
  }

  public void setSafetyBeltsOn(boolean safetyBeltsOn) {
    this.safetyBeltsOn = safetyBeltsOn;
  }

}
