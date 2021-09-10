package se.sensera.banking;

import java.util.Optional;
import java.util.stream.Stream;

public interface Repository<E extends Repository.Entity<I>,I> {
    Optional<E> getEntityById(I id);
    Stream<E> all();
    E save(E entity);
    E delete(E entity);

    interface Entity<I> {
        I getId();
    }
}
