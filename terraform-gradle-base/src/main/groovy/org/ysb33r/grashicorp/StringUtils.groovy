package org.ysb33r.grashicorp

import groovy.transform.CompileDynamic
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import java.util.concurrent.Callable
import java.util.function.Supplier

class StringUtils {
    static String stringize(final Object stringy) {
        if (stringy == null) {
            throw new Exception('No value available to be converted to a string')
        }

        if (stringy instanceof Callable) {
            stringize(((Callable) stringy).call())
        } else if (stringy instanceof Supplier) {
            stringize((((Supplier) stringy).get()))
        } else if (stringy instanceof Optional) {
            stringize(((Optional) stringy).get())
        } else if (isProvider(stringy)) {
            stringize(getProvided(stringy))
        } else {
            stringy.toString()
        }
    }

    static List<String> stringize(final Iterable<?> stringyThings) {
        List<String> collection = []

        for (Object item in stringyThings) {
            if (isIterableProperty(item)) {
                resolveIterablePropertyTo(collection, item)
            } else {
                switch (item) {
                    case Map:
                        collection.addAll(stringize((Iterable) ((Map) item).values()))
                        break
                    case Iterable:
                        collection.addAll(stringize((Iterable) item))
                        break
                    case Optional:
                        resolveSingleItemOrIterableTo(collection, ((Optional) item).get())
                        break
                    case Supplier:
                        resolveSingleItemOrIterableTo(collection, ((Supplier) item).get())
                        break
                    case Provider:
                        resolveSingleItemOrIterableTo(collection, ((Provider) item).get())
                        break
                    case Callable:
                        resolveSingleItemOrIterableTo(collection, ((Callable) item).call())
                        break
                    default:
                        collection.add(stringize(item))
                }
            }
        }
        collection
    }


    private static boolean isProvider(Object interrogee) {
        interrogee instanceof Provider
    }

    @CompileDynamic
    @SuppressWarnings('UnnecessaryPackageReference')
    private static Object getProvided(Object interrogee) {
        ((Provider) interrogee).get()
    }

    @CompileDynamic
    static private boolean isIterableProperty(Object o) {
        isListProperty(o) || isSetProperty(o)
    }

    @CompileDynamic
    static private boolean isListProperty(Object o) {
        o instanceof ListProperty
    }

    @CompileDynamic
    static private boolean isSetProperty(Object o) {
        o instanceof SetProperty
    }

    @CompileDynamic
    static private void resolveIterablePropertyTo(List<String> strings, Object o) {
        strings.addAll(stringize(o.get()))
    }

    static private void resolveSingleItemOrIterableTo(List<String> strings, Object o) {
        if (o instanceof Iterable) {
            strings.addAll(stringize(o))
        } else {
            strings.addAll(stringize([o]))
        }
    }
}
