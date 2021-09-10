package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.impl.TransactionServiceImpl;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TransactionServiceParallelTest {

    TransactionService transactionService;

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;
    TransactionsRepository transactionsRepository;
    User user;
    User otherUser;
    Account account;
    Account otherAccount;

    @BeforeEach
    void setUp() {
        //TODO must be included in create of AccountService
        usersRepository = new TestUsersRepository();
        accountsRepository = new TestAccountsRepository();
        transactionsRepository = new TestTransactionsRepository();

        transactionService = new TransactionServiceImpl(usersRepository, accountsRepository, transactionsRepository); //TODO create Your implementing class here

        user = createUser("Arne Arnesson", "9283749238472", true);
        otherUser = createUser("Arne Arnesson", "9283749238472", true);
        account = createAccount(user, "default", true);
        otherAccount = createAccount(user, "other", true, otherUser);
    }

    @Test
    void create_parallel_transaction_success() {
        // Given
        String created = "2020-01-01 10:34";
        int count = 1000;
        Object monitorSync = new Object();
        transactionService.addMonitor(waitSync1msec(monitorSync));

        long start = System.currentTimeMillis();
        List<Transaction> transactions = IntStream.range(0, count)
                .boxed()
                .parallel()
                .map(n -> {
                    try {
                        return transactionService.createTransaction(created, user.getId(), account.getId(), (double) n);
                    } catch (UseException e) {
                        throw new RuntimeException("Internal error",e);
                    }
                })
                .collect(Collectors.toList());
        int duration = (int) (System.currentTimeMillis() - start);

        assertThat(transactions.size(), is(count));
        assertThat(transactions, containsInAnyOrder(transactionsRepository.all().toArray(Transaction[]::new)));
        assertThat(duration, is(lessThanOrEqualTo(5000)));
    }

    @Test
    void create_parallel_sum_transaction_success() {
        // Given
        String created = "2020-01-01 10:34";
        int count = 1000;
        Object monitorSync = new Object();
        transactionService.addMonitor(waitSync1msec(monitorSync));

        long start = System.currentTimeMillis();
        long countErrors = IntStream.range(0, count).boxed()
                .parallel()
                .map(n -> createAccount(user, UUID.randomUUID().toString(), true))
                .map(account -> {
                    try {
                        transactionService.createTransaction(created, user.getId(), account.getId(), 100D);
                    } catch (UseException e) {
                        e.printStackTrace();
                    }
                    return Stream.of(100D,-150D)
                            .parallel()
                            .anyMatch(amount -> {
                                try {
                                    transactionService.createTransaction(created, user.getId(), account.getId(), amount);
                                    return amount > 0;
                                } catch (UseException e) {
                                    e.printStackTrace();
                                    return amount < 0;
                                }
                            });
                })
                .filter(ok -> !ok)
                .collect(Collectors.toList())
                .size();
        int duration = (int) (System.currentTimeMillis() - start);

        assertThat(countErrors, is(0L));
        assertThat(duration, is(lessThanOrEqualTo(30000)));
    }


    @Test
    void monitor_created_transaction_success() throws InterruptedException {
        // Given
        String created = "2020-01-01 10:34";
        final int count = 1000;

        List<Transaction> transactions = new LinkedList<>();
        transactionService.addMonitor(transaction -> {
            synchronized (transactions) {
                transactions.add(transaction);
                if (transactions.size() == count)
                    transactions.notifyAll();
            }
        });

        long start = System.currentTimeMillis();
        int countTransactions = (int) IntStream.range(0, count).boxed()
                .map(n -> createAccount(user, UUID.randomUUID().toString(), true))
                .parallel()
                .map(account1 -> {
                    try {
                        return transactionService.createTransaction(created, user.getId(), account1.getId(), 100D);
                    } catch (UseException e) {
                        e.printStackTrace();
                        //throw new RuntimeException("Internal error!",e);
                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .size();
        int duration = (int) (System.currentTimeMillis() - start);

        synchronized (transactions) {
            if (transactions.size() < count)
                transactions.wait(5000);
        }

        assertThat(countTransactions, is(count));
        assertThat(transactions, is(hasSize(count)));
        assertThat(duration, is(lessThanOrEqualTo(5000)));
    }

    private Account createAccount(User owner, String name, boolean active, User... users) {
        Account account = mock(Account.class);
        String accountId = UUID.randomUUID().toString();
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn(name);
        when(account.getOwner()).thenReturn(owner);
        when(account.isActive()).thenReturn(active);
        when(account.getUsers()).then(invocation -> Stream.of(users));
        //when(accountsRepository.getEntityById(accountId)).thenReturn(Optional.of(account));
        accountsRepository.save(account);
        return account;
    }

    private User createUser(String name, String pid, boolean active) {
        User user = mock(User.class);
        String userId = UUID.randomUUID().toString();
        when(user.getId()).thenReturn(userId);
        when(user.getPersonalIdentificationNumber()).thenReturn(pid);
        when(user.getName()).thenReturn(name);
        when(user.isActive()).thenReturn(active);
        //when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        usersRepository.save(user);
        return user;
    }

    private static class TestTransactionsRepository extends AbstractTestRepository<Transaction> implements TransactionsRepository {}
    private static class TestAccountsRepository extends AbstractTestRepository<Account> implements AccountsRepository {}
    private static class TestUsersRepository extends AbstractTestRepository<User> implements UsersRepository {}

    private Consumer<Transaction> waitSync1msec(Object monitorSync) {
        return transaction -> {
            synchronized (monitorSync) {
                try {
                    monitorSync.wait(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    static abstract class AbstractTestRepository<E extends Repository.Entity<String>> implements Repository<E,String> {
        private final List<E> entities = new LinkedList<>();

        @Override
        public Optional<E> getEntityById(String id) {
            //synchronized (entities) {
            return all()
                    .parallel()
                    .filter(transaction -> transaction.getId().equals(id))
                    .findFirst();
            //}
        }

        @Override
        public Stream<E> all() {
            synchronized (entities) {
                return new ArrayList<>(entities).stream();
            }
        }

        @Override
        public E save(E entity) {
            synchronized (entities) {
                entities.add(entity);
                return entity;
            }
        }

        @Override
        public E delete(E entity) {
            synchronized (entities) {
                List<E> tmp = entities.stream()
                        .parallel()
                        .filter(transaction -> !transaction.getId().equals(transaction.getId()))
                        .collect(Collectors.toList());
                entities.clear();
                entities.addAll(tmp);
                return entity;
            }
        }
    }
}
