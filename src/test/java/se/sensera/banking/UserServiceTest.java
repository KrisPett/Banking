package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sensera.banking.exceptions.Activity;
import se.sensera.banking.exceptions.UseException;
import se.sensera.banking.exceptions.UseExceptionType;
import se.sensera.banking.impl.UserServiceImpl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    UserService userService;
    UsersRepository usersRepository;
    private String userId;
    private User user;

    @BeforeEach
    void setUp() {
        //TODO Måste skickas med som en parameter i UserService constructor
        usersRepository = mock(UsersRepository.class);

        userService = new UserServiceImpl(usersRepository);

        userId = UUID.randomUUID().toString();
        user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("Arne Gunnarsson");
        when(user.getPersonalIdentificationNumber()).thenReturn("20011010-1234");
        when(user.isActive()).thenReturn(true);
    }

    @Test
    void create_user_success() throws UseException {
        // Given
        when(usersRepository.all()).thenReturn(Stream.empty());
        when(usersRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        // When
        User user = userService.createUser("Arne Gunnarsson", "20011010-1234");

        // Then
        verify(usersRepository).save(user);
        assertThat(user.getId(), is(notNullValue()));
        assertThat(user.getName(), is("Arne Gunnarsson"));
        assertThat(user.getPersonalIdentificationNumber(), is("20011010-1234"));
        assertThat(user.isActive(), is(true));
    }

    @Test
    void create_user_fail_because_not_unique() {
        // Given
        User user = mock(User.class);
        when(user.getPersonalIdentificationNumber()).thenReturn("20011010-1234");
        when(usersRepository.all()).thenReturn(Stream.of(user));

        // when
        UseException userException = assertThrows(UseException.class, () -> {
            userService.createUser("Arne Gunnarsson", "20011010-1234");
        });

        // Then
        verify(usersRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE));
        assertThat(userException.getActivity(), is(Activity.CREATE_USER));
    }

    @Test
    void update_name_success() throws UseException {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(usersRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        // when
        userService.changeUser(userId, changeUser -> changeUser.setName("Arne Andersson"));

        // Then
        verify(usersRepository).save(user);
        verify(user).setName("Arne Andersson");
        verify(user, never()).setPersonalIdentificationNumber(anyString());
    }

    @Test
    void update_personal_id_success() throws UseException {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(usersRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);
        when(usersRepository.all()).thenReturn(Stream.empty());

        // when
        userService.changeUser(userId, changeUser -> {
            try {
                changeUser.setPersonalIdentificationNumber("20011010-0234");
            } catch (UseException e) {
                throw new RuntimeException("Test failed", e);
            }
        });

        // Then
        verify(usersRepository).save(user);
        verify(user).setPersonalIdentificationNumber("20011010-0234");
        verify(user, never()).setName(anyString());
    }

    @Test
    void update_personal_id_fail_because_user_not_found() throws UseException {
        // Given
        when(usersRepository.getEntityById(anyString())).thenReturn(Optional.empty());

        // when
        UseException userException = assertThrows(UseException.class, () -> {
            userService.changeUser(UUID.randomUUID().toString(), changeUser -> {
                try {
                    changeUser.setPersonalIdentificationNumber("20011010-0234");
                } catch (UseException e) {
                    throw new RuntimeException("Failed!");
                }
            });
        });

        // Then
        verify(usersRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_FOUND));
        assertThat(userException.getActivity(), is(Activity.UPDATE_USER));
    }

    @Test
    void update_personal_id_fail_because_not_unique_id() throws UseException {
        // Given
        String lisaUserId = UUID.randomUUID().toString();
        User lisa = mock(User.class);
        when(lisa.getPersonalIdentificationNumber()).thenReturn("20011010-0234");
        when(lisa.getId()).thenReturn(lisaUserId);
        when(lisa.getName()).thenReturn("Lisa Gunnarsson");
        when(usersRepository.all()).thenReturn(Stream.of(user, lisa));
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));

        // when
        AtomicReference<UseException> idUserException = new AtomicReference<>();
        userService.changeUser(userId, changeUser -> {
            try {
                changeUser.setPersonalIdentificationNumber("20011010-0234");
            } catch (UseException e) {
                idUserException.set(e);
            }
        });

        // Then
        verify(usersRepository, never()).save(anyObject());
        assertThat(idUserException.get().getUserExceptionType(), is(UseExceptionType.USER_PERSONAL_ID_NOT_UNIQUE));
        assertThat(idUserException.get().getActivity(), is(Activity.UPDATE_USER));
    }

    @Test
    void get_user_by_id_success() {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));

        // when
        Optional<User> userOptional = userService.getUser(userId);

        // Then
        assertThat(userOptional.isPresent(), is(true));
        User user = userOptional.get();
        assertThat(user.getId(), is(notNullValue()));
        assertThat(user.getName(), is("Arne Gunnarsson"));
        assertThat(user.getPersonalIdentificationNumber(), is("20011010-1234"));
        assertThat(user.isActive(), is(true));
    }

    @Test
    void get_user_by_id_fail_user_id_not_exist() {
        // Given
        when(usersRepository.getEntityById(anyString())).thenReturn(Optional.empty());

        // when
        Optional<User> userOptional = userService.getUser(UUID.randomUUID().toString());

        // Then
        assertThat(userOptional.isPresent(), is(false));
    }

    @Test
    void inactivate_user_success() throws UseException {
        // Given
        when(usersRepository.getEntityById(eq(userId))).thenReturn(Optional.of(user));
        when(usersRepository.save(anyObject())).then(invocation -> invocation.getArguments()[0]);

        // when
        userService.inactivateUser(userId);

        // Then
        verify(usersRepository).save(user);
        verify(user).setActive(false);
    }

    @Test
    void inactivate_user_failed_because_not_found() throws UseException {
        // Given
        when(usersRepository.getEntityById(anyString())).thenReturn(Optional.empty());

        // when
        UseException userException = assertThrows(UseException.class, () -> {
            userService.inactivateUser(UUID.randomUUID().toString());
        });

        // Then
        verify(usersRepository, never()).save(anyObject());
        assertThat(userException.getUserExceptionType(), is(UseExceptionType.NOT_FOUND));
        assertThat(userException.getActivity(), is(Activity.UPDATE_USER));
    }
}
