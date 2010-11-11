package com.limegroup.gnutella;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Set specifically for URNs.
 * This is not backed by a HashSet because there are
 * so few URN types that it doesn't need to be.
 * 
 * This currently only supports SHA1 and TTROOT URNs. If further
 * UrnTypes are created, this class will have to be updated.
 * (Note that there'll have to be MANY changes.)
 * 
 * If the set already has a URN of the specified type added to it,
 * further additions of that type will be rejected.
 */
public class UrnSet implements Set<URN>, Iterable<URN>, Cloneable, Serializable {
    
    private static final long serialVersionUID = -1065284624401321676L;

    /** The sole URNs this knows about. */
    private URN sha1, ttroot, nms1;
    
    /** Returns a set of the UrnSet version of the set. */
    public static UrnSet resolve(Set<? extends URN> set) {
        if (set instanceof UnmodifiableUrnSet || set instanceof UrnSet) {
            return (UrnSet) set;
        }
        else {
            return new UrnSet(set);
        }
    }
    
    /** Returns a set of the UrnSet version of the set. */
    public static UrnSet modifiableSet(Set<? extends URN> set) {
        if (set instanceof UnmodifiableUrnSet) {
            UrnSet urnSet = (UrnSet) set;
            return new UrnSet(urnSet.getSHA1(), urnSet.getTTRoot(), urnSet.getNMS1());
        }
        if(set instanceof UrnSet) {
            return (UrnSet)set;
        } else {
            return new UrnSet(set);
        }
    }
    
    /** Returns an unmodifiable set of the UrnSet version of the set. */
    public static UrnSet unmodifiableSet(Set<? extends URN> set) {
        if(set instanceof UnmodifiableUrnSet) {
            return (UrnSet)set;
        } else {
            return new UnmodifiableUrnSet(set);
        }
    }
    
    /** Constructs an empty UrnSet. */
    public UrnSet() {}
    
    /** Constructs a UrnSet with the given URN. */
    public UrnSet(URN urn) {
        add(urn);
    }
    
    /** Constructs a UrnSet with URNs from the given collection. */
    public UrnSet(Collection<? extends URN> c) {
        addAll(c);
    }
    
    private UrnSet(URN sha1, URN ttroot, URN nms1) {
        this.sha1 = sha1;
        this.ttroot = ttroot;
        this.nms1 = nms1;
    }
    
    @Override
    public String toString() {
        return isEmpty() ? "{Empty UrnSet}" : "UrnSet of: " + sha1;
    }
    
    /** Clones this set. */
    @Override
    public UrnSet clone() {
        return new UrnSet(sha1, ttroot, nms1);
    }
    
    /** Returns the hashcode for this UrnSet. */
    @Override
    public int hashCode() {
        return sha1 == null ? 0 : sha1.hashCode();
    }
    
    public URN getSHA1() {
        return sha1;
    }
    
    public URN getTTRoot() {
        return ttroot;
    }
    
    public URN getNMS1() {
        return nms1;
    }
    
    /**
     * Determines if this set contains all the same objects
     * as another set.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;

        Collection c = (Collection) o;
        if (c.size() != size())
            return false;
        
        try {
            return containsAll(c);
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    /**
     * Attempts to add the given URN into this set.
     * If the set already contained a URN of the same type,
     * the new URN is rejected and this returns false.
     * 
     * If the URN's type is not supported, the URN is rejected
     * and this returns false.
     * 
     * @param o
     * @return
     */
    public boolean add(URN o) {
        return addInternal(o);
    }
    
    protected boolean addInternal(URN o) {
        if(o.isSHA1() && sha1 == null) {
            sha1 = o;
            return true;
        } else if (o.isTTRoot() && ttroot == null) {
            ttroot = o;
            return true;
        } else if(o.isNMS1() && nms1 == null) {
            nms1 = o;
            return true;
        }
        return false;
    }

    /**
     * Attempts to add all the URNs in the given collection into this
     * set.  If the set was modified as a result of any of the additions,
     * this will return true.  Otherwise, it will return false.
     * 
     * @param c
     * @return
     */
    public boolean addAll(Collection<? extends URN> c) {
        return addAllInternal(c);
    }
    
    protected boolean addAllInternal(Collection<? extends URN> c) {
        boolean ret = false;
        for(URN urn : c)
            ret |= addInternal(urn);
        return ret;
    }

    public void clear() {
        sha1 = null;
        ttroot = null;
        nms1 = null;
    }

    public boolean contains(Object o) {
        return o.equals(sha1) || o.equals(ttroot) || o.equals(nms1); 
    }

    public boolean containsAll(Collection<?> c) {
        if(c.size() > 3)
            return false;
        if(c.isEmpty())
            return true;
        if(isEmpty())
            return false;
        
        boolean ret = true;
        for (Object o : c)
            ret &= contains(o);
        return ret;
    }

    public boolean isEmpty() {
        return sha1 == null && ttroot == null && nms1 == null;
    }

    public Iterator<URN> iterator() {
        return new UrnIterator();
    }

    public boolean remove(Object o) {
        if(sha1 != null && o.equals(sha1)) {
            sha1 = null;
            return true;
        }
        
        if(ttroot != null && o.equals(ttroot)) {
            ttroot = null;
            return true;
        }
        
        if(nms1 != null && o.equals(nms1)) {
            nms1 = null;
            return true;
        }
        
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        if(sha1 == null && ttroot == null && nms1 == null || c.isEmpty())
            return false;
        
        boolean ret = false;
        for(Object o : c) {
            ret |= remove(o);
            if (isEmpty())
                break;
        }
        
        return ret;
    }

    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        if(sha1 != null && !c.contains(sha1)) {
            sha1 = null;
            ret = true;
        }
        if(ttroot != null && !c.contains(ttroot)) {
            ttroot = null;
            ret = true;
        }
        if(nms1 != null && !c.contains(nms1)) {
            nms1 = null;
            ret = true;
        }
        
        return ret;
    }

    public int size() {
        int ret = 0;
        if(sha1 != null)
            ret++;
        if(ttroot != null)
            ret++;
        if(nms1 != null)
            ret++;
        return ret;
    }

    public Object[] toArray() {
        switch(size()) {
        case 0: return new Object[0];
        case 1: if(sha1 != null)
                    return new Object[]{sha1};
                else if(ttroot != null)
                    return new Object[]{ttroot};
                else
                    return new Object[]{nms1};
        case 2: if(sha1 != null && ttroot != null) {
                    return new Object[]{sha1, ttroot};
                } else if(sha1 != null) {
                    return new Object[]{sha1, nms1};
                } else {
                    return new Object[]{ttroot, nms1};
                }
        case 3: return new Object[]{sha1, ttroot, nms1};
        default:
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size)
            a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
        
        switch(size) {
        case 1:
            if(sha1 != null)
                a[0] = (T)sha1;
            else if(ttroot != null)
                a[0] = (T)ttroot;
            else
                a[0] = (T)nms1;
            break;
        case 2:
            if(sha1 != null && ttroot != null) {
                a[0] = (T)sha1;
                a[1] = (T)ttroot;
            } else if(sha1 != null) {
                a[0] = (T)sha1;
                a[1] = (T)nms1;
            } else {
                a[0] = (T)ttroot;
                a[1] = (T)nms1;
            }
            break;        
        case 3:
            a[0] = (T)sha1;
            a[1] = (T)ttroot;
            a[2] = (T)nms1;
            break;
        }
        
        if(a.length > size)
            a[size] = null;
        return a;
    }
    
    /** Iterator that returns each of the Urn Types in turn. */
    private class UrnIterator implements Iterator<URN> {
        private boolean givenSHA1, givenTTRoot, givenNMS1;
        
        public boolean hasNext() {
            return (!givenSHA1 && sha1 != null) ||
               (!givenTTRoot && ttroot != null) ||
               (!givenNMS1 && nms1 != null);
        }

        public URN next() {
            if (!hasNext())
                throw new NoSuchElementException();
            
            if (!givenSHA1 && sha1 != null) {
                givenSHA1 = true;
                return sha1;
            }
            if (!givenTTRoot && ttroot != null) {
                givenTTRoot = true;
                return ttroot;
            }
            if (!givenNMS1 && nms1 != null) {
                givenNMS1 = true;
                return nms1;
            }
            throw new IllegalStateException();
        }

        public void remove() {
            if (!(givenSHA1 || givenTTRoot || givenNMS1))
                throw new IllegalStateException();
            
            if(givenNMS1) {
            	nms1 = null;
            } else if (givenTTRoot) {
                ttroot = null;
            } else if (givenSHA1) {
                sha1 = null;
            }
        }

    }

    public static URN getSha1(Set<? extends URN> urns) {
        if(urns instanceof UrnSet) {
            return ((UrnSet)urns).sha1;
        } else {
            for(URN urn : urns) {
                if(urn.isSHA1()) {
                    return urn;
                }
            }
            return null;
        }
    }
    
    public static URN getNMS1(Set<? extends URN> urns) {
        if(urns instanceof UrnSet) {
            return ((UrnSet)urns).nms1;
        } else {
            for(URN urn : urns) {
                if(urn.isNMS1()) {
                    return urn;
                }
            }
            return null;
        } 
    }
    
    private static class UnmodifiableUrnSet extends UrnSet {
        
        public UnmodifiableUrnSet(Set<? extends URN> set) {
            super.addAll(set);
        }
        
        private UnmodifiableUrnSet(URN sha1, URN ttroot, URN nms1) {
            super(sha1, ttroot, nms1);
        }
        
        @Override
        public boolean add(URN o) {
            throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");        
        }
        
        @Override
        public boolean addAll(Collection<? extends URN> c) {
            throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");        
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");
        }
        
        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");
        }
        
        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");
        }
        
        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");
        }

        @Override
        public Iterator<URN> iterator() {
            final Iterator<URN> oldIterator = super.iterator();
            return new Iterator<URN> () {
                @Override
                public boolean hasNext() {
                    return oldIterator.hasNext();
                }

                @Override
                public URN next() {
                    return oldIterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Can't modify an UnmodifiableUrnSet");
                }
            };
        }
        
        @Override
        public UrnSet clone() {
            return new UnmodifiableUrnSet(getSHA1(), getTTRoot(), getNMS1());
        }
    }
}
