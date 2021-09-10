package se.sensera.banking.client;

import se.sensera.banking.*;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.impl.AccountServiceImpl;
import se.sensera.banking.impl.RepositoryFactory;
import se.sensera.banking.impl.TransactionServiceImpl;
import se.sensera.banking.impl.UserServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Scanner;

public class ClientServices {
    private final UsersRepository usersRepository = (UsersRepository) RepositoryFactory.createRepository("UserRepository");
    private final AccountsRepository accountRepository = (AccountsRepository) RepositoryFactory.createRepository("AccountRepository");
    private final TransactionsRepository transactionRepository = (TransactionsRepository) RepositoryFactory.createRepository("TransactionRepository");

    private final UserServiceImpl userService = new UserServiceImpl(usersRepository);
    private final AccountService accountService = new AccountServiceImpl(usersRepository, accountRepository);
    private final TransactionService transactionService = new TransactionServiceImpl(usersRepository, accountRepository, transactionRepository);

    private final Scanner input = new Scanner(System.in);

    public void startScreen() {
        System.out.println("Press 1 to create user:");
        System.out.println("Press 2 to create account:");
        System.out.println("Press 3 to create transaction:");
        System.out.println("Press 4 to quit:");
    }

    public User createUser() throws UseException {
        System.out.println("Type in name");
        String name = input.nextLine();
        System.out.println("Type in personalIdentificationNumber");
        String personalIdentificationNumber = input.nextLine();
        return userService.createUser(name, personalIdentificationNumber);
    }

    public Account createAccount() throws UseException {
        System.out.println("Type in username");
        String username = input.nextLine();
        User user = userService.find(username, 0, 10, UserService.SortOrder.None).findFirst().get();
        System.out.println("Type in account name");
        String accountName = input.nextLine();
        return accountService.createAccount(user.getId(), accountName);
    }

    public Transaction createTransaction() throws UseException {
            System.out.println("Type in username");
            String username = input.nextLine();
            User user = userService.find(username, 0, 10, UserService.SortOrder.None).findFirst().get();
            System.out.println("Type in account name");
            String accountName = input.nextLine();
            Account account = accountService.findAccounts(accountName, user.getId(), 0, 10, AccountService.SortOrder.None).findFirst().get();
            System.out.println("Type in how much you wish to deposit or withdraw");
            int amount = input.nextInt();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return transactionService.createTransaction(formatter.format(LocalDateTime.now()), user.getId(), account.getId(), amount);
    }

    public UsersRepository getUsersRepository() {
        return usersRepository;
    }

    public AccountsRepository getAccountRepository() {
        return accountRepository;
    }

    public TransactionsRepository getTransactionRepository() {
        return transactionRepository;
    }

}
