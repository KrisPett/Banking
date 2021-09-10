package se.sensera.banking;

import se.sensera.banking.exceptions.UseException;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface AccountService {

    Account createAccount(String userId, String accountName) throws UseException;
    Account changeAccount(String userId, String accountId, Consumer<ChangeAccount> changeAccountConsumer) throws UseException;

    Account addUserToAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException;
    Account removeUserFromAccount(String userId, String accountId, String userIdToBeAssigned) throws UseException;

    Account inactivateAccount(String userId, String accountId) throws UseException;

    Stream<Account> findAccounts(String searchValue, String userId, Integer pageNumber, Integer pageSize, SortOrder sortOrder) throws UseException;

    interface ChangeAccount {
        void setName(String name) throws UseException;
    }

    enum SortOrder {
        None,
        AccountName,
    }

}
