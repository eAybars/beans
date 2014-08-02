package com.eaybars.beans.index;

import com.eaybars.beans.IndexedBean1;
import com.eaybars.beans.IndexedBean2;
import com.eaybars.beans.MutableBean;
import com.eaybars.beans.ReadOrWriteOnlyBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Ertunc
 */
public class BeanIndexerTest {

    private BeanIndexer<IndexedBean1> b1;
    private BeanIndexer<IndexedBean2> b2;

    @Before
    public void setUp() {
	b1 = BeanIndexer.beanIndexFrom(IndexedBean1.class)
		.add(new IndexedBean1("abc", 1))
		.add(new IndexedBean1("abc", 2))
		.add(new IndexedBean1("xyz", 1))
		.add(new IndexedBean1("xyz", 2))
		.add(new IndexedBean1("qwerty", 8));
	b2 = BeanIndexer
		.beanIndexFrom(IndexedBean2.class)
		.add(new IndexedBean2("b1").addBean(new IndexedBean1("abc", 1))
			.addBean(new IndexedBean1("abc", 2)))
		.add(new IndexedBean2("b2").addBean(new IndexedBean1("xyz", 1))
			.addBean(new IndexedBean1("xyz", 2)))
		.add(new IndexedBean2("b3").addBean(new IndexedBean1("qwr", 8)));
    }

