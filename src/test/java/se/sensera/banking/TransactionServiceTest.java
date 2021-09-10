package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.impl.TransactionServiceImpl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static se.sensera.banking.exceptions.HandleException.safe;

public class TransactionServiceTest {

    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

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
        usersRepository = mock(UsersRepository.class);
        accountsRepository = mock(AccountsRepository.class);
        transactionsRepository = mock(TransactionsRepository.class);

        transactionService = new TransactionServiceImpl(usersRepository, accountsRepository, transactionsRepository); //TODO create Your implementing class here

        user = createUser("Arne Arnesson", "9283749238472", true);
        otherUser = createUser("Arne Arnesson", "9283749238472", true);
        account = createAccount(user, "default", true);
        otherAccount = createAccount(user, "other", true, otherUser);

        Transaction transaction = mock(Transaction.class);
        when(transaction.getAccount()).thenReturn(account);
        when(transaction.getAmount()).thenReturn(200D);
        when(transaction.getCreated()).thenReturn(safe(()-> formatter.parse("2020-01-01 10:32"), e -> "Cannot parse date '2020-01-01 10:32'"));
        when(transaction.getUser()).thenReturn(user);

        when(transactionsRepository.all()).thenReturn(Stream.of(transaction));
    }

    @Test
    void create_transaction_success() throws UseException, ParseException {
        // Given
        String created = "2020-01-01 10:34";
        double amount = 100;
        when(transactionsRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        Transaction transaction = transactionService.createTransaction(created, user.getId(), account.getId(), amount);

        verify(transactionsRepository).save(transaction);

        assertThat(transaction.getId(), is(notNullValue()));
        assertThat(transaction.getCreated(), is(formatter.parse(created)));
        assertThat(transaction.getUser(), is(user));
        assertThat(transaction.getAccount(), is(account));
        assertThat(transaction.getAmount(), is(amount));
    }

    @Test
    void create_withdrawal_transaction_success() throws UseException, ParseException {
        // Given
        String created = "2020-01-01 10:34";
        double amount = -100;
        when(transactionsRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        Transaction transaction = transactionService.createTransaction(created, user.getId(), account.getId(), amount);

        verify(transactionsRepository).save(transaction);

        assertThat(transaction.getId(), is(notNullValue()));
        assertThat(transaction.getUser(), is(user));
        assertThat(transaction.getAccount(), is(account));
        assertThat(transaction.getAmount(), is(amount));
    }

    @Test
    void create_transaction_by_other_success() throws UseException, ParseException {
        // Given
        String created = "2020-01-01 10:34";
        double amount = 100;
        when(transactionsRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        Transaction transaction = transactionService.createTransaction(created, otherUser.getId(), otherAccount.getId(), amount);

        verify(transactionsRepository).save(transaction);

        assertThat(transaction.getId(), is(notNullValue()));
        assertThat(transaction.getCreated(), is(formatter.parse(created)));
        assertThat(transaction.getUser(), is(otherUser));
        assertThat(transaction.getAccount(), is(otherAccount));
        assertThat(transaction.getAmount(), is(amount));
    }

    @Test
    void create_transaction_failed_because_not_owner_or_user() {
        // Given
        String created = "2020-01-01 10:34";
        double amount = 100;

        UseException userException = assertThrows(UseException.class, () -> {
            transactionService.createTransaction(created, otherUser.getId(), account.getId(), amount);
        });

        verify(transactionsRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_ALLOWED));
        assertThat(userException.getActivity(), is(Activity.CREATE_TRANSACTION));
    }

    @Test
    void create_transaction_failed_because_not_enough_founds_at_account() {
        // Given
        String created = "2020-01-01 10:34";
        double amount = -300;

        UseException userException = assertThrows(UseException.class, () -> {
            transactionService.createTransaction(created, user.getId(), account.getId(), amount);
        });

        verify(transactionsRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_FUNDED));
        assertThat(userException.getActivity(), is(Activity.CREATE_TRANSACTION));
    }

    private Account createAccount(User owner, String name, boolean active, User... users) {
        Account account = mock(Account.class);
        String accountId = UUID.randomUUID().toString();
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn(name);
        when(account.getOwner()).thenReturn(owner);
        when(account.isActive()).thenReturn(active);
        when(account.getUsers()).then(invocation -> Stream.of(users));
        when(accountsRepository.getEntityById(accountId)).thenReturn(Optional.of(account));
        return account;
    }

    private User createUser(String name, String pid, boolean active) {
        User user = mock(User.class);
        String userId = UUID.randomUUID().toString();
        when(user.getId()).thenReturn(userId);
        when(user.getPersonalIdentificationNumber()).thenReturn(pid);
        when(user.getName()).thenReturn(name);
        when(user.isActive()).thenReturn(active);
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        return user;
    }


}
