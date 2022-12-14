/*
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
package org.apache.aries.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.transaction.parsing.TxNamespaceHandler;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.BeanMetadata;

public class AnnotationEnablingNameSpaceHandlerTest extends BaseNameSpaceHandlerSetup {
    
    @Test
    public void testAnnotationEnabled() throws Exception
    {
      ComponentDefinitionRegistry cdr = parseCDR("enable-annotations.xml");
      checkCompTop(cdr);
      BeanMetadata pmd = (BeanMetadata) cdr.getComponentDefinition(TxNamespaceHandler.ANNOTATION_PARSER_BEAN_NAME);
      assertNotNull(pmd);
      assertEquals(3, pmd.getArguments().size());
      assertEquals(cdr, ((PassThroughMetadata)pmd.getArguments().get(0).getValue()).getObject());
//      assertEquals(tm, ((PassThroughMetadata) pmd.getArguments().get(2).getValue()).getObject());
    }
    
    @Test
    public void testAnnotationDisabled() throws Exception
    {
        ComponentDefinitionRegistry cdr = parseCDR("enable-annotations2.xml");
        checkCompTop(cdr);
        BeanMetadata pmd = (BeanMetadata) cdr.getComponentDefinition(TxNamespaceHandler.ANNOTATION_PARSER_BEAN_NAME);
        assertNull(pmd);
    }

    private void checkCompTop(ComponentDefinitionRegistry cdr) {
        BeanMetadata compTop = (BeanMetadata) cdr.getComponentDefinition("top");
        assertNotNull(compTop);
        assertEquals(0, cdr.getInterceptors(compTop).size());
        //assertNull(txenhancer.getComponentMethodTxAttribute(compTop, "increment"));
    }
}
