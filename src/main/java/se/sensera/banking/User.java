package se.sensera.banking;

public interface User extends Repository.Entity<String> {
    String getId();

    String getName();

    void setName(String name);

    String getPersonalIdentificationNumber();

    void setPersonalIdentificationNumber(String pid);

    boolean isActive();

    void setActive(boolean active);
}