    @Test
    public void simpleSearchTest() {
	Collection<IndexedBean1> simpleResult = b1.filter()
		.having("name", "abc").results();
	assertEquals(2, simpleResult.size());
	assertTrue(simpleResult.contains(new IndexedBean1("abc", 1)));
	assertTrue(simpleResult.contains(new IndexedBean1("abc", 2)));

	simpleResult = b1.filter().having("number", 8).results();
	assertEquals(1, simpleResult.size());
	assertTrue(simpleResult.contains(new IndexedBean1("qwerty", 8)));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void noIndexExceptionTest() {
	b1.filter().having("noName", "abc");
    }

    @Test
    public void searchTest() {
	Collection<IndexedBean1> searchResult = b1.filter()
		.having("name", "abc").and().having("number", 1).results();
	assertEquals(1, searchResult.size());
	assertTrue(searchResult.contains(new IndexedBean1("abc", 1)));

	searchResult = b1.filter().having("name", "abc").and()
		.having("number", 1).inverse().results();
	assertEquals(4, searchResult.size());
	assertFalse(searchResult.contains(new IndexedBean1("abc", 1)));

	searchResult = b1.filter().having("name", "abc").and()
		.having("number", 1).or().having("number", 8).results();
	assertEquals(2, searchResult.size());
	assertTrue(searchResult.contains(new IndexedBean1("abc", 1)));
	assertTrue(searchResult.contains(new IndexedBean1("qwerty", 8)));
    }

    @Test
    public void rangeSearchTest() {
	Collection<IndexedBean1> rangeResult = b1.filter()
		.havingGreater("number", 2, true).and()
		.havingLower("number", 8, false).results();
	assertEquals(2, rangeResult.size());
	assertTrue(rangeResult.contains(new IndexedBean1("abc", 2)));
	assertTrue(rangeResult.contains(new IndexedBean1("xyz", 2)));

	rangeResult = b1.filter().having("name", "abc").and()
		.havingGreater("number", 1, false).results();
	assertEquals(1, rangeResult.size());
	assertTrue(rangeResult.contains(new IndexedBean1("abc", 2)));

	rangeResult = b1.filter().havingGreater("number", 1, false).and()
		.having("name", "abc").results();
	assertEquals(1, rangeResult.size());
	assertTrue(rangeResult.contains(new IndexedBean1("abc", 2)));
    }

    @Test
    public void sortedResultTest() {
	NavigableSet<IndexedBean1> set = b1.filter().having("name", "abc")
		.sortedResults(new Comparator<IndexedBean1>() {
		    @Override
		    public int compare(IndexedBean1 o1, IndexedBean1 o2) {
			return o2.getNumber() - o1.getNumber(); // descending
								// ordering by
								// number
		    }
		});
	assertEquals(2, set.size());
	Iterator<IndexedBean1> iterator = set.iterator();
	assertEquals(2, iterator.next().getNumber());
	assertEquals(1, iterator.next().getNumber());
    }

    @Test
    public void singleResultTest() {
	IndexedBean1 result = b1.filter().having("name", "abc").and()
		.having("number", 1).singleResult();
	assertEquals(new IndexedBean1("abc", 1), result);
    }

    @Test(expected = NoSuchElementException.class)
    public void singleNoResultTest() {
	b1.filter().having("name", "abc").and().having("number", 3)
		.singleResult();
    }

    @Test(expected = IllegalStateException.class)
    public void singleResultMultipleElementTest() {
	b1.filter().having("name", "abc").singleResult();
    }

    @Test
    public void notHavingTest() {
	IndexedBean1 result = b1.filter().having("name", "abc").and()
		.notHaving("number", 1).singleResult();
	assertEquals(new IndexedBean1("abc", 2), result);

	result = b1.filter().notHaving("number", 1).and()
		.notHaving("number", 2).singleResult();
	assertEquals(new IndexedBean1("qwerty", 8), result);
    }

    @Test
    public void removeTest() {
	b1.remove(new IndexedBean1("abc", 2));
	Collection<IndexedBean1> result = b1.filter().having("name", "abc")
		.results();

	assertEquals(1, result.size());
	assertTrue(result.contains(new IndexedBean1("abc", 1)));
    }
    
    @Test
    public void removeAllTest() {
        b1.removeAll(Arrays.asList(new IndexedBean1("abc", 2), 
                new IndexedBean1("abc", 1)));
        
        assertEquals(3, b1.getElements().size());
        assertEquals(Collections.emptySet(), b1.filter().having("name", "abc")
		.results());
    }
    
    @Test
    public void clearTest() {
        b1.clear();
        assertEquals(Collections.emptySet(), b1.getElements());
        assertEquals(Collections.emptySet(), b1.filter().having("name", "abc")
		.results());
    }
    
    @Test
    public void addLaterTest() {
        b1.add(new IndexedBean1("later", 0));
        
        Set<IndexedBean1> results = b1.filter().having("name", "later").results();
        assertEquals(1, results.size());
        assertTrue(results.contains(new IndexedBean1("later", 0)));
    }

    @Test
    public void simpleTestForCollectionBean() {
	Collection<IndexedBean2> result = b2.filter().having("name", "b2")
		.results();
	assertEquals(1, result.size());
	assertEquals("b2", result.iterator().next().getName());

	result = b2.filter().having("beans.name", "abc").results();
	assertEquals(1, result.size());
	assertEquals("b1", result.iterator().next().getName());

	result = b2.filter().having("beans.name", "abc").and()
		.having("beans.number", 8).results();
	assertTrue(result.isEmpty());

	result = b2.filter().havingGreater("beans.number", 2, false).results();
	assertEquals(1, result.size());
	assertEquals("b3", result.iterator().next().getName());
    }

    @Test
    public void beanWithNoAnnotationTest() {
	BeanIndexer<MutableBean> indexer = new BeanIndexer<MutableBean>(
		MutableBean.class);
	indexer.addUnsortedIndex("address");
	indexer.addSortedIndex("quantity", null);

	MutableBean b1 = new MutableBean();
	b1.setAddress("Some address");
	b1.setQuantity(2);

	MutableBean b2 = new MutableBean();
	b2.setAddress("Some other address");
	b2.setQuantity(3);

	indexer.add(b1).add(b2);

	Set<MutableBean> result = indexer.filter().having("quantity", 2)
		.results();
	assertEquals(1, result.size());
	assertTrue(result.contains(b1));
        
        b1.setQuantity(4);
        result = indexer.filter().having("quantity", 2)
		.results();
        assertEquals(Collections.emptySet(), result);
        
        result = indexer.filter().having("quantity", 4)
		.results();
	assertEquals(1, result.size());
	assertTrue(result.contains(b1));
    }

    @Test
    public void observerTest() {
	BeanIndexer<MutableBean> indexer = new BeanIndexer<MutableBean>(
		MutableBean.class);
	indexer.addUnsortedIndex("address");
	indexer.addSortedIndex("quantity", null);

	MutableBean b1 = new MutableBean();
	b1.setAddress("Some address");
	b1.setQuantity(2);

	MutableBean b2 = new MutableBean();
	b2.setAddress("Some other address");
	b2.setQuantity(3);

	indexer.add(b1).add(b2);

	b1.setQuantity(1);

	Set<MutableBean> result = indexer.filter().having("quantity", 2)
		.results();
	assertTrue(result.isEmpty());

	result = indexer.filter().having("quantity", 1).results();
	assertEquals(1, result.size());
	assertTrue(result.contains(b1));
    }

    @Test
    public void inTest() {
	Set<IndexedBean1> result = b1.filter()
		.in("name", new HashSet<Object>(Arrays.asList("abc", "xyz")))
		.results();
	assertEquals(4, result.size());
	assertTrue(result.contains(new IndexedBean1("abc", 1)));
	assertTrue(result.contains(new IndexedBean1("abc", 2)));
	assertTrue(result.contains(new IndexedBean1("xyz", 1)));
	assertTrue(result.contains(new IndexedBean1("xyz", 2)));

	result = b1.filter().having("name", "qwerty").or()
		.in("name", new HashSet<String>(Arrays.asList("abc")))
		.results();
	assertEquals(3, result.size());
	assertTrue(result.contains(new IndexedBean1("abc", 1)));
	assertTrue(result.contains(new IndexedBean1("abc", 2)));
	assertTrue(result.contains(new IndexedBean1("qwerty", 8)));
    }

    @Test
    public void notInTest() {
	Set<IndexedBean1> result = b1
		.filter()
		.notIn("name", new HashSet<Object>(Arrays.asList("abc", "xyz")))
		.results();
	assertEquals(1, result.size());
	assertTrue(result.contains(new IndexedBean1("qwerty", 8)));
	
	result = b1.filter().having("name", "abc").or()
		.notIn("name", new HashSet<Object>(Arrays.asList("abc", "xyz")))
		.results();
	assertEquals(3, result.size());
	assertTrue(result.contains(new IndexedBean1("abc", 1)));
	assertTrue(result.contains(new IndexedBean1("abc", 2)));
	assertTrue(result.contains(new IndexedBean1("qwerty", 8)));
    }

    @Test
    public void propertyValuesTest() {
	Set<Object> set = b1.propertyValues("name");
	assertEquals(3, set.size());
	assertTrue(set.contains("abc"));
	assertTrue(set.contains("xyz"));
	assertTrue(set.contains("qwerty"));
    }
    
    @Test
    public void addWriteOnlyPropertyTest() {
        BeanIndexer<ReadOrWriteOnlyBean> indexer = BeanIndexer.beanIndexFrom(ReadOrWriteOnlyBean.class);
        Set<String> indexes = indexer.getAllIndexes();
        
        assertEquals(2, indexes.size());
        assertTrue(indexes.contains("name"));
        assertTrue(indexes.contains("score"));
    }
    
    @Test
    public void comparatorIndexTest() {
        BeanIndexer<ReadOrWriteOnlyBean> indexer = BeanIndexer.beanIndexFrom(ReadOrWriteOnlyBean.class);
        
        indexer.add(new ReadOrWriteOnlyBean("a" , 1))
                .add(new ReadOrWriteOnlyBean("a" , 2))
                .add(new ReadOrWriteOnlyBean("a" , 3));
        
        Set<ReadOrWriteOnlyBean> result = indexer.filter().havingGreater("score", 2, false).results();
        
        assertEquals(1, result.size());
        assertTrue(result.contains(new ReadOrWriteOnlyBean("a" , 1)));//because of the reverse comparator
    }
}
