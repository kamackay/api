package com.keithmackay.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(builderClassName = "Builder")
@AllArgsConstructor
@NoArgsConstructor
public class GroceryItem {
  String name;
  Integer count;
  String list;
  String addedBy;
  long addedAt;
  boolean removed;
}
