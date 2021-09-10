package se.sensera.banking.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ListUtilsTest {

    private List<Integer> list;

    @BeforeEach
    void setUp() {
        list = Stream.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).collect(Collectors.toList());
    }

    @Test
    void ingen_page_success() {
        // When
        List<Integer> rs = ListUtils.applyPage(list.stream(), null, null).collect(Collectors.toList());

        assertThat(rs, containsInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    void pageNumber_utan_pageSize_success() {
        // When
        List<Integer> rs = ListUtils.applyPage(list.stream(), 4, null).collect(Collectors.toList());

        assertThat(rs, containsInAnyOrder(4, 5, 6, 7, 8, 9));
    }

    @Test
    void pageNumber_med_pageSize_success() {
        // When
        List<Integer> rs = ListUtils.applyPage(list.stream(), 2, 2).collect(Collectors.toList());

        assertThat(rs, containsInAnyOrder(4, 5));
    }

    @Test
    void utan_pageNumber_med_pageSize_success() {
        // When
        List<Integer> rs = ListUtils.applyPage(list.stream(), null, 5).collect(Collectors.toList());

        assertThat(rs, containsInAnyOrder(0, 1, 2, 3, 4));
    }

    @Test
    void zero_pageNumber_med_pageSize_success() {
        // When
        List<Integer> rs = ListUtils.applyPage(list.stream(), 0, 5).collect(Collectors.toList());

        assertThat(rs, containsInAnyOrder(0, 1, 2, 3, 4));
    }

    @Test
    void pageNumber_med_pageSize_utanför_success() {
        // When
        List<Integer> rs = ListUtils.applyPage(list.stream(), 4, 10).collect(Collectors.toList());

        assertThat(rs, is(empty()));
    }

}