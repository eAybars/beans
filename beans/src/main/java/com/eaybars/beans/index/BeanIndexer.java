package com.eaybars.beans.index;

import static com.eaybars.beans.events.EventIntrospector.*;
import static com.eaybars.beans.properties.PropertyIntrospector.*;
import com.eaybars.beans.events.BeanEvent;
import com.eaybars.beans.events.NoSuchEventException;
import com.eaybars.beans.properties.BeanProperty;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * BeanIndexer is used to index bean properties and enable searching on those
 * properties. Properties can be indexed as sorted to enable range search such
 * as lower than or greater than a given value, or can be indexed as unsorted
 * for exact match search use case.
 * <p>
 * Use beanIndexFrom static method to have a BeanIndexer search the given class
 * and automatically add properties as sorted or unsorted indexes according to
 * the declared @Index annotations. BeanIndexer constructed via new operator
 * does not search for @Index annotations.
 *
 * @author Ertunc
 * @param <K>
 */
public class BeanIndexer<K> {

    private Set<K> elements;
    private Class<K> beanClass;
    private Map<String, Map<Object, Set<K>>> index;
    private PropertyListener listener;
    private BeanEvent<PropertyChangeListener> beanEvent;

    /**
     * Create a new BeanIndexer for the given class with no indexed properties
     *
     * @param beanClass
     */
    public BeanIndexer(Class<K> beanClass) {
        elements = new HashSet<K>();
        index = new HashMap<String, Map<Object, Set<K>>>();
        this.beanClass = beanClass;
        try {
            beanEvent = getBeanEvent(beanClass, PropertyChangeListener.class);
            listener = new PropertyListener();
        } catch (NoSuchEventException e) {
        }
    }

    /**
     * Creates a new BeanIndexer by searching the given class and automatically
     * adding properties as sorted or unsorted indexes according to the declared
     *
     * @Index annotations.
     *
     * @param <T> type for the bean to be indexed
     * @param clazz
     * @return
     */
    public static <T> BeanIndexer<T> beanIndexFrom(Class<T> clazz) {
        BeanIndexer<T> instance = new BeanIndexer<T>(clazz);
        instance.scanAndAddIndexes(null, false);
        return instance;
    }

    private void scanAndAddIndexes(BeanProperty parent,
            boolean checkBeforeIndexing) {
        Class<?> clazz = getPropertyClass(parent);
        Index classIndex = clazz.getAnnotation(Index.class);

        if (checkBeforeIndexing ? (classIndex != null && classIndex.contents())
                : true) {
            for (BeanProperty property : getAllProperties(clazz)) {
                if (!property.isWriteOnly()) {
                    Index index = property.getAnnotation(Index.class);
                    if (index == null ? (classIndex != null && !classIndex
                            .contents()) : !index.contents()) {
                        addIndex(parent == null ? property : getProperty(beanClass,
                                parent.toString() + "." + property));
                    } else {
                        scanAndAddIndexes(property, index == null);
                    }
                }
            }
        }
    }

