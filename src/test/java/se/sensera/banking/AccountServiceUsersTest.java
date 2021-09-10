package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.impl.AccountServiceImpl;
import se.sensera.banking.impl.TransactionImpl;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AccountServiceUsersTest {

    AccountService accountService;

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;

    private Account account;
    private String userId;
    private String otherUserId;
    private String accountId;
    private String accountName;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        //TODO must be included in create of AccountService
        usersRepository = mock(UsersRepository.class);
        accountsRepository = mock(AccountsRepository.class);

        accountService = new AccountServiceImpl(usersRepository, accountsRepository); //TODO create Your implementing class here

        userId = UUID.randomUUID().toString();
        otherUserId = UUID.randomUUID().toString();

        user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        otherUser = mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(user.isActive()).thenReturn(true);
        when(otherUser.getId()).thenReturn(otherUserId);
        when(otherUser.isActive()).thenReturn(true);
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(usersRepository.getEntityById(eq(otherUserId))).thenReturn(Optional.of(otherUser));

        accountId = UUID.randomUUID().toString();
        accountName = "default";
        account = mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn(accountName);
        when(account.getOwner()).thenReturn(user);
        when(account.isActive()).thenReturn(true);
        when(account.getUsers()).then(invocation -> Stream.empty());
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));
        when(accountsRepository.all()).thenReturn(Stream.of(account));
    }

    @Test
    void add_user_to_account_success() throws UseException {
        // When
        Account changedAccount = accountService.addUserToAccount(userId, account.getId(), otherUserId);

        // Then
        verify(accountsRepository).save(this.account);
        verify(this.account).addUser(otherUser);
    }

    @Test
    void add_user_to_account_failed_because_user_owner() {
        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.addUserToAccount(userId, account.getId(), userId);
        });

        // Then
        verify(accountsRepository, never()).save(this.account);
        verify(this.account, never()).addUser(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.CANNOT_ADD_OWNER_AS_USER));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void add_user_to_account_failed_because_user_not_owner() {
        // Given
        String userIdToBeAssigned = UUID.randomUUID().toString();
        User userToBeAssigned = mock(User.class);
        when(userToBeAssigned.getId()).thenReturn(userIdToBeAssigned);
        when(userToBeAssigned.isActive()).thenReturn(true);
        when(usersRepository.getEntityById(eq(userIdToBeAssigned))).thenReturn(Optional.of(userToBeAssigned));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.addUserToAccount(otherUserId, account.getId(), userIdToBeAssigned);
        });

        // Then
        verify(accountsRepository, never()).save(this.account);
        verify(this.account, never()).addUser(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_OWNER));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void add_user_to_account_failed_because_account_inactive() {
        // Given
        when(account.isActive()).thenReturn(false);

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.addUserToAccount(userId, account.getId(), userId);
        });

        // Then
        verify(accountsRepository, never()).save(this.account);
        verify(this.account, never()).addUser(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.ACCOUNT_NOT_ACTIVE));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void add_user_to_account_failed_because_account_not_found() {
        // Given
        String unknownAccountId = UUID.randomUUID().toString();
        when(accountsRepository.getEntityById(eq(unknownAccountId))).thenReturn(Optional.empty());

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.addUserToAccount(userId, unknownAccountId, otherUserId);
        });

        // Then
        verify(accountsRepository, never()).save(this.account);
        verify(this.account, never()).addUser(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_FOUND));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void add_user_to_account_failed_because_user_already_assigned_to_account() throws UseException {
        // Given
        when(account.getUsers()).then(invocation -> Stream.of(otherUser));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.addUserToAccount(userId, account.getId(), otherUserId);
        });

        // Then
        verify(accountsRepository, never()).save(this.account);
        verify(this.account, never()).addUser(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.USER_ALREADY_ASSIGNED_TO_THIS_ACCOUNT));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void remove_user_from_account_success() throws UseException {
        // Given
        when(account.getUsers()).thenReturn(Stream.of(otherUser));

        // When
        accountService.removeUserFromAccount(userId, account.getId(), otherUserId);

        // Then
        verify(accountsRepository).save(this.account);
        verify(this.account).removeUser(otherUser);
    }

    @Test
    void remove_user_from_account_failed_because_not_owner() {
        // Given
        String extraUserId = UUID.randomUUID().toString();
        User extraUser = mock(User.class);
        when(extraUser.getId()).thenReturn(extraUserId);
        when(extraUser.isActive()).thenReturn(true);
        when(account.getUsers()).thenReturn(Stream.of(otherUser));
        when(usersRepository.getEntityById(eq(extraUserId))).thenReturn(Optional.of(extraUser));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.removeUserFromAccount(otherUserId, account.getId(), extraUserId);
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_OWNER));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void remove_user_from_account_failed_because_user_not_assigned_to_this_account() {
        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.removeUserFromAccount(userId, account.getId(), otherUserId);
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.USER_NOT_ASSIGNED_TO_THIS_ACCOUNT));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }
}
