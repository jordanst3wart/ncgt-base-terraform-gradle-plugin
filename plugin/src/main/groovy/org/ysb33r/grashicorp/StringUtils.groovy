/*
 * Copyright 2017-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.grashicorp

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import java.util.concurrent.Callable
import java.util.function.Supplier

@CompileStatic
class StringUtils {
    static String stringize(final Object stringy) {
        if (stringy == null) {
            throw new IllegalArgumentException('No value available to be converted to a string')
        }

        switch (stringy) {
            case Callable:
                return stringize(((Callable) stringy).call())
            case Supplier:
                return stringize(((Supplier) stringy).get())
            case Optional:
                return stringize(((Optional) stringy).get())
            case { isProvider(it) }:
                return stringize(getProvided(stringy))
            default:
                return stringy.toString()
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
