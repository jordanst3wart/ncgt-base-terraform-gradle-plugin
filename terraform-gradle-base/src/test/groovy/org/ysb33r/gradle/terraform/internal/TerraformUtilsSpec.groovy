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
package org.ysb33r.gradle.terraform.internal

import spock.lang.Specification

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapeOneItem
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedList
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedMap

class TerraformUtilsSpec extends Specification {

    void 'Can escape string'() {
        expect:
        escapeOneItem('123', true) == '"123"'
    }

    void 'Can escape string with quotes'() {
        expect:
        escapeOneItem('1"2"3', true) == '"1\\"2\\"3"'
    }

    void 'Can escape boolean'() {
        expect:
        escapeOneItem(true, true) == 'true'
        escapeOneItem(false, true) == 'false'
    }

    void 'Can escape number'() {
        expect:
        escapeOneItem(123, true) == '123'
    }

    void 'Can escape map'() {
        expect:
        escapedMap([a: 123, b: '123'], true) == '{"a" = 123, "b" = "123"}'
    }

    void 'Can escape list'() {
        expect:
        escapedList([123, '123', true], true) == '[123, "123", true]'
    }

    void 'Can escape collections inside collections'() {
        expect:
        escapedMap([a: [123, '456'], b: [aa: '!@#', bb: '%^&', cc: [true, 890]]], true) ==
            '{"a" = [123, "456"], "b" = {"aa" = "!@#", "bb" = "%^&", "cc" = [true, 890]}}'
        escapedList([123, [a: 'aa'], [true, 'TRUE']], true) == '[123, {"a" = "aa"}, [true, "TRUE"]]'
    }

}