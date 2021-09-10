package se.sensera.banking;

import se.sensera.banking.exceptions.UseException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface UserService {

    User createUser(String name, String personalIdentificationNumber) throws UseException;

    User changeUser(String userId, Consumer<ChangeUser> changeUser) throws UseException;

    User inactivateUser(String userId) throws UseException;

    Optional<User> getUser(String userId);

    Stream<User> find(String searchString, Integer pageNumber, Integer pageSize, SortOrder sortOrder);

    interface ChangeUser {
        void setName(String name);
        void setPersonalIdentificationNumber(String personalIdentificationNumber) throws UseException;
    }

    enum SortOrder {
        None,
        Name,
        PersonalId
    }
}
