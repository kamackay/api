package com.keithmackay.api.model.mapper;

import com.google.protobuf.Timestamp;
import protos.Sensor;

import java.time.Instant;
import java.util.Optional;

//Class for mapping standard library types, enums, etc to protobuf
public class BaseMapper {

  public static Optional<Timestamp> sqltimeToProtoTime(java.sql.Timestamp sqltime) {
    if (sqltime != null) {
      Instant instant = sqltime.toInstant();
      return Optional.of(Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build());
    }
    return Optional.empty();
  }

  public static java.sql.Timestamp protoTimetoSQLTime(Timestamp tstamp) {
    return java.sql.Timestamp.from(Instant.ofEpochSecond(tstamp.getSeconds(), tstamp.getNanos()));
  }

  /*public Sensor.SensorData toBuffer() {
    Sensor.SensorData.Builder builder = Sensor.SensorData.newBuilder();

    // Set all non-nullable values first
    builder.addAllFluxCapacitorReadings(fluxCapacitorReadings)
        .setDestinationYear(destinationYear)
        .setSafetyBeltsOn(safetyBeltsOn);

    // Demonstrates using Java Optional for proto optionals, in this case lastCheckIn is optional in our proto
    // therefore we should check that lastCheckIn is not null prior to setting the lastCheckIn value on our proto object
    sqltimeToProtoTime(lastCheckIn).ifPresent(builder::setLastCheckIn);

    // Demonstrates null string check for proto optionals, in this case make and model can be null in our proto
    // so it can be assumed it could also be null in our datastore, thus we should check before setting it with our builder.
    if(makeAndModel != null){ builder.setMakeAndModel(makeAndModel); }

    return builder.build();
  }/**/

}
