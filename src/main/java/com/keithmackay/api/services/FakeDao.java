package com.keithmackay.api.services;


import com.keithmackay.api.model.SensorData;

import java.sql.Timestamp;
import java.util.List;

public class FakeDao {

  public FakeDao() {
  }

  public SensorData getSensorDataFromVehicleDB() {
    SensorData data = new SensorData();
    data.setMakeAndModel("DMC, DeLorean");
    data.setDestinationYear(2035);
    data.setFluxCapacitorReadings(List.of(37456.3245, 3453.3454, 348765.2343));
    data.setLastCheckIn(new Timestamp(System.currentTimeMillis()));
    data.setSafetyBeltsOn(true);
    return data;
  }
}
