package org.limewire.util;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;


/**
 *  Predicate parser that parses a string encoded in Reverse Polish Notation
 *  and evaluates it to true or false. 
 *  <p>
 *  An optional StringLookup object can be provided the different terms
 *  are looked up for values.
 */
public class RPNParser {
    
    /** Map from recognized operands to their implementations. */
    private static final Map<String, Predicate> predicateByOperand;
    private static final Set<String> experimentalPredicates;
    static {
        Map<String, Predicate> pMap = new HashMap<String, Predicate>();
        Set<String> eSet = new HashSet<String>();
        pMap.put("==", new EqualsPredicate());
        pMap.put("<", new LessPredicate());
        pMap.put(">", new GreaterPredicate());
        pMap.put("NOT", new NOTPredicate());
        pMap.put("OR", new ORPredicate());
        pMap.put("AND", new ANDPredicate());
        pMap.put("CONTAINS", new ContainsPredicate());
        pMap.put("MATCHES", new MatchesPredicate());
        eSet.add("MATCHES");
        predicateByOperand = Collections.unmodifiableMap(pMap);
        experimentalPredicates = Collections.unmodifiableSet(eSet);
    }
    
    /**
     * Interface that provides lookup based on string.
     */
    public static interface StringLookup {
        /**
         * @return value for certain key.
         * A return value of null means there is no such key.
         */
        public String lookup(String key);
    }
    
    /** expression to evaluate */
    private final String[] expression;
    
    /** Whether experimental predicates are allowed */
    private final boolean experimental;
    
    /** Stack used for parsing */
    private final Stack<String> stack = new Stack<String>();
    
    public RPNParser(String... expression) {
        this(true, expression);
    }
    
    /**
     * Creates a new parser.
     * @param expression the expression to parse
     * @param experimental if experimental predicates are allowed
     */
    public RPNParser(boolean experimental, String... expression) {
        this.expression = expression;
        this.experimental = experimental;
    }
    
    /**
     * @return true or false.
     * @throws IllegalArgumentException if either the input or
     * the values returned by the lookups are not valid.
     */
    public boolean evaluate() {
        return evaluate(new StringLookup() {
            public String lookup(String key) {
                return key;
            }
        });
    }
    
    /**
     * @return true or false.
     * @throws IllegalArgumentException if either the input or
     * the values returned by the lookups are not valid.
     */
    public boolean evaluate(StringLookup lookup) {
        for (String r : expression) {
            
            if (r == null)
                throw new IllegalArgumentException("null input");
            
            if (!predicateByOperand.containsKey(r)) {
                String val = lookup.lookup(r);
                stack.push(val != null ? val : r);
            } else if (experimental || !experimentalPredicates.contains(r))
                evaluateOp(r);
        }
        
        if (stack.size() != 1) // this illegal state can only be caused by illegal argument
            throw new IllegalArgumentException(stack.size()+" elements at end of parse");
        
        return Boolean.valueOf(stack.pop());
    }
    
    /**
     *  Evaluates an operand based on the contents of the stack
     *  and pushes either "True" or "False" on the stack.  
     */
    private void evaluateOp(String operand) {
        Predicate p = predicateByOperand.get(operand);
        
        if (stack.size() < p.numOperands())
            throw new IllegalArgumentException("not enough operands"+p.numOperands());
        
        String[] strings = new String[p.numOperands()];
        for (int i = strings.length-1; i>=0; i--)
            strings[i] = stack.pop();
        stack.push(Boolean.toString(p.evaluate(strings)));
    }
    
    /**
     * A class representing a boolean predicate. 
     */
    private static abstract class Predicate {
        /**
         * @return true or false depending on how the operands evaluate
         */
        public abstract boolean evaluate(String... operands);
        
        /**
         * @return the number of operands this predicate needs.
         */
        public int numOperands() {
            return 2;
        }
    }
    
    /**
     * Predicate for the == operation.
     */
    static class EqualsPredicate extends Predicate {
        @Override
        public boolean evaluate(String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            return operands[0].equals(operands[1]);
        }
    }
    
    /**
     * Predicate for the || operation.
     */
    static class ORPredicate extends Predicate {
        @Override
        public boolean evaluate(String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            return strictBoolean(operands[0]) || strictBoolean(operands[1]);
        }
    }
    
    /**
     * Predicate for the && operation.
     */
    static class ANDPredicate extends Predicate {
        @Override
        public boolean evaluate(String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            return strictBoolean(operands[0]) && strictBoolean(operands[1]);
        }
    }
    
    /**
     * Predicate for the ! operation.  
     */
    static class NOTPredicate extends Predicate {
        @Override
        public boolean evaluate(String... operands) {
            if (operands.length != 1)
                throw new IllegalArgumentException();
            return !strictBoolean(operands[0]);
        }
        @Override
        public int numOperands() {
            return 1;
        }
    }
    
    /**
     * Predicate for the > operation.
     */
    static class GreaterPredicate extends Predicate {
        @Override
        public boolean evaluate (String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            return Double.valueOf(operands[0]) > Double.valueOf(operands[1]);
        }
    }
    
    /**
     * Predicate for the < operation.
     */
    static class LessPredicate extends Predicate {
        @Override
        public boolean evaluate (String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            return Double.valueOf(operands[0]) < Double.valueOf(operands[1]);
        }
    }
    
    /**
     * Predicate for the String.contains operation
     */
    static class ContainsPredicate extends Predicate {
        @Override
        public boolean evaluate (String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            return operands[0].toLowerCase().contains(operands[1].toLowerCase());
        }
    }

    /**
     * Predicate for matching a pattern.
     */
    static class MatchesPredicate extends Predicate {
        @Override
        public boolean evaluate (String... operands) {
            if (operands.length != 2)
                throw new IllegalArgumentException();
            
            return Pattern.matches(operands[0],operands[1]);
        }
    }
    
    /**
     * Stricter version of Boolean.valueOf.
     */
    static boolean strictBoolean(String s) {
        if (s == null)
            throw new IllegalArgumentException();
        if (s.equalsIgnoreCase("true"))
            return true;
        if (s.equalsIgnoreCase("false"))
            return false;
        throw new IllegalArgumentException();
    }
}


