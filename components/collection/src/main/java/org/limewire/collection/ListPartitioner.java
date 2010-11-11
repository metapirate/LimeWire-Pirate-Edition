package org.limewire.collection;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Partitions a list into sublists with equal size. The remainder items in the 
 * list are included with the last sublist. For example, list = {1,2,3,4,5} and 2
 * partitions makes subList1 = {1,2} and subList2 = {3,4,5}.
<pre>
    LinkedList&lt;String&gt; list = new LinkedList&lt;String&gt;();
    for(int i = 1; i < 6; i++)
        list.add(String.valueOf(i));
    System.out.println(list);
    
    ListPartitioner&lt;String&gt; lp = new ListPartitioner&lt;String&gt;(list, 2);
    List&lt;String&gt; p1 = lp.getPartition(0);
    List&lt;String&gt; p2 = lp.getPartition(1);
    
    System.out.println("partition 1: " + p1);
    System.out.println("partition 2: " + p2);
    
    Output:
        [1, 2, 3, 4, 5]
        partition 1: [1, 2]
        partition 2: [3, 4, 5]

</pre>
 */ 

// ListPartitioner could be easily made to implement Iterable<List<E>>
 
public class ListPartitioner<E> {
    private final List<E> list;
    private final int numPartitions;
    
    public ListPartitioner(List<E> list, int numPartitions) {
        assert numPartitions > 0;
        this.list = list;
        this.numPartitions = numPartitions;
    }
    
    public List<E> getPartition(int index) {
        if (index >= numPartitions)
            throw new NoSuchElementException();
        if (numPartitions == 1)
            return list;
        if (list.isEmpty())
            return Collections.emptyList();
        
        int partitionSize = list.size() / numActivePartitions();
        if (partitionSize * index >= list.size())
            return Collections.emptyList();
        
        // if the last partition is not full, extend it
        int end = partitionSize * (index + 1);
        if (list.size() - end <= partitionSize && index == numPartitions -1 )
            end = list.size();
        
        return list.subList(partitionSize * index, end);
    }
    
    public List<E> getLastPartition() {
        return getPartition(numActivePartitions() - 1);
    }
    
    private int numActivePartitions() {
        return Math.min(list.size(), numPartitions);
    }
    
    public List<E> getFirstPartition() {
        return getPartition(0);
    }
}
