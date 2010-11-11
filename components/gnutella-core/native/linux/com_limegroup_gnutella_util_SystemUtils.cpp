/*
This library gets idle calls from systems that have X and screensavers installed.
(which means practically any linux user that would use limewire ;-))
*/

#include "com_limegroup_gnutella_util_SystemUtils.h"

#  include <X11/Xlib.h>
#  include <X11/Xutil.h>
# include <X11/X.h>
#  include <X11/extensions/scrnsaver.h>
//#include <iostream>

//global to cache across jni invokations
Display *display = NULL;
Window window =0;
XScreenSaverInfo *mit_info = NULL;

//will do these later
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setOpenFileLimit0
  (JNIEnv *, jclass, jint){return 0;}
  
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setFileWriteable
  (JNIEnv *, jclass, jstring){return 0;}
  
  
//gets the idle time from X
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime
  (JNIEnv *, jclass){
  //cout << "entered method\n";
  
  	if(display == NULL) 
  		display = XOpenDisplay(NULL);

	
	//if we are still null, fail gracefully
	if (display == NULL)
		return 0;
		
	//cout << "display not null\n";
	
	if (window==0)
		window = DefaultRootWindow(display);
		
	//if still 0, fail gracefully
	if (window==0)
		return 0;
	
	//cout << "got window\n";
	
  	int event_base, error_base;

		if (XScreenSaverQueryExtension(display, &event_base, &error_base)) {
	//		cout << "system supports screensavers\n";
			
			if (mit_info == NULL) {
				mit_info = XScreenSaverAllocInfo();
			}
			
			//if still null, fail gracefully.
			if (mit_info ==NULL) 
				return 0;
			
			XScreenSaverQueryInfo(display, window, mit_info);
//			cout <<"queried successfully " << mit_info->idle <<"\n";			
			return mit_info->idle;
			

		} else
			return 0;
			
  }
