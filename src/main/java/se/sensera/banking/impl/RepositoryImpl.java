package se.sensera.banking.impl;

import se.sensera.banking.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryImpl<E extends Repository.Entity<String>> implements Repository<E, String> {
    List<E> entities = new ArrayList<>();

    @Override
    public Optional<E> getEntityById(String id) {
        return all()
                .parallel()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    @Override
    public Stream<E> all() {
        return new ArrayList<>(entities).stream();
    }

    @Override
    public E save(E entity) {
        entities.add(entity);
        return entity;
    }

    @Override
    public E delete(E entity) {
        List<E> tmp = entities.stream()
                .parallel()
                .filter(e -> !e.getId().equals(e.getId()))
                .collect(Collectors.toList());
        entities.clear();
        entities.addAll(tmp);
        return entity;
    }

    public static class UsersRepositoryImpl extends RepositoryImpl<User> implements UsersRepository{}

    public static class AccountsRepositoryImpl extends RepositoryImpl<Account> implements AccountsRepository {}

    public static class TransactionsRepositoryImpl extends RepositoryImpl<Transaction> implements TransactionsRepository {}
}