    private BeanIndexer<K> addIndex(BeanProperty property) {
        Index index = property.getAnnotation(Index.class);
        Class<?> clazz = getPropertyClass(property);
        if (index != null) {
            Comparator<?> comparator;
            if (index.sorted() || !Comparator.class.equals(index.comparator())) {
                if (!index.sorted()) {
                    try {
                        comparator = index.comparator().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else if (!isSortable(clazz)) {
                    throw new IllegalArgumentException("Type of the property "
                            + property.toString() + " ("
                            + clazz.getSimpleName() + ") is not sortable");
                } else {
                    comparator = null;
                }
                addSortedIndex(property.toString(), comparator);
            } else {
                addUnsortedIndex(property.toString());
            }
        } else {
            index = clazz.getAnnotation(Index.class);
            if (index == null || !index.sorted() || !isSortable(clazz)) {
                addUnsortedIndex(property.toString());
            } else {
                addSortedIndex(property.toString(), null);
            }
        }
        return this;
    }

    private Class<?> getPropertyClass(BeanProperty parent) {
        return parent == null ? beanClass
                : (parent.isCollectionOrArrayType() ? parent
                .getCollectionOrArrayType() : parent.getPropertyType());
    }

    private boolean isSortable(Class<?> clazz) {
        return clazz.isPrimitive() || Comparable.class.isAssignableFrom(clazz);
    }

    /**
     * Adds a sorted index on the given property to enable range searching on
     * the property
     *
     * @param property
     * @param comparator
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BeanIndexer<K> addSortedIndex(String property,
            Comparator<?> comparator) {
        Map<Object, Set<K>> holder = index.get(property);
        if (holder instanceof NavigableMap<?, ?>) {
            return this;
        }
        if (holder != null) {
            removeIndex(property);
        }
        holder = new TreeMap<Object, Set<K>>((Comparator) comparator);
        index.put(property, holder);
        indexBeans(property, elements);
        return this;

    }

    /**
     * Adds an unsorted index on the given property which can only be used with
     * exact match searching
     *
     * @param property
     * @return
     */
    public BeanIndexer<K> addUnsortedIndex(String property) {
        Map<Object, Set<K>> holder = index.get(property);
        if (holder != null && !(holder instanceof NavigableMap<?, ?>)) {
            return this;
        }
        if (holder != null) {
            removeIndex(property);
        }
        holder = new HashMap<Object, Set<K>>();
        index.put(property, holder);
        indexBeans(property, elements);
        return this;
    }

    /**
     * Removes the previously added sorted or unsorted index for the given
     * property
     *
     * @param property
     * @return
     */
    public BeanIndexer<K> removeIndex(String property) {
        index.remove(property);
        return this;
    }

    /**
     * Returns all elements which are added to this indexer
     *
     * @return
     */
    public Set<K> getElements() {
        return Collections.unmodifiableSet(elements);
    }

    private void indexBeans(String property, Collection<K> elements) {
        Map<Object, Set<K>> map = index.get(property);
        for (K element : elements) {
            indexBean(property, map, element);
        }
    }

    private void indexBean(String property, Map<Object, Set<K>> map, K element) {
        for (Object o : retrievePropertyValueAsCollection(property, element)) {
            addToMap(map, o, element);
        }
    }

    private void addToMap(Map<Object, Set<K>> map, Object key, K element) {
        Set<K> collection = map.get(key);
        if (collection == null) {
            collection = new HashSet<K>();
            map.put(key, collection);
        }
        collection.add(element);
    }

    /**
     * Adds all the elements to this indexer. No action is taken for the already
     * present elements. This method calls add method of this indexer for each
     * element of the given collection
     *
     * @param beans
     * @return
     */
    public BeanIndexer<K> addAll(Collection<K> beans) {
        for (K bean : beans) {
            add(bean);
        }
        return this;
    }

    /**
     * Adds the given element to this indexer. If the given element is already
     * added, no action is taken
     *
     * @param bean
     * @return
     */
    public BeanIndexer<K> add(K bean) {
        if (elements.add(bean)) {
            addListener(bean);
            for (Entry<String, Map<Object, Set<K>>> e : index.entrySet()) {
                indexBean(e.getKey(), e.getValue(), bean);
            }
        }
        return this;
    }

    private void addListener(K bean) {
        if (listener != null) {
            try {
                beanEvent.addListener(bean, listener);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex.getCause());
            }
        }
    }

    /**
     * Removes the given element from the indexer. If no such element exist, no
     * action is taken. After an element is removed from the indexer, all its
     * indexed property values are also removed from the indexer. Therefore this
     * element becomes eligible for garbage collection and will not show up in
     * any search result
     *
     * @param element
     * @return
     */
    public BeanIndexer<K> remove(K element) {
        if (elements.remove(element)) {
            removeListener(element);
            for (Entry<String, Map<Object, Set<K>>> e : index.entrySet()) {
                for (Object value : retrievePropertyValueAsCollection(
                        e.getKey(), element)) {
                    e.getValue().get(value).remove(element);
                }
            }
        }
        return this;
    }

    private void removeListener(K bean) {
        if (listener != null) {
            try {
                beanEvent.removeListener(bean, listener);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex.getCause());
            }
        }
    }

    /**
     * Calls the remove method of this indexer for each element of the given
     * collection
     *
     * @param elements
     * @return
     */
    public BeanIndexer<K> removeAll(Collection<K> elements) {
        for (K element : elements) {
            remove(element);
        }
        return this;
    }

