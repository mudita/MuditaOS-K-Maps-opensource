package net.osmand;


import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * That class is replacing of standard LogFactory due to 
 * problems with Android implementation of LogFactory.
 * 
 * 1. It is impossible to replace standard LogFactory (that is hidden in android.jar)
 * 2. Implementation of LogFactory always creates Logger.getLogger(String name)
 * 3. + It is possible to enable logger level by calling 
 * 		Logger.getLogger("net.osmand").setLevel(Level.ALL);
 * 4. Logger goes to low level android.util.Log where android.util.Log#isLoggable(String, int) is checked
 *    String tag -> is string of length 23 (stripped full class name)
 * 5. It is impossible to set for all tags debug level (info is default) - android.util.Log#isLoggable(String, int).
 *  
 */
public class PlatformUtil {
	public static String TAG = "net.osmand.map.java"; //$NON-NLS-1$
	private static class OsmandLogImplementation implements Log {
		
		private final String fullName;
		private final String name;

		public OsmandLogImplementation(String name){
			this.fullName = name;
			this.name = fullName.substring(fullName.lastIndexOf('.') + 1);
		}
		
		@Override
		public void trace(Object message) {
			if(isTraceEnabled()){
				System.out.println(TAG + name + " " + message); //$NON-NLS-1$
			}
		}
		
		@Override
		public void trace(Object message, Throwable t) {
			if(isTraceEnabled()){
				System.out.println(TAG + name + " " + t.getMessage()); //$NON-NLS-1$
			}
		}

		
		
		@Override
		public void debug(Object message) {
			if(isDebugEnabled()){
				System.out.println(TAG + name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void debug(Object message, Throwable t) {
			if(isDebugEnabled()){
				System.out.println(TAG + name + " " + t.getMessage()); //$NON-NLS-1$
			}
		}

		@Override
		public void error(Object message) {
			if(isErrorEnabled()){
				System.out.println(TAG +  name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void error(Object message, Throwable t) {
			if(isErrorEnabled()){
				System.out.println(TAG +  name + " " + t.getMessage()); //$NON-NLS-1$
			}
		}

		@Override
		public void fatal(Object message) {
			if(isFatalEnabled()){
				System.out.println(TAG +  name + " " + message); //$NON-NLS-1$
			}
			
		}

		@Override
		public void fatal(Object message, Throwable t) {
			if(isFatalEnabled()){
				System.out.println(TAG +  name + " " + t.getMessage()); //$NON-NLS-1$
			}
		}

		@Override
		public void info(Object message) {
			if(isInfoEnabled()){
				System.out.println(TAG + name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void info(Object message, Throwable t) {
			if(isInfoEnabled()){
				System.out.println(TAG + name + " " + t.getMessage()); //$NON-NLS-1$
			}
		}
		
		@Override
		public boolean isTraceEnabled() {
			return true;
		}



		@Override
		public boolean isDebugEnabled() {
			// For debug purposes always true
			// return android.util.Log.isLoggable(TAG, android.util.Log.DEBUG);
			return true;
		}

		@Override
		public boolean isErrorEnabled() {
			return true;
		}

		@Override
		public boolean isFatalEnabled() {
			return true;
		}

		@Override
		public boolean isInfoEnabled() {
			return true;
		}

		@Override
		public boolean isWarnEnabled() {
			return true;
		}

		@Override
		public void warn(Object message) {
			if(isWarnEnabled()){
				System.out.println(TAG +  name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void warn(Object message, Throwable t) {
			if(isWarnEnabled()){
				System.out.println(TAG +  name + " " + t.getMessage()); //$NON-NLS-1$
			}
		}
	}
	
	public static Log getLog(String name){
		return new OsmandLogImplementation(name);
	}
	
	public static Log getLog(Class<?> cl){
		return getLog(cl.getName());
	}
	
	public static XmlPullParser newXMLPullParser() throws XmlPullParserException {
		 return XmlPullParserFactory.newInstance().newPullParser();
	}
	
	public static XmlSerializer newSerializer() throws XmlPullParserException {
		return XmlPullParserFactory.newInstance().newSerializer();
	}
	

}
