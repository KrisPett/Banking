package se.sensera.banking.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import se.sensera.banking.User;

@Data
@AllArgsConstructor
public class UserImpl implements User {
    private final String id;
    private String name;
    private String personalIdentificationNumber;
    private boolean active;
}
