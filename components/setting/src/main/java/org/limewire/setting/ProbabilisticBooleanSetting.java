package org.limewire.setting;

import java.util.Properties;

/**
 * Provides a boolean setting value that is true a random percentage
 * of time. As a subclass of 
 * <code>Setting</code>, the setting has a key. 
 * <p>
 * You set the random percentage of time as a float between 0 (never)
 * and 1 (always). 
 * <p>
 * Create a <code>ProbabilisticBooleanSetting</code> object with a 
 * {@link SettingsFactory#createProbabilisticBooleanSetting(String, float)}.
 * <p>
 * In the following code example, the probability <code>pb.getBoolean</code>
 * returns true is .001. The probability <code>pb2.getBoolean</code> 
 * returns true is 1/4.
<pre>
        File f = new File("setting.txt");
        SettingsFactory sf = new SettingsFactory(f);
        ProbabilisticBooleanSetting pb = sf.createProbabilisticBooleanSetting("pb", .5f);
        pb.setValue(.001f);//low odds for pb.getBoolean to be true
        System.out.println("pb is " + pb.getBoolean());

        ProbabilisticBooleanSetting pb2 = sf.createProbabilisticBooleanSetting("pb2", .5f);
        pb2.setValue(.25f);//return true 25 percent of the time
        System.out.println("pb2 is " + pb2.getBoolean());

     Random Output:
         pb is false
         pb2 is false
     Or:
         pb is false
         pb2 is true
</pre>
 */
public class ProbabilisticBooleanSetting extends FloatSetting {
    
    private volatile boolean value;

    ProbabilisticBooleanSetting(Properties defaultProps, Properties props, String key, float defaultFloat, float min, float max) {
        super(defaultProps, props, key, defaultFloat, min, max);
    }

    ProbabilisticBooleanSetting(Properties defaultProps, Properties props, String key, float defaultFloat) {
        super(defaultProps, props, key, defaultFloat);
    }
   
    public boolean getBoolean() {
        return value;
    }
    
    public void setBoolean(boolean b){
        setValue(b ? 1.0f : 0f);
    }
 
    @Override
    protected void loadValue(String sValue) {
        super.loadValue(sValue);
        value = Math.random() < getValue();
    }

    /**
     * @return reevaluated boolean based on value of float setting
     */
    public boolean evaluateBoolean() {
        return Math.random() < getValue();
    }
}
