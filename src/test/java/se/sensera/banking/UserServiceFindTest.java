package se.sensera.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sensera.banking.impl.UserServiceImpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserServiceFindTest {

    UserService userService;
    UsersRepository usersRepository;

    private User arne_gunnarsson;
    private User lisa_gunnarsson;
    private User beda_gunnarsson;
    private User per_andersson;
    private User bruno_pettersson;
    private User jason_mars;

    User[] usersInAnyOrder;
    User[] usersSortedByName;
    User[] usersSortedById;

    @BeforeEach
    void setUp() {
        //TODO Måste skickas med som en parameter i UserService constructor
        usersRepository = mock(UsersRepository.class);

        userService = new UserServiceImpl(usersRepository); //TODO create Your implementing class here

        arne_gunnarsson = createUser("Arne Gunnarsson", "20011010-1234", true);
        lisa_gunnarsson = createUser("Lisa  Gunnarsson", "20011010-0234", true);
        beda_gunnarsson = createUser("Beda Gunnarsson", "20011010-2234", true);
        per_andersson = createUser("Per Andersson", "20011010-4234", true);
        bruno_pettersson = createUser("Bruno Pettersson", "20011010-5234", true);
        jason_mars = createUser("Jason Mars", "20011010-6234", true);

        when(usersRepository.all()).thenReturn(Stream.of(
                arne_gunnarsson,
                lisa_gunnarsson,
                beda_gunnarsson,
                per_andersson,
                bruno_pettersson,
                jason_mars
        ));

        usersInAnyOrder = new User[]{
                arne_gunnarsson,
                lisa_gunnarsson,
                beda_gunnarsson,
                per_andersson,
                bruno_pettersson,
                jason_mars
        };

        usersSortedByName = new User[]{
                arne_gunnarsson,
                beda_gunnarsson,
                bruno_pettersson,
                jason_mars,
                lisa_gunnarsson,
                per_andersson,
        };

        usersSortedById = new User[]{
                lisa_gunnarsson,
                arne_gunnarsson,
                beda_gunnarsson,
                per_andersson,
                bruno_pettersson,
                jason_mars
        };
    }

    @Test
    void find_all_users_success() {
        // When
        Stream<User> rs = userService.find("", null, null, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), containsInAnyOrder(usersInAnyOrder));
    }

    @Test
    void find_users_by_name_success() {
        // When
        Stream<User> rs = userService.find("gunnarsson", null, null, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), containsInAnyOrder(arne_gunnarsson, lisa_gunnarsson, beda_gunnarsson));
    }

    @Test
    void find_users_by_name_success_with_page_number() {
        // When
        Stream<User> rs = userService.find("gunnarsson", 0, null, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), containsInAnyOrder(arne_gunnarsson, lisa_gunnarsson, beda_gunnarsson));
    }

    @Test
    void find_users_by_name_success_with_page_number_and_size() {
        // When
        Stream<User> rs = userService.find("gunnarsson", 0, 10, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), containsInAnyOrder(
                arne_gunnarsson,
                lisa_gunnarsson,
                beda_gunnarsson));
    }

    @Test
    void find_all_users_sorted_by_name() {
        // When
        Stream<User> rs = userService.find("", null, null, UserService.SortOrder.Name);

        // Then
        assertThat(rs.collect(Collectors.toList()), contains(usersSortedByName));
    }

    @Test
    void find_all_users_sorted_by_id() {
        // When
        Stream<User> rs = userService.find("", null, null, UserService.SortOrder.PersonalId);

        // Then
        assertThat(rs.collect(Collectors.toList()), contains(usersSortedById));
    }

    @Test
    void find_users_by_name_fail_because_no_result() {
        // When
        Stream<User> rs = userService.find("XYZ", null, null, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), is(empty()));
    }

    @Test
    void find_users_by_name_fail_because_page_number_to_high() {
        // When
        Stream<User> rs = userService.find("", 2, 10, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), is(empty()));
    }

    @Test
    void find_users_by_name_do_not_show_inactivated_users_success() {
        // Given
        when(bruno_pettersson.isActive()).thenReturn(false);
        when(arne_gunnarsson.isActive()).thenReturn(false);

        // When
        Stream<User> rs = userService.find("", null, null, UserService.SortOrder.None);

        // Then
        assertThat(rs.collect(Collectors.toList()), containsInAnyOrder(
                lisa_gunnarsson,
                beda_gunnarsson,
                per_andersson,
                jason_mars
        ));
    }

    private User createUser(String name, String pid, boolean active) {
        User user = mock(User.class);
        String userId = UUID.randomUUID().toString();
        when(user.getId()).thenReturn(userId);
        when(user.getPersonalIdentificationNumber()).thenReturn(pid);
        when(user.getName()).thenReturn(name);
        when(user.isActive()).thenReturn(active);
        return user;
    }
}
