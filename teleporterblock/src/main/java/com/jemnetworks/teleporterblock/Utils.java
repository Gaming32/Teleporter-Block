package com.jemnetworks.teleporterblock;

import java.util.Iterator;

public class Utils {
    public static <E> Iterable<E> toIterable(Iterator<E> iterator) {
        return () -> iterator;
    }
}
