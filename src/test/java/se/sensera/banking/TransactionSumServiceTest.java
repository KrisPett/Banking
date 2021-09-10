package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.impl.TransactionServiceImpl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static se.sensera.banking.exceptions.HandleException.safe;

public class TransactionSumServiceTest {
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    static User arne = createUser("Arne", "34598798734", true);
    static User lisa = createUser("Lisa", "90098098", true);
    static Account arnesAccount = createAccount(arne, "default", true);
    static Account lisasAccount = createAccount(lisa, "default", true, arne);
    static String ARNES_ID = arne.getId();
    static String LISAS_ID = lisa.getId();
    static String ARNES_KONTO = arnesAccount.getId();
    static String LISAS_KONTO = lisasAccount.getId();

    static Transaction t1 = createTransaction("2020-01-01 10:34", arne, arnesAccount, 200);
    static Transaction t2 = createTransaction("2020-01-01 10:35", lisa, lisasAccount, 250);
    static Transaction t3 = createTransaction("2020-01-01 10:38", arne, arnesAccount, 400);
    static Transaction t4 = createTransaction("2020-01-01 10:39", lisa, lisasAccount, 150);
    static Transaction t5 = createTransaction("2020-01-01 10:40", arne, arnesAccount, 350);

    TransactionService transactionService;

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;
    TransactionsRepository transactionsRepository;

    @BeforeEach
    void setUp() {
        //TODO must be included in create of AccountService
        usersRepository = mock(UsersRepository.class);
        accountsRepository = mock(AccountsRepository.class);
        transactionsRepository = mock(TransactionsRepository.class);

        transactionService = new TransactionServiceImpl(usersRepository, accountsRepository, transactionsRepository); //TODO create Your implementing class here
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void sum_transaction_success(String date, String userId, String accountId, double expectedSum) throws UseException {
        // Given
        when(usersRepository.getEntityById(eq(LISAS_ID))).thenReturn(Optional.of(lisa));
        when(usersRepository.getEntityById(eq(ARNES_ID))).thenReturn(Optional.of(arne));
        when(accountsRepository.getEntityById(ARNES_KONTO)).thenReturn(Optional.of(arnesAccount));
        when(accountsRepository.getEntityById(LISAS_KONTO)).thenReturn(Optional.of(lisasAccount));

        when(transactionsRepository.all()).then(invocation -> Stream.of(
                t1, t2, t3, t4, t5
        ));

        // When
        double sum = transactionService.sum(date, userId, accountId);

        // Then
        assertThat(sum, is(expectedSum));
    }

    @Test
    void sum_transaction_failed_because_not_allowed() {
        // Given
        when(usersRepository.getEntityById(eq(LISAS_ID))).thenReturn(Optional.of(lisa));
        when(usersRepository.getEntityById(eq(ARNES_ID))).thenReturn(Optional.of(arne));
        when(accountsRepository.getEntityById(ARNES_KONTO)).thenReturn(Optional.of(arnesAccount));
        when(accountsRepository.getEntityById(LISAS_KONTO)).thenReturn(Optional.of(lisasAccount));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            transactionService.sum("2020-01-01 10:45", LISAS_ID, ARNES_KONTO);
        });

        // Then
        verify(transactionsRepository, never()).all();
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_ALLOWED));
        assertThat(userException.getActivity(), is(Activity.SUM_TRANSACTION));

    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("2020-01-01 10:39", ARNES_ID, ARNES_KONTO, 600),
                Arguments.of("2020-01-01 10:36", ARNES_ID, ARNES_KONTO, 200),
                Arguments.of("2020-01-01 10:32", ARNES_ID, ARNES_KONTO, 0),
                Arguments.of("2020-01-01 10:45", ARNES_ID, ARNES_KONTO, 950),
                Arguments.of("2020-01-01 10:20", LISAS_ID, LISAS_KONTO, 0),
                Arguments.of("2020-01-01 10:38", LISAS_ID, LISAS_KONTO, 250),
                Arguments.of("2020-01-01 10:59", LISAS_ID, LISAS_KONTO, 400),
                Arguments.of("2020-01-01 10:59", ARNES_ID, LISAS_KONTO, 400)
        );
    }

    public static Transaction createTransaction(String created, User user, Account account, double amount) {
        Date parsedDate = safe(()-> formatter.parse(created), e -> "Cannot parse date '"+created+"'");
        Transaction transaction = mock(Transaction.class);
        when(transaction.getAccount()).thenReturn(account);
        when(transaction.getAmount()).thenReturn(amount);
        when(transaction.getCreated()).thenReturn(parsedDate);
        when(transaction.getUser()).thenReturn(user);
        return transaction;
    }

    private static Account createAccount(User owner, String name, boolean active, User... users) {
        Account account = mock(Account.class);
        String accountId = UUID.randomUUID().toString();
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn(name);
        when(account.getOwner()).thenReturn(owner);
        when(account.isActive()).thenReturn(active);
        when(account.getUsers()).thenReturn(Stream.of(users));

        return account;
    }

    private static User createUser(String name, String pid, boolean active) {
        User user = mock(User.class);
        String userId = UUID.randomUUID().toString();
        when(user.getId()).thenReturn(userId);
        when(user.getPersonalIdentificationNumber()).thenReturn(pid);
        when(user.getName()).thenReturn(name);
        when(user.isActive()).thenReturn(active);

        return user;
    }
}