    /**
     * Removes all previously added elements from this indexer.
     *
     * @return
     */
    public BeanIndexer<K> clear() {
        for (Map<Object, Set<K>> map : index.values()) {
            map.clear();
        }

        if (listener != null) {
            for (K element : elements) {
                removeListener(element);
            }
        }
        elements.clear();
        return this;
    }

    /**
     * Returns a Filter which can be used for searching this indexer.
     *
     * @return
     */
    public Filter filter() {
        return new Search().or();
    }

    private Collection<?> retrievePropertyValueAsCollection(String property,
            K element) {
        Object value = retrievePropertyValue(property, element);
        return value instanceof Collection<?> ? (Collection<?>) value : Arrays
                .asList(value);
    }

    /**
     * Provides a set of values gathered from the added elements for the given
     * property
     *
     * @param property
     * @return
     * @throws IllegalArgumentException
     */
    public Set<Object> propertyValues(String property)
            throws IllegalArgumentException {
        Map<Object, Set<K>> values = index.get(property);
        if (values == null) {
            throw new IllegalArgumentException("No such indexed field: "
                    + property);
        }
        return Collections.unmodifiableSet(values.keySet());
    }
    
    public Set<String> getAllIndexes() {
        return Collections.unmodifiableSet(index.keySet());
    }

