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
  public String username;
  public String firstName;
  public String lastName;
  transient String password;
  public String email;
  public boolean admin;

  @Override
  public String toJson() {
    return new GsonBuilder().create().toJson(this);
  }
}
