package se.sensera.banking;

import java.util.stream.Stream;

public interface Account extends Repository.Entity<String> {

    String getId();
    User getOwner();
    String getName();
    void setName(String name);
    boolean isActive();
    void setActive(boolean active);

    Stream<User> getUsers();
    void addUser(User user);
    void removeUser(User user);

}