    /**
     * Called by the indexer to retrieve the value of an indexed property. Sub
     * classes of indexer may override this method to integrate indexer to
     * different environments like Expression Language
     *
     * @param property
     * @param element
     * @return
     */
    protected Object retrievePropertyValue(String property, K element) {
        BeanProperty bProperty = getProperty(beanClass, property);
        try {
            return bProperty.getValue(element);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private interface SearchState<K> {

        public void add(Set<K> elements);

        public void add(Collection<Set<K>> elements);
    }

    public class Search {

        private Set<K> result;
        private Filter filter;
        private SearchState<K> state;
        @SuppressWarnings("rawtypes")
        private SearchState[] states = {new AndSearchState(),
            new OrSearchState()};

        public Search() {
            this(new HashSet<K>());
            filter = new Filter(this);
        }

        public Search(Set<K> resultSet) {
            result = resultSet;
        }

        @SuppressWarnings("unchecked")
        public Filter and() {
            this.state = states[0];
            return filter;
        }

        @SuppressWarnings("unchecked")
        public Filter or() {
            this.state = states[1];
            return filter;
        }

        /**
         * Transforms the current search result to its inverse. For example if
         * indexer holds 3 elements 'a', 'b' and 'c', and the current result of
         * the search is ['a'], inverse will transform the current result of
         * this search to ['b', 'c']
         *
         * @return
         */
        public Search inverse() {
            HashSet<K> inverse = new HashSet<K>(elements);
            inverse.removeAll(result);
            result = inverse;
            return this;
        }

        /**
         * Retrieves the results of this search
         *
         * @return
         */
        public Set<K> results() {
            return result;
        }

        /**
         * Retrieves a single result from this search. This method should only
         * be invoked if you are sure that the search contains exactly one
         * result.
         *
         * @return
         * @throws NoSuchElementException if this search contains no results
         * @throws IllegalStateException if this search contains more than one
         * result
         */
        public K singleResult() throws NoSuchElementException,
                IllegalStateException {
            if (result.size() > 1) {
                throw new IllegalStateException("There are " + result.size()
                        + " results in this search");
            } else {
                return result.iterator().next();// to cause
                // NoSuchElementException
                // if there are no
                // results
            }
        }

        /**
         * Retrieves the results as a sorted set, comparing the elements
         * according to their natural ordering. (i.e. if element class
         * implements Comparable<K>)
         *
         * @return
         */
        public NavigableSet<K> sortedResults() {
            return new TreeSet<K>(result);
        }

        /**
         * Retrieves the results as a sorted set, comparing the elements
         * according to the gicen comparator
         *
         * @param comparator
         * @return
         */
        public NavigableSet<K> sortedResults(Comparator<K> comparator) {
            NavigableSet<K> sorteResult = new TreeSet<K>(comparator);
            sorteResult.addAll(result);
            return sorteResult;
        }

        protected Search add(Set<K> elements) {
            state.add(elements);
            return this;
        }

        protected Search add(Collection<Set<K>> elements) {
            state.add(elements);
            return this;
        }

        private class AndSearchState implements SearchState<K> {

            @Override
            public void add(Set<K> elements) {
                result.retainAll(elements);
            }

            @Override
            public void add(Collection<Set<K>> elements) {
                HashSet<K> all = new HashSet<K>();
                for (Set<K> e : elements) {
                    all.addAll(e);
                }
                add(all);
            }

        }

        private class OrSearchState implements SearchState<K> {

            @Override
            public void add(Set<K> elements) {
                result.addAll(elements);
            }

            @Override
            public void add(Collection<Set<K>> elements) {
                for (Set<K> e : elements) {
                    add(e);
                }
            }
        }
    }

    public class Filter {

        protected Search search;

        public Filter(Search search) {
            this.search = search;
        }

        /**
         * Searches for an exact match
         *
         * @param property
         * @param value
         * @return
         * @throws IllegalArgumentException if no such index exists
         */
        public Search having(String property, Object value)
                throws IllegalArgumentException {
            return search.add(havingResultSet(property, value));
        }

        /**
         * Searches for elements which does not exactly match the given property
         * and value
         *
         * @param property
         * @param value
         * @return
         * @throws IllegalArgumentException if no such index exists
         */
        public Search notHaving(String property, Object value)
                throws IllegalArgumentException {
            Set<K> notHaving = new HashSet<K>(getElements());
            notHaving.removeAll(havingResultSet(property, value));
            return search.add(notHaving);
        }

        /**
         * Searches for an exact match on any element in the supplied set
         *
         * @param property
         * @param values
         * @return
         * @throws IllegalArgumentException
         */
        public Search in(String property, Set<? extends Object> values)
                throws IllegalArgumentException {
            return search.add(inResultSet(property, values));
        }

        /**
         * Searches for elements whose property value is not in the supplied set
         *
         * @param property
         * @param values
         * @return
         * @throws IllegalArgumentException
         */
        public Search notIn(String property, Set<? extends Object> values)
                throws IllegalArgumentException {
            Set<K> notIn = new HashSet<K>(getElements());
            notIn.removeAll(inResultSet(property, values));
            return search.add(notIn);
        }

        private Set<K> inResultSet(String property, Set<? extends Object> values) {
            Set<K> set = new HashSet<K>();
            for (Object v : values) {
                set.addAll(havingResultSet(property, v));
            }
            return set;
        }

        private Set<K> havingResultSet(String property, Object value) {
            Map<Object, Set<K>> map = index.get(property);
            if (map == null) {
                throw new IllegalArgumentException("No such indexed field: "
                        + property);
            }
            Set<K> result = map.get(value);
            return result == null ? Collections.<K>emptySet() : result;
        }

        /**
         * Range search for sorted indexes only
         *
         * @param property
         * @param value
         * @param inclusive
         * @return
         * @throws IllegalArgumentException if no such sorted index exists
         */
        public Search havingGreater(String property, Object value,
                boolean inclusive) throws IllegalArgumentException {
            return search.add(findMap(property).tailMap(value, inclusive)
                    .values());
        }

        /**
         * Range search for sorted indexes only
         *
         * @param property
         * @param value
         * @param inclusive
         * @return
         * @throws IllegalArgumentException if no such sorted index exists
         */
        public Search havingLower(String property, Object value,
                boolean inclusive) throws IllegalArgumentException {
            return search.add(findMap(property).headMap(value, inclusive)
                    .values());
        }

        private NavigableMap<Object, Set<K>> findMap(String property) {
            NavigableMap<Object, Set<K>> map;
            try {
                map = (NavigableMap<Object, Set<K>>) index.get(property);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(
                        "No such sorted-indexed field: " + property);
            }
            if (map == null) {
                throw new IllegalArgumentException("No such indexed field: "
                        + property);
            }
            return map;
        }

    }

    private class PropertyListener implements PropertyChangeListener {

        @SuppressWarnings("unchecked")
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Map<Object, Set<K>> map = index.get(evt.getPropertyName());
            if (map != null) {// ensure this is an index property
                map.get(evt.getOldValue()).remove(evt.getSource());
                addToMap(map, evt.getNewValue(), (K) evt.getSource());
            }
        }
    }
}
