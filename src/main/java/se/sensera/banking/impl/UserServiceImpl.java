package se.sensera.banking.impl;

import lombok.AllArgsConstructor;
import se.sensera.banking.User;
import se.sensera.banking.UserService;
import se.sensera.banking.UsersRepository;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.utils.ListUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UsersRepository usersRepository;

    @Override
    public User createUser(String name, String personalIdentificationNumber) throws UseException {
        if (checkIfPersonalIdentificationNumberIsUnique(personalIdentificationNumber)) {
            throw new UseException(Activity.CREATE_USER, UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE);
        }
        User user = new UserImpl(UUID.randomUUID().toString(), name, personalIdentificationNumber, true);

        return usersRepository.save(user);
    }

    private boolean checkIfPersonalIdentificationNumberIsUnique(String personalIdentificationNumber) {
        return (usersRepository.all()
                .anyMatch(user -> user.getPersonalIdentificationNumber().equals(personalIdentificationNumber)));
    }

    @Override
    public User changeUser(String userId, Consumer<ChangeUser> changeUser) throws UseException {
        boolean[] ifPersonalIdIsUnique = {true};
        User user = getUserFromUserRepository(userId);

        changeUser.accept(new ChangeUser() {
            @Override
            public void setName(String name) {
                user.setName(name);
            }

            @Override
            public void setPersonalIdentificationNumber(String personalIdentificationNumber) throws UseException {
                if (checkIfPersonalIdentificationNumberIsUnique(personalIdentificationNumber)) {
                    ifPersonalIdIsUnique[0] = false;
                    throw new UseException(Activity.UPDATE_USER, UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE);
                }
                user.setPersonalIdentificationNumber(personalIdentificationNumber);
            }
        });
        if (ifPersonalIdIsUnique[0]) {
            usersRepository.save(user);
        }
        return user;
    }

    private User getUserFromUserRepository(String userId) throws UseException {
        return usersRepository.getEntityById(userId).
                orElseThrow(() -> new UseException(Activity.UPDATE_USER, UseExceptionType.NOT_FOUND));
    }

    @Override
    public User inactivateUser(String userId) throws UseException {
        User user = getUserFromUserRepository(userId);
        user.setActive(false);

        usersRepository.save(user);
        return user;
    }

    @Override
    public Optional<User> getUser(String userId) {
        return usersRepository.getEntityById(userId);
    }

    @Override
    public Stream<User> find(String searchString, Integer pageNumber, Integer pageSize, SortOrder sortOrder) {
        Stream<User> user = ListUtils.applyPage(usersRepository.all(), pageNumber, pageSize)
                .filter(user1 -> user1.getName().toLowerCase().contains(searchString) && user1.isActive());

        switch (sortOrder) {
            case Name -> {
                return user.sorted(Comparator.comparing(User::getName));
            }
            case PersonalId -> {
                return user.sorted(Comparator.comparing(User::getPersonalIdentificationNumber));
            }
        }
        return user;
    }
}