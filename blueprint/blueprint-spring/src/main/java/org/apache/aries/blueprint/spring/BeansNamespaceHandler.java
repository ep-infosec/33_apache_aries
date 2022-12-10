/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.spring;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A spring namespace handler to handle spring bean elements.
 */
public class BeansNamespaceHandler implements org.springframework.beans.factory.xml.NamespaceHandler {

    private WeakHashMap<ParserContext, BeanDefinitionReader> readers = new WeakHashMap<ParserContext, BeanDefinitionReader>();

    @Override
    public void init() {
    }

    @Override
    public BeanDefinition parse(Element ele, ParserContext parserContext) {
        BeanDefinitionHolder bdh = getReader(parserContext).parseElement(ele);
        return bdh != null ? bdh.getBeanDefinition() : null;
    }

    @Override
    public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext) {
        return definition;
    }

    private BeanDefinitionReader getReader(ParserContext parserContext) {
        BeanDefinitionReader reader = readers.get(parserContext);
        if (reader == null) {
            reader = new BeanDefinitionReader(parserContext.getReaderContext());
            readers.put(parserContext, reader);
        }
        return reader;
    }

    /**
     * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
     * reads bean definitions according to the "spring-beans" DTD and XSD format
     * (Spring's default XML bean definition format).
     *
     * <p>The structure, elements, and attribute names of the required XML document
     * are hard-coded in this class. (Of course a transform could be run if necessary
     * to produce this format). {@code <beans>} does not need to be the root
     * element of the XML document: this class will parse all bean definition elements
     * in the XML file, regardless of the actual root element.
     *
     * @author Rod Johnson
     * @author Juergen Hoeller
     * @author Rob Harrop
     * @author Erik Wiersma
     * @since 18.12.2003
     */
    public static class BeanDefinitionReader {

        public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

        public static final String NESTED_BEANS_ELEMENT = "beans";

        public static final String ALIAS_ELEMENT = "alias";

        public static final String NAME_ATTRIBUTE = "name";

        public static final String ALIAS_ATTRIBUTE = "alias";

        public static final String IMPORT_ELEMENT = "import";

        public static final String RESOURCE_ATTRIBUTE = "resource";

        public static final String PROFILE_ATTRIBUTE = "profile";

        private final XmlReaderContext readerContext;

        private BeanDefinitionParserDelegate delegate;

        public BeanDefinitionReader(XmlReaderContext readerContext) {
            this.readerContext = readerContext;
        }

        /**
         * Return the descriptor for the XML resource that this parser works on.
         */
        protected final XmlReaderContext getReaderContext() {
            return this.readerContext;
        }

        /**
         * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor} to pull the
         * source metadata from the supplied {@link Element}.
         */
        protected Object extractSource(Element ele) {
            return getReaderContext().extractSource(ele);
        }


        public BeanDefinitionHolder parseElement(Element ele) {
            BeanDefinitionParserDelegate parent = this.delegate;
            this.delegate = createDelegate(getReaderContext(), ele.getOwnerDocument().getDocumentElement(), parent);
            BeanDefinitionHolder bdh = parseDefaultElement(ele, this.delegate);
            this.delegate = parent;
            return bdh;
        }

        /**
         * Register each bean definition within the given root {@code <beans/>} element.
         */
        protected void doRegisterBeanDefinitions(Element root) {
            // Any nested <beans> elements will cause recursion in this method. In
            // order to propagate and preserve <beans> default-* attributes correctly,
            // keep track of the current (parent) delegate, which may be null. Create
            // the new (child) delegate with a reference to the parent for fallback purposes,
            // then ultimately reset this.delegate back to its original (parent) reference.
            // this behavior emulates a stack of delegates without actually necessitating one.
            BeanDefinitionParserDelegate parent = this.delegate;
            this.delegate = createDelegate(getReaderContext(), root, parent);

            if (this.delegate.isDefaultNamespace(root)) {
                String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
                if (StringUtils.hasText(profileSpec)) {
                    String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
                            profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
                    if (!getReaderContext().getReader().getEnvironment().acceptsProfiles(specifiedProfiles)) {
                        return;
                    }
                }
            }

            parseBeanDefinitions(root, this.delegate);

            this.delegate = parent;
        }

        protected BeanDefinitionParserDelegate createDelegate(
                XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate) {

            BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
            delegate.initDefaults(root, parentDelegate);
            return delegate;
        }

        /**
         * Parse the elements at the root level in the document:
         * "import", "alias", "bean".
         * @param root the DOM root element of the document
         */
        protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
            if (delegate.isDefaultNamespace(root)) {
                NodeList nl = root.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    Node node = nl.item(i);
                    if (node instanceof Element) {
                        Element ele = (Element) node;
                        if (delegate.isDefaultNamespace(ele)) {
                            parseDefaultElement(ele, delegate);
                        }
                        else {
                            delegate.parseCustomElement(ele);
                        }
                    }
                }
            }
            else {
                delegate.parseCustomElement(root);
            }
        }

        private BeanDefinitionHolder parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
            BeanDefinitionHolder bdh = null;
            if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
                importBeanDefinitionResource(ele);
            }
            else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
                processAliasRegistration(ele);
            }
            else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
                bdh = processBeanDefinition(ele, delegate);
            }
            else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
                // recurse
                doRegisterBeanDefinitions(ele);
            }
            return bdh;
        }

        /**
         * Parse an "import" element and load the bean definitions
         * from the given resource into the bean factory.
         */
        protected void importBeanDefinitionResource(Element ele) {
            String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
            if (!StringUtils.hasText(location)) {
                getReaderContext().error("Resource location must not be empty", ele);
                return;
            }

            // Resolve system properties: e.g. "${user.dir}"
            location = getReaderContext().getReader().getEnvironment().resolveRequiredPlaceholders(location);

            Set<Resource> actualResources = new LinkedHashSet<Resource>(4);

            // Discover whether the location is an absolute or relative URI
            boolean absoluteLocation = false;
            try {
                absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
            }
            catch (URISyntaxException ex) {
                // cannot convert to an URI, considering the location relative
                // unless it is the well-known Spring prefix "classpath*:"
            }

            // Absolute or relative?
            if (absoluteLocation) {
                try {
                    int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
//                    }
                }
                catch (BeanDefinitionStoreException ex) {
                    getReaderContext().error(
                            "Failed to import bean definitions from URL location [" + location + "]", ele, ex);
                }
            }
            else {
                // No URL -> considering resource location as relative to the current file.
                try {
                    int importCount;
                    Resource relativeResource = getReaderContext().getResource().createRelative(location);
                    if (relativeResource.exists()) {
                        importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
                        actualResources.add(relativeResource);
                    }
                    else {
                        String baseLocation = getReaderContext().getResource().getURL().toString();
                        importCount = getReaderContext().getReader().loadBeanDefinitions(
                                StringUtils.applyRelativePath(baseLocation, location), actualResources);
                    }
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
//                    }
                }
                catch (IOException ex) {
                    getReaderContext().error("Failed to resolve current resource location", ele, ex);
                }
                catch (BeanDefinitionStoreException ex) {
                    getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
                            ele, ex);
                }
            }
            Resource[] actResArray = actualResources.toArray(new Resource[actualResources.size()]);
            getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
        }

        /**
         * Process the given alias element, registering the alias with the registry.
         */
        protected void processAliasRegistration(Element ele) {
            String name = ele.getAttribute(NAME_ATTRIBUTE);
            String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
            boolean valid = true;
            if (!StringUtils.hasText(name)) {
                getReaderContext().error("Name must not be empty", ele);
                valid = false;
            }
            if (!StringUtils.hasText(alias)) {
                getReaderContext().error("Alias must not be empty", ele);
                valid = false;
            }
            if (valid) {
                try {
                    getReaderContext().getRegistry().registerAlias(name, alias);
                }
                catch (Exception ex) {
                    getReaderContext().error("Failed to register alias '" + alias +
                            "' for bean with name '" + name + "'", ele, ex);
                }
                getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
            }
        }

        /**
         * Process the given bean element, parsing the bean definition
         * and registering it with the registry.
         */
        protected BeanDefinitionHolder processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
            BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
            if (bdHolder != null) {
                bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
                try {
                    // Register the final decorated instance.
                    BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
                }
                catch (BeanDefinitionStoreException ex) {
                    getReaderContext().error("Failed to register bean definition with name '" +
                            bdHolder.getBeanName() + "'", ele, ex);
                }
                // Send registration event.
                getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
            }
            return bdHolder;
        }
    }

}