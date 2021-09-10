package se.sensera.banking.impl;

import lombok.SneakyThrows;
import se.sensera.banking.*;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.HandleException;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class TransactionServiceImpl implements TransactionService {
    private final UsersRepository usersRepository;
    private final AccountsRepository accountsRepository;
    private final TransactionsRepository transactionsRepository;
    private final List<Consumer<Transaction>> listMonitor = new LinkedList<>();

    public TransactionServiceImpl(UsersRepository usersRepository, AccountsRepository accountsRepository, TransactionsRepository transactionsRepository) {
        this.usersRepository = usersRepository;
        this.accountsRepository = accountsRepository;
        this.transactionsRepository = transactionsRepository;
    }

    @Override
    public Transaction createTransaction(String created, String userId, String accountId, double amount) throws UseException {
        User user = getUserFromUserRepository(userId);
        Account account = getAccountFromAccountsRepository(accountId);
        Date date = formatStringToDate(created);
        checkIfUserIsOwnerOfAccount(userId, user, account);
        checkIfFundsIsEnoughForAccount(date, accountId, amount);

        Transaction transaction = new TransactionImpl(UUID.randomUUID().toString(), date, user, account, amount);
        listMonitor.forEach(transactionConsumer -> transactionConsumer.accept(transaction));

        return transactionsRepository.save(transaction);
    }

    private void checkIfFundsIsEnoughForAccount(Date date, String accountId, double amount) throws UseException {
        //Around 200 millis faster
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            if (executorService.submit(() -> countSum(date, accountId) + amount < 0).get()) {
                throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_FUNDED);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    //Performance boost
    public double countSum(Date date, String accountId) {
        return transactionsRepository.all()
                .filter(transaction -> transaction.getAccount().getId().equals(accountId) && (transaction.getCreated().before(date) || transaction.getCreated().equals(date)))
                .mapToDouble(Transaction::getAmount).sum();
    }

    private void checkIfUserIsOwnerOfAccount(String userId, User user, Account account) throws UseException {
        if (!account.getOwner().equals(user) &
                account.getUsers().noneMatch(user1 -> user1.getId().equals(userId))) {
            throw new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.NOT_ALLOWED);
        }
    }

    private Date formatStringToDate(String created) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(created, formatter);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private User getUserFromUserRepository(String userId) throws UseException {
        return usersRepository.getEntityById(userId).
                orElseThrow(() -> new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.USER_NOT_FOUND));
    }

    private Account getAccountFromAccountsRepository(String accountId) throws UseException {
        return accountsRepository.getEntityById(accountId).
                orElseThrow(() -> new UseException(Activity.CREATE_TRANSACTION, UseExceptionType.ACCOUNT_NOT_FOUND));
    }

    @Override
    public double sum(String created, String userId, String accountId) throws UseException {
        Account account = getAccountFromAccountsRepository(accountId);
        checkIfUsersExistInAccount(userId, account);
        Date date = formatStringToDate(created);

        return countSum(date, accountId);
    }

    private void checkIfUsersExistInAccount(String userId, Account account) throws UseException {
        if (!account.getOwner().getId().equals(userId)
                && account.getUsers().noneMatch(user -> user.getId().equals(userId))) {
            throw new UseException(Activity.SUM_TRANSACTION, UseExceptionType.NOT_ALLOWED);
        }
    }

    @Override
    public void addMonitor(Consumer<Transaction> monitor) {
        listMonitor.add(monitor);
    }
}

