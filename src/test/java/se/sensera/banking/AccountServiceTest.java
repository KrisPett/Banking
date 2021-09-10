package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.impl.AccountServiceImpl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AccountServiceTest {

    AccountService accountService;

    UsersRepository usersRepository;
    AccountsRepository accountsRepository;

    Account account;
    String accountId;
    String accountName;

    String userId;
    String otherUserId;
    User user;
    User otherUser;

    @BeforeEach
    void setUp() {
        //TODO must be included in create of AccountService
        usersRepository = mock(UsersRepository.class);
        accountsRepository = mock(AccountsRepository.class);

        accountService = new AccountServiceImpl(usersRepository, accountsRepository); //TODO create Your implementing class here

        user = mock(User.class);
        userId = UUID.randomUUID().toString();

        otherUser = mock(User.class);
        otherUserId = UUID.randomUUID().toString();

        when(user.getId()).thenReturn(userId);
        when(user.isActive()).thenReturn(true);
        when(otherUser.getId()).thenReturn(otherUserId);
        when(otherUser.isActive()).thenReturn(true);
        when(usersRepository.getEntityById(anyString())).thenReturn(Optional.empty());

        account = mock(Account.class);
        accountId = UUID.randomUUID().toString();
        accountName = "default";
        when(account.getId()).thenReturn(accountId);
        when(account.getName()).thenReturn(accountName);
        when(account.getOwner()).thenReturn(user);
        when(account.isActive()).thenReturn(true);
        when(account.getUsers()).thenReturn(Stream.empty());
        when(accountsRepository.getEntityById(anyString())).thenReturn(Optional.empty());
        when(accountsRepository.all()).thenReturn(Stream.empty());
    }

    @Test
    void create_account_success() throws UseException {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));

        when(accountsRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        // When
        Account account = accountService.createAccount(userId, accountName);

        // Then
        verify(accountsRepository).save(account);
        assertThat(account.getOwner().getId(), is(userId));
        assertThat(account.getName(), is(accountName));
        assertThat(account.isActive(), is(true));
        assertThat(account.getUsers().collect(Collectors.toList()), is(empty()));
    }

    @Test
    void create_account_failed_because_user_not_found() {
        // when
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.createAccount(UUID.randomUUID().toString(), accountName);
        });

        // Then
        verify(accountsRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.USER_NOT_FOUND));
        assertThat(userException.getActivity(), is(Activity.CREATE_ACCOUNT));
    }

    @Test
    void create_account_failed_because_duplicate_name_for_owner_user() {
        // Given
        Account otherAccount = mock(Account.class);
        when(otherAccount.getOwner()).thenReturn(user);
        when(otherAccount.getName()).thenReturn(accountName);
        when(accountsRepository.all()).thenReturn(Stream.of(otherAccount));
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));

        // when
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.createAccount(userId, accountName);
        });

        // Then
        verify(accountsRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE));
        assertThat(userException.getActivity(), is(Activity.CREATE_ACCOUNT));
    }

    @Test
    void change_account_name_success() throws UseException {
        // Given
        String otherAccountName = "other";
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));
        when(accountsRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        // When
        Account account = accountService.changeAccount(userId, accountId, changeAccount -> {
            try {
                changeAccount.setName(otherAccountName);
            } catch (UseException e) {
                throw new RuntimeException("Name change failed", e);
            }
        });

        // Then
        verify(accountsRepository).save(account);
        verify(account).setName(otherAccountName);
        assertThat(account.getOwner().getId(), is(userId));
        assertThat(account.getName(), is(accountName));
        assertThat(account.isActive(), is(true));
        assertThat(account.getUsers().collect(Collectors.toList()), is(empty()));
    }

    @Test
    void change_account_name_to_same_name_success() throws UseException {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        Account account = accountService.changeAccount(userId, accountId, changeAccount -> {
            try {
                changeAccount.setName(accountName);
            } catch (UseException e) {
                throw new RuntimeException("Name change failed", e);
            }
        });

        // Then
        verify(accountsRepository, never()).save(anyObject());
        verify(this.account, never()).setName(anyString());
    }

    @Test
    void change_account_name_failed_because_duplicate_name() throws UseException {
        // Given
        String otherAccountName = "other";
        Account otherAccount = mock(Account.class);
        when(otherAccount.getOwner()).thenReturn(user);
        when(otherAccount.getName()).thenReturn(otherAccountName);
        when(accountsRepository.all()).thenReturn(Stream.of(this.account, otherAccount));
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        AtomicReference<UseException> userException = new AtomicReference<>();
        accountService.changeAccount(userId, accountId, changeAccount -> {
            try {
                changeAccount.setName(otherAccountName);
            } catch (UseException e) {
                userException.set(e);
            }
        });

        // Then
        verify(accountsRepository, never()).save(anyObject());
        assertThat(userException.get().getUserExceptionType(), is(UseExceptionType.ACCOUNT_NAME_NOT_UNIQUE));
        assertThat(userException.get().getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void change_account_name_failed_because_not_owner() {
        // Given
        String otherAccountName = "other";
        Account otherAccount = mock(Account.class);
        when(otherAccount.getOwner()).thenReturn(user);
        when(accountsRepository.all()).thenReturn(Stream.of(this.account, otherAccount));
        when(usersRepository.getEntityById(eq(otherUserId))).thenReturn(Optional.of(otherUser));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.changeAccount(otherUserId, accountId, changeAccount -> {
                try {
                    changeAccount.setName(otherAccountName);
                } catch (UseException e) {
                    throw new RuntimeException("Name change failed", e);
                }
            });
        });

        // Then
        verify(accountsRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_OWNER));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void change_account_name_failed_because_account_inactive() {
        // Given
        String otherAccountName = "other";
        when(accountsRepository.all()).thenReturn(Stream.of(account));
        when(account.isActive()).thenReturn(false);
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.changeAccount(userId, accountId, changeAccount -> {
                try {
                    changeAccount.setName(otherAccountName);
                } catch (UseException e) {
                    throw new RuntimeException("Name change failed", e);
                }
            });
        });

        // Then
        verify(accountsRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_ACTIVE));
        assertThat(userException.getActivity(), is(Activity.UPDATE_ACCOUNT));
    }

    @Test
    void inactivate_account_success() throws UseException {
        // Given
        when(accountsRepository.all()).thenReturn(Stream.of(account));
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));
        when(accountsRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        // When
        Account account = accountService.inactivateAccount(userId, accountId);

        // Then
        verify(accountsRepository).save(this.account);
        verify(account).setActive(false);
    }

    @Test
    void inactivate_account_failed_because_not_found() {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.inactivateAccount(userId, UUID.randomUUID().toString());
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_FOUND));
        assertThat(userException.getActivity(), is(Activity.INACTIVATE_ACCOUNT));
    }

    @Test
    void inactivate_account_failed_because_user_not_found() {
        // Given
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.inactivateAccount(UUID.randomUUID().toString(), accountId);
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.USER_NOT_FOUND));
        assertThat(userException.getActivity(), is(Activity.INACTIVATE_ACCOUNT));
    }

    @Test
    void inactivate_account_failed_because_not_owner() {
        // Given
        when(accountsRepository.all()).thenReturn(Stream.of(account));
        when(usersRepository.getEntityById(eq(otherUserId))).thenReturn(Optional.of(otherUser));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.inactivateAccount(otherUserId, accountId);
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_OWNER));
        assertThat(userException.getActivity(), is(Activity.INACTIVATE_ACCOUNT));
    }

    @Test
    void inactivate_account_failed_because_not_active() {
        // Given
        when(accountsRepository.all()).thenReturn(Stream.of(account));
        when(account.isActive()).thenReturn(false);
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.inactivateAccount(userId, accountId);
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_ACTIVE));
        assertThat(userException.getActivity(), is(Activity.INACTIVATE_ACCOUNT));
    }

    @Test
    void inactivate_account_failed_because_user_not_active() {
        // Given
        when(accountsRepository.all()).thenReturn(Stream.of(account));
        when(account.isActive()).thenReturn(false);
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(false);
        when(accountsRepository.getEntityById(eq(accountId))).thenReturn(Optional.of(account));

        // When
        UseException userException = assertThrows(UseException.class, () -> {
            accountService.inactivateAccount(userId, accountId);
        });

        // Then
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_ACTIVE));
        assertThat(userException.getActivity(), is(Activity.INACTIVATE_ACCOUNT));
    }
}
