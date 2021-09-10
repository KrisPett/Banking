package se.sensera.banking.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ListUtils {

    static <E> Stream<E> applyPage(Stream<E> stream, Integer pageNumber, Integer pageSize) {
        if (pageNumber != null) {
            List<E> intermediateList = stream.collect(Collectors.toList());
            if (pageSize == null)
                try {
                    return intermediateList
                            .subList(pageNumber, intermediateList.size())
                            .stream();
                } catch (Exception e) {
                    return Stream.empty();
                }
            else
                try {
                    int fromIndex = pageNumber * pageSize;
                    int toIndex = pageNumber > 0 ? pageNumber * (pageSize + 1) : pageSize;
                    if (toIndex > intermediateList.size())
                        toIndex = intermediateList.size();
                    return intermediateList
                            .subList(fromIndex, toIndex)
                            .stream();
                } catch (Exception e) {
                    return Stream.empty();
                }
        } else if (pageSize != null)
            return stream.limit(pageSize);
        return stream;
    }
}
