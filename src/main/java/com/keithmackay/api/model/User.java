package com.keithmackay.api.model;

import com.google.gson.GsonBuilder;
import com.keithmackay.api.db.JsonObj;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements JsonObj<User> {
    public String username;
    public String firstName;
    public String lastName;
    public String email;
    public boolean admin;
    transient String password;

    @Override
    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }

    @Override
    public User fromJson(final Document doc) {
        return builder()
                .username(doc.getString("username"))
                .firstName(doc.getString("firstName"))
                .lastName(doc.getString("lastName"))
                .email(doc.getString("email"))
                .admin(doc.getBoolean("admin"))
                .password(doc.getString("password"))
                .build();
    }
}
