package com.eaybars.beans.index;

import static com.eaybars.beans.events.EventIntrospector.*;
import static com.eaybars.beans.properties.PropertyIntrospector.*;
import com.eaybars.beans.events.BeanEvent;
import com.eaybars.beans.events.NoSuchEventException;
import com.eaybars.beans.properties.BeanProperty;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;

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
public class BeanIndexer<K> extends AbstractSet<K> implements Set<K> {

    private CollectionFactory factory;
    private Class<K> beanClass;
    private Set<K> elements;
    private Map<String, Map<Object, Set<K>>> index;
    private PropertyListener listener;
    private BeanEvent<PropertyChangeListener> beanEvent;

    /**
     * Create a new thread unsafe BeanIndexer for the given class with no
     * indexed properties
     *
     * @param beanClass
     */
    public BeanIndexer(Class<K> beanClass) {
        this(beanClass, CollectionFactory.Predefined.THREAD_UNSAFE.getFactory());
    }

    /**
     * Creates a new BeanIndexer which has no index and uses the given factory
     * to generate its backing collections
     *
     * @param beanClass
     * @param factory
     */
    public BeanIndexer(Class<K> beanClass, CollectionFactory factory) {
        this.beanClass = beanClass;
        this.factory = factory;
        elements = factory.createNewSet();
        index = factory.createNewMap();
        try {
            beanEvent = getBeanEvent(beanClass, PropertyChangeListener.class);
            listener = new PropertyListener();
        } catch (NoSuchEventException e) {
        }
    }

    /**
     * Creates a new tread unsafe BeanIndexer by searching the given class and
     * automatically adding properties as sorted or unsorted indexes according
     * to the annotations.
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
        return beanIndexFrom(clazz, CollectionFactory.Predefined.THREAD_UNSAFE.getFactory());
    }

    /**
     * Creates a new BeanIndexer with the given factory by searching the given
     * class and automatically adding properties as sorted or unsorted indexes
     * according to the annotations
     *
     * @param <T>
     * @param clazz
     * @param factory
     * @return
     */
    public static <T> BeanIndexer<T> beanIndexFrom(Class<T> clazz, CollectionFactory factory) {
        BeanIndexer<T> instance = new BeanIndexer<T>(clazz, factory);
        instance.scanAndAddIndexes(null, false);
        return instance;
    }

    protected void scanAndAddIndexes(BeanProperty parent,
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
     * the property. If a sorted index already exists, no action is taken. If an
     * unsorted index already exist, an IllegalStateException is thrown
     *
     * @param property
     * @param comparator
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public BeanIndexer<K> addSortedIndex(String property,
            Comparator<?> comparator) {
        Map<Object, Set<K>> holder;
        synchronized (this) {
            holder = index.get(property);
            if (holder == null) {
                index.put(property, factory.createNewNavigableMap(comparator));
            }
        }
        if (holder != null) {
            if (holder instanceof NavigableMap<?, ?>) {
                return this;
            } else {
                throw new IllegalStateException("An unsorted index for the property already exists: "
                        + property);
            }
        }
        indexBeans(property, elements);
        return this;

    }

    /**
     * Adds an unsorted index on the given property which can only be used with
     * exact match searching. If an unsorted index already exists, no action is
     * taken. If a sorted index already exist, an IllegalStateException is
     * thrown
     *
     * @param property
     * @return
     */
    public BeanIndexer<K> addUnsortedIndex(String property) {
        Map<Object, Set<K>> holder;
        synchronized (this) {
            holder = index.get(property);
            if (holder == null) {
                index.put(property, factory.createNewMap());
            }
        }
        if (holder != null) {
            if (!(holder instanceof NavigableMap<?, ?>)) {
                return this;
            } else {
                throw new IllegalStateException("A sorted index for the property already exists: "
                        + property);
            }
        }
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
            collection = factory.createNewSet();
            map.put(key, collection);
        }
        collection.add(element);
    }

    @Override
    public boolean add(K bean) {
        if (elements.add(bean)) {
            addListener(bean);
            for (Entry<String, Map<Object, Set<K>>> e : index.entrySet()) {
                indexBean(e.getKey(), e.getValue(), bean);
            }
            return true;
        }
        return false;
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

    @Override
    public boolean remove(Object element) {
        if (elements.remove(element)) {
            removeImpl(element);
            return true;
        }
        return false;
    }

    private void removeImpl(Object element) {
        removeListener(element);
        for (Entry<String, Map<Object, Set<K>>> e : index.entrySet()) {
            for (Object value : retrievePropertyValueAsCollection(
                    e.getKey(), element)) {
                Set<K> elementSet = e.getValue().get(value);
                if (elementSet != null) {//if no more element exists for the given index value 
                    //or element has been modified after indexing is done
                    elementSet.remove(element);
                    if (elementSet.isEmpty()) {
                        e.getValue().remove(value);
                    }
                }
            }
        }
    }

    private void removeListener(Object bean) {
        if (listener != null) {
            try {
                beanEvent.removeListener(bean, listener);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex.getCause());
            }
        }
    }

    @Override
    public void clear() {
        for (Map<Object, Set<K>> map : index.values()) {
            map.clear();
        }

        if (listener != null) {
            for (K element : elements) {
                removeListener(element);
            }
        }
        elements.clear();
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
            Object element) {
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
    public Set<Object> getAllValuesForProperty(String property)
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
    protected Object retrievePropertyValue(String property, Object element) {
        BeanProperty bProperty = getProperty(beanClass, property);
        try {
            return bProperty.getValue(element);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public int size() {
        return this.elements.size();
    }

    @Override
    public boolean contains(Object o) {
        return this.elements.contains(o);
    }

    @Override
    public Iterator<K> iterator() {
        return new Iterator() {
            Iterator<K> delegate = elements.iterator();
            K lastElement = null;

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public K next() {
                return lastElement = delegate.next();
            }

            @Override
            public void remove() {
                delegate.remove();
                removeImpl(lastElement);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return elements.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return elements.containsAll(c);
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
            this(factory.createNewSet());
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
            Set<K> inverse = factory.createNewSet();
            inverse.addAll(elements);
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
            NavigableSet<K> set = factory.createNewNavigableSet(null);
            set.addAll(result);
            return set;
        }

        /**
         * Retrieves the results as a sorted set, comparing the elements
         * according to the given comparator
         *
         * @param comparator
         * @return
         */
        public NavigableSet<K> sortedResults(Comparator<K> comparator) {
            NavigableSet<K> sorteResult = factory.createNewNavigableSet(comparator);
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
                Set<K> all = factory.createNewSet();
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
            Set<K> notHaving = factory.createNewSet();
            notHaving.addAll(BeanIndexer.this);
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
            Set<K> notIn = factory.createNewSet();
            notIn.addAll(BeanIndexer.this);
            notIn.removeAll(inResultSet(property, values));
            return search.add(notIn);
        }

        private Set<K> inResultSet(String property, Set<? extends Object> values) {
            Set<K> set = factory.createNewSet();
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
