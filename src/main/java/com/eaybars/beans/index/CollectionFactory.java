package com.eaybars.beans.index;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A factory interface to produce backing collections used in the bean indexer
 *
 * @author ertunc
 */
public interface CollectionFactory {

    public Set createNewSet();
    
    public NavigableSet createNewNavigableSet(Comparator<?> c);

    public Map createNewMap();

    public NavigableMap createNewNavigableMap(Comparator<?> c);

    public enum Predefined {

        THREAD_UNSAFE(new CollectionFactory() {

            @Override
            public Set createNewSet() {
                return new HashSet();
            }

            @Override
            public NavigableSet createNewNavigableSet(Comparator<?> c) {
                return new TreeSet(c);
            }
            
            @Override
            public Map createNewMap() {
                return new HashMap();
            }

            @Override
            public NavigableMap createNewNavigableMap(Comparator<?> c) {
                return new TreeMap(c);
            }

        }),
        
        CONCURRENT(new CollectionFactory() {

            @Override
            public Set createNewSet() {
                return Collections.newSetFromMap(new ConcurrentHashMap());
            }

            @Override
            public NavigableSet createNewNavigableSet(Comparator<?> c) {
                return new ConcurrentSkipListSet(c);
            }
            
            @Override
            public Map createNewMap() {
                return new ConcurrentHashMap();
            }

            @Override
            public NavigableMap createNewNavigableMap(Comparator<?> c) {
                return new ConcurrentSkipListMap(c);
            }

        });

        private final CollectionFactory factory;

        private Predefined(CollectionFactory factory) {
            this.factory = factory;
        }

        public CollectionFactory getFactory() {
            return factory;
        }
    }
}
