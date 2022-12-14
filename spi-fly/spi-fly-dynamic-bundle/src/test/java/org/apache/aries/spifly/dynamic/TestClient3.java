/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly.dynamic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.aries.mytest.MySPI;

public class TestClient3 {

    public Set<String> test(String input) {
        Set<String> results = new HashSet<String>();

        MySPI mySPI = EnumLoader.INSTANCE.load(MySPI.class);
        results.add(mySPI.someMethod(input));

        return results;
    }

    public static enum EnumLoader {
        INSTANCE;
        public <S> S load(Class<S> type) {
            Iterator<S> services = ServiceLoader.load(type).iterator();
            return services.hasNext() ? services.next() : null;
        }
    }

}
