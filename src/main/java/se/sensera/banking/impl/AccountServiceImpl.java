package se.sensera.banking.impl;

import lombok.AllArgsConstructor;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.utils.ListUtils;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@AllArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UsersRepository usersRepository;
    private final AccountsRepository accountsRepository;

    @Override
    public Account createAccount(String userId, String accountName) throws UseException {
        User user = getUserFromUserRepository(userId, Activity.CREATE_ACCOUNT);
        Account account = new AccountImpl(UUID.randomUUID().toString(), user, accountName, true);
        checkIfAccountNameIsUnique(accountName);

        return accountsRepository.save(account);
    }

    private void checkIfAccountNameIsUnique(String accountName) throws UseException {
        if (accountsRepository.all()
                .anyMatch(account1 -> account1.getName().equals(accountName))) {
            throw new UseException(Activity.CREATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE);
        }
    }

    @Override
    public Account changeAccount(String userId, String accountId, Consumer<ChangeAccount> changeAccountConsumer) throws UseException {
        boolean[] save = {true};
        Account account = getAccountFromAccountsRepository(accountId, Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_FOUND);

        checkIfAccountBelongsToOwner(userId, account, Activity.UPDATE_ACCOUNT);
        checkIfAccountIsActive(account, Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_ACTIVE);

        changeAccountConsumer.accept(name -> {
            if (accountsRepository.all()
                    .anyMatch(account1 -> account1.getName().equals(name))) {
                save[0] = false;
                throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE);
            }
            if (account.getName().equals(name)) {
                save[0] = false;
            } else {
                account.setName(name);
            }
        });

        if (save[0]) {
            accountsRepository.save(account);
        }
        return account;
    }

    private void checkIfAccountBelongsToOwner(String userId, Account account, Activity activity) throws UseException {
        if (!account.getOwner().getId().equals(userId)) {
            throw new UseException(activity, UseExceptionType.NOT_OWNER);
        }
    }

    private void checkIfAccountIsActive(Account account, Activity activity, UseExceptionType useExceptionType) throws UseException {
        if (!account.isActive()) {
            throw new UseException(activity, useExceptionType);
        }
    }

    private Account getAccountFromAccountsRepository(String accountId, Activity activity, UseExceptionType useExceptionType) throws UseException {
        return accountsRepository.getEntityById(accountId).
                orElseThrow(() -> new UseException(activity, useExceptionType));
    }

    private User getUserFromUserRepository(String userId, Activity activity) throws UseException {
        return usersRepository.getEntityById(userId).
                orElseThrow(() -> new UseException(activity, UseExceptionType.USER_NOT_FOUND));
    }

    @Override
    public Account addUserToAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        User newUser = getUserFromUserRepository(userIdToBeAssigned, Activity.UPDATE_ACCOUNT);
        Account account = getAccountFromAccountsRepository(accountId, Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);

        checkIfAccountIsActive(account, Activity.UPDATE_ACCOUNT, UseExceptionType.ACCOUNT_NOT_ACTIVE);
        checkIfNewAssignedUserIsOwner(userId, newUser.getId());
        checkIfUserIsAssignedToAccount(newUser, account);
        checkIfAccountBelongsToOwner(userId, account, Activity.UPDATE_ACCOUNT);

        account.addUser(newUser);
        return accountsRepository.save(account);
    }

    private void checkIfNewAssignedUserIsOwner(String userId, String userIdToBeAssigned) throws UseException {
        if (userId.equals(userIdToBeAssigned)) {
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.CANNOT_ADD_OWNER_AS_USER);
        }
    }

    private void checkIfUserIsAssignedToAccount(User newUser, Account account) throws UseException {
        if(account.getUsers()
                .anyMatch(user1 -> user1.getId().equals(newUser.getId()))){
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_ALREADY_ASSIGNED_TO_THIS_ACCOUNT);
        }
    }

    private void checkIfUserIsNotAssignedToAccount(String userIdToBeAssigned, Account account) throws UseException {
        if(account.getUsers()
                .noneMatch(user1 -> user1.getId().equals(userIdToBeAssigned))){
            throw new UseException(Activity.UPDATE_ACCOUNT, UseExceptionType.USER_NOT_ASSIGNED_TO_THIS_ACCOUNT);
        }
    }

    @Override
    public Account removeUserFromAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException {
        Account account = getAccountFromAccountsRepository(accountId, Activity.UPDATE_ACCOUNT, UseExceptionType.NOT_FOUND);
        User user = getUserFromUserRepository(userIdToBeAssigned, Activity.UPDATE_ACCOUNT);

        checkIfAccountBelongsToOwner(userId, account, Activity.UPDATE_ACCOUNT);
        checkIfUserIsNotAssignedToAccount(userIdToBeAssigned, account);

        account.removeUser(user);
        return accountsRepository.save(account);
    }

    @Override
    public Account inactivateAccount(String userId, String accountId) throws UseException {
        User user = getUserFromUserRepository(userId, Activity.INACTIVATE_ACCOUNT);
        Account account = getAccountFromAccountsRepository(accountId, Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_FOUND);

        checkIfAccountIsActive(account, Activity.INACTIVATE_ACCOUNT, UseExceptionType.NOT_ACTIVE);
        checkIfAccountBelongsToOwner(user.getId(), account, Activity.INACTIVATE_ACCOUNT);

        account.setActive(false);
        return accountsRepository.save(account);
    }

    @Override
    public Stream<Account> findAccounts(String searchValue, String userId, Integer pageNumber, Integer pageSize, SortOrder sortOrder) throws UseException {
        Stream<Account> account = accountsRepository.all();
        switch (sortOrder) {
            case AccountName -> {
                return accountSortedByName(pageNumber, pageSize, account);
            }
            case None -> {
                return accountMatchedByArgumentValues(searchValue, userId, pageNumber, pageSize, account);
            }
            default -> throw new UseException(Activity.FIND_ACCOUNT, UseExceptionType.NOT_FOUND);
        }
    }

    private Stream<Account> accountSortedByName(Integer pageNumber, Integer pageSize, Stream<Account> account) {
        if (pageNumber != null | pageSize != null) {
            account = account.sorted(Comparator.comparing(Account::getName));
            return ListUtils.applyPage(account, pageNumber, pageSize);
        }
        return account.sorted(Comparator.comparing(Account::getName));
    }

    private Stream<Account> accountMatchedByArgumentValues(String searchValue, String userId, Integer pageNumber, Integer pageSize, Stream<Account> account) {
        if (searchValue.equals("") & userId == null) {
            return ListUtils.applyPage(account, pageNumber, pageSize);
        }
        if (userId != null) {
            return account
                    .filter(account1 -> account1.getOwner().getId().equals(userId) | account1.getUsers()
                            .anyMatch(user -> user.getId().equals(userId)));
        }
        return account.filter(account1 -> account1.getName().toLowerCase().contains(searchValue));
    }
}
