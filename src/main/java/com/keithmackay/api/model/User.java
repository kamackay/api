package com.keithmackay.api.model;

import com.google.gson.GsonBuilder;
import com.keithmackay.api.db.JsonObj;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements JsonObj {
  String username;
  String firstName;
  String lastName;
  transient String password;

  @Override
  public String toJson() {
    return new GsonBuilder().create().toJson(this);
  }
}
