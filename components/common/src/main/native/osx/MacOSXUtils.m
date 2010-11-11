//
//  MacOSXUtils.m
//  GURL
//
//  Created by Curtis Jones on 2008.04.08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

#import <JavaVM/jni.h>
#import <Foundation/Foundation.h>
#import <Cocoa/Cocoa.h>

#ifdef __cplusplus
extern "C" {
#endif

#define OS_NATIVE(func) Java_org_limewire_ui_swing_util_MacOSXUtils_##func

JNIEXPORT jstring JNICALL OS_NATIVE(GetCurrentFullUserName)
    (JNIEnv *env, jobject clazz)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSString *usernameNSString = NSFullUserName();
    jstring usernameJString = (*env)->NewStringUTF(env, [usernameNSString UTF8String]);
    [pool release];
    
    return usernameJString;
}

JNIEXPORT void JNICALL OS_NATIVE(SetLoginStatusNative)
    (JNIEnv *env, jobject obj, jboolean onoff)
{
    NSMutableArray *loginItems;
    NSDictionary *appDict;
    NSEnumerator *appEnum;
    NSString *agentAppPath = @"/Applications/LimeWire.app";
    
    NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];

    // Make a mutable copy (why a copy?)
    loginItems = (NSMutableArray *)CFPreferencesCopyValue((CFStringRef)@"AutoLaunchedApplicationDictionary", 
                                                                                                                (CFStringRef)@"loginwindow", kCFPreferencesCurrentUser, 
                                                                                                                kCFPreferencesAnyHost);
    loginItems = [[loginItems autorelease] mutableCopy];
    appEnum = [loginItems objectEnumerator];
    
    while ((appDict = [appEnum nextObject])) {
        if ([[[appDict objectForKey:@"Path"] stringByExpandingTildeInPath] isEqualToString:agentAppPath])
            break;
    }
    
    // register the item
    if (onoff == JNI_TRUE) {
        if (!appDict)
            [loginItems addObject:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:NO], @"Hide", agentAppPath, @"Path", nil]];
    }
    // unregister the item
    else if (appDict)
        [loginItems removeObject:appDict];
    
    CFPreferencesSetValue((CFStringRef)@"AutoLaunchedApplicationDictionary", 
                                                loginItems, (CFStringRef)@"loginwindow", 
                                                kCFPreferencesCurrentUser, kCFPreferencesAnyHost);
    
    CFPreferencesSynchronize((CFStringRef)@"loginwindow", kCFPreferencesCurrentUser, kCFPreferencesAnyHost);
    
    [loginItems release];
    [pool release];    
}

JNIEXPORT jint JNICALL OS_NATIVE(SetDefaultFileTypeHandler)
    (JNIEnv *env, jobject this, jstring fileType, jstring applicationBundleIdentifier)
{
    OSErr theErr = -1;
    
    const char *fileTypeCstr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCstr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCstr,                                    
                                                  kCFStringEncodingMacRoman);

    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);
    
    const char *applicationBundleIdentifierCstr = (*env)->GetStringUTFChars(env, applicationBundleIdentifier, NULL);
    if (applicationBundleIdentifierCstr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef applicationBundleIdentifierCFStr = CFStringCreateWithCString(NULL, applicationBundleIdentifierCstr,                                    
                                                                            kCFStringEncodingMacRoman);

    theErr = LSSetDefaultRoleHandlerForContentType(
                    utiForTorrents,
                    kLSRolesAll, 
                    applicationBundleIdentifierCFStr);
    
    (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCstr);
    (*env)->ReleaseStringUTFChars(env, fileType, applicationBundleIdentifierCstr);

    CFRelease(fileTypeCFStr);
    CFRelease(applicationBundleIdentifierCFStr);
    CFRelease(utiForTorrents);

    return (jint)theErr;
}

JNIEXPORT jint JNICALL OS_NATIVE(SetDefaultURLSchemeHandler)
    (JNIEnv *env, jobject this, jstring urlScheme, jstring applicationBundleIdentifier)
{
    OSErr theErr = -1;

    const char *urlSchemeCstr = (*env)->GetStringUTFChars(env, urlScheme, NULL);
    if (urlSchemeCstr == NULL) {
        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCstr);

        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef urlSchemeCFStr = CFStringCreateWithCString(NULL, urlSchemeCstr,                                    
                                                  kCFStringEncodingMacRoman);

    const char *applicationBundleIdentifierCstr = (*env)->GetStringUTFChars(env, applicationBundleIdentifier, NULL);
    if (applicationBundleIdentifierCstr == NULL) {
        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCstr);
        CFRelease(urlSchemeCFStr);

        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef applicationBundleIdentifierCFStr = CFStringCreateWithCString(NULL, applicationBundleIdentifierCstr,                                    
                                                                            kCFStringEncodingMacRoman);

    theErr = LSSetDefaultHandlerForURLScheme(
                    urlSchemeCFStr,
                    applicationBundleIdentifierCFStr);
    
    (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCstr);
    (*env)->ReleaseStringUTFChars(env, urlScheme, applicationBundleIdentifierCstr);

    CFRelease(urlSchemeCFStr);
    CFRelease(applicationBundleIdentifierCFStr);

    return (jint) theErr;
}

JNIEXPORT jboolean JNICALL OS_NATIVE(IsApplicationTheDefaultFileTypeHandler)
(JNIEnv *env, jobject this, jstring fileType, jstring applicationBundleIdentifier)
{
    OSErr theErr = -1;
    
    const char *fileTypeCStr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCStr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCStr,                                    
                                                          kCFStringEncodingMacRoman);
    
    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);

    CFStringRef defaultApplicationIdentifier = LSCopyDefaultRoleHandlerForContentType(utiForTorrents, kLSRolesAll);

    if ( defaultApplicationIdentifier == NULL ) {
        CFRelease(fileTypeCFStr);
        CFRelease(utiForTorrents);

        return false;
    }
    
    const char *applicationBundleIdentifierCstr = (*env)->GetStringUTFChars(env, applicationBundleIdentifier, NULL);
    if (applicationBundleIdentifierCstr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef applicationBundleIdentifierCFStr = CFStringCreateWithCString(NULL, applicationBundleIdentifierCstr,                                    
                                                                            kCFStringEncodingMacRoman);

    bool isGivenApplicationTheDefaultFileTypeHandler = (CFStringCompare(defaultApplicationIdentifier, applicationBundleIdentifierCFStr, kCFCompareCaseInsensitive) == 0);

    (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCStr);
    (*env)->ReleaseStringUTFChars(env, fileType, applicationBundleIdentifierCstr);

    CFRelease(fileTypeCFStr);
    CFRelease(utiForTorrents);
    CFRelease(defaultApplicationIdentifier);
    CFRelease(applicationBundleIdentifierCFStr);

    return isGivenApplicationTheDefaultFileTypeHandler;
}

JNIEXPORT jboolean JNICALL OS_NATIVE(IsApplicationTheDefaultURLSchemeHandler)
(JNIEnv *env, jobject this, jstring urlScheme, jstring applicationBundleIdentifier)
{
    const char *urlSchemeCStr = (*env)->GetStringUTFChars(env, urlScheme, NULL);
    if (urlSchemeCStr == NULL) {
        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);

        return false; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef urlSchemeCFStr = CFStringCreateWithCString(NULL, urlSchemeCStr,                                    
                                                          kCFStringEncodingMacRoman);

    CFStringRef defaultApplicationIdentifier = LSCopyDefaultHandlerForURLScheme(urlSchemeCFStr);

    if ( defaultApplicationIdentifier == NULL ) {
        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);
        CFRelease(urlSchemeCFStr);

        return false;
    }
    
    const char *applicationBundleIdentifierCstr = (*env)->GetStringUTFChars(env, applicationBundleIdentifier, NULL);
    if (applicationBundleIdentifierCstr == NULL) {
        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);
        CFRelease(urlSchemeCFStr);
        CFRelease(defaultApplicationIdentifier);

        return false; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef applicationBundleIdentifierCFStr = CFStringCreateWithCString(NULL, applicationBundleIdentifierCstr,                                    
                                                                            kCFStringEncodingMacRoman);

    bool isGivenApplicationTheDefaultFileTypeHandler = (CFStringCompare(defaultApplicationIdentifier, applicationBundleIdentifierCFStr, kCFCompareCaseInsensitive) == 0);

    (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);
    (*env)->ReleaseStringUTFChars(env, urlScheme, applicationBundleIdentifierCstr);

    CFRelease(urlSchemeCFStr);
    CFRelease(defaultApplicationIdentifier);
    CFRelease(applicationBundleIdentifierCFStr);

    return isGivenApplicationTheDefaultFileTypeHandler;
}

JNIEXPORT jboolean JNICALL OS_NATIVE(IsFileTypeHandled)
(JNIEnv *env, jobject this, jstring fileType)
{
    OSErr theErr = -1;
    
    const char *fileTypeCStr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCStr == NULL) {
        return theErr; /* OutOfMemoryError already thrown */
    }
    
    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCStr,                                    
                                                          kCFStringEncodingMacRoman);
    
    //(*env)->GetStringUTFChars(env, fileType, true)
    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);

    CFStringRef defaultApplicationIdentifier = LSCopyDefaultRoleHandlerForContentType(utiForTorrents, kLSRolesAll);

    bool isFileTypeHandled = (defaultApplicationIdentifier != NULL);

    (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCStr);

    CFRelease(fileTypeCFStr);
    CFRelease(utiForTorrents);
    if (defaultApplicationIdentifier != nil)
        CFRelease(defaultApplicationIdentifier);

    return isFileTypeHandled;
}

JNIEXPORT jboolean JNICALL OS_NATIVE(IsURLSchemeHandled)
(JNIEnv *env, jobject this, jstring urlScheme)
{
    const char *urlSchemeCStr = (*env)->GetStringUTFChars(env, urlScheme, NULL);
    if (urlSchemeCStr == NULL) {
        return false; /* OutOfMemoryError already thrown */
    }

    CFStringRef urlSchemeCFStr = CFStringCreateWithCString(NULL, urlSchemeCStr,                                    
                                                          kCFStringEncodingMacRoman);

    CFStringRef defaultApplicationIdentifier = LSCopyDefaultHandlerForURLScheme(urlSchemeCFStr);

    bool isURLSchemeHandled = (defaultApplicationIdentifier != nil);

    (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);

    CFRelease(urlSchemeCFStr);
    if (defaultApplicationIdentifier != nil)
        CFRelease(defaultApplicationIdentifier);

    return isURLSchemeHandled;
}

/**
* This method returns all of the applications registered to handle the file type
* designated by the given file extension.
*/
JNIEXPORT jobjectArray JNICALL OS_NATIVE(GetAllHandlersForFileType)
(JNIEnv *env, jobject this, jstring fileType)
{
    const char *fileTypeCStr = (*env)->GetStringUTFChars(env, fileType, NULL);
    if (fileTypeCStr == NULL) {
        /* OutOfMemoryError already thrown */
        return NULL;
    }

    CFStringRef fileTypeCFStr = CFStringCreateWithCString(NULL, fileTypeCStr,                                    
                                                          kCFStringEncodingMacRoman);
    
    CFStringRef utiForTorrents = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension,
                                                                       fileTypeCFStr,
                                                                       kUTTypeData);

    CFArrayRef handlers = LSCopyAllRoleHandlersForContentType(utiForTorrents, kLSRolesAll);

    if (handlers == NULL) {
        CFRelease(fileTypeCFStr);
        CFRelease(utiForTorrents);
        
        return NULL;
    } else {
        // if we have a valid list of file URLs, then let's convert them to 
        // a java string array and pass it out.
        jclass strCls = (*env)->FindClass(env,"Ljava/lang/String;");
        jobjectArray handlerArray = (*env)->NewObjectArray(env, CFArrayGetCount(handlers), strCls, NULL);
    
        for (int counter = 0; counter < CFArrayGetCount(handlers); counter++) {
            CFStringRef applicationBundleIdentifier = CFArrayGetValueAtIndex(handlers, counter);
            
            CFRange range;
            range.location = 0;
            // Note that CFStringGetLength returns the number of UTF-16 characters,
            // which is not necessarily the number of printed/composed characters
            range.length = CFStringGetLength(applicationBundleIdentifier);
            UniChar charBuf[range.length];
            CFStringGetCharacters(applicationBundleIdentifier, range, charBuf);
            jstring applicationBundleIdentifierJavaStr = (*env)->NewString(env, (jchar *)charBuf, (jsize)range.length);

            // set the Java string in the java string array
            (*env)->SetObjectArrayElement(env, handlerArray, counter, applicationBundleIdentifierJavaStr);
            
            (*env)->DeleteLocalRef(env, applicationBundleIdentifierJavaStr);            
    
        }

        (*env)->ReleaseStringUTFChars(env, fileType, fileTypeCStr);

        CFRelease(handlers);
        CFRelease(fileTypeCFStr);
        CFRelease(utiForTorrents);
        
        return handlerArray;
    }
}

/**
* This method returns all of the applications registered to handle this URL scheme
*/
JNIEXPORT jobjectArray JNICALL OS_NATIVE(GetAllHandlersForURLScheme)
(JNIEnv *env, jobject this, jstring urlScheme)
{
    const char *urlSchemeCStr = (*env)->GetStringUTFChars(env, urlScheme, NULL);
    if (urlSchemeCStr == NULL) {
        /* OutOfMemoryError already thrown */
        return NULL;
    }

    CFStringRef urlSchemeCFStr = CFStringCreateWithCString(NULL, urlSchemeCStr,                                    
                                                          kCFStringEncodingMacRoman);

    CFArrayRef handlers = LSCopyAllHandlersForURLScheme(urlSchemeCFStr);

    if (handlers == NULL) {
        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);
        CFRelease(urlSchemeCFStr);
        
        return NULL;
    } else {
        // if we have a valid list of file URLs, then let's convert them to 
        // a java string array and pass it out.
        jclass strCls = (*env)->FindClass(env,"Ljava/lang/String;");
        jobjectArray handlerArray = (*env)->NewObjectArray(env, CFArrayGetCount(handlers), strCls, NULL);
    
        for (int counter = 0; counter < CFArrayGetCount(handlers); counter++) {
            CFStringRef applicationBundleIdentifier = CFArrayGetValueAtIndex(handlers, counter);
            
            CFRange range;
            range.location = 0;
            // Note that CFStringGetLength returns the number of UTF-16 characters,
            // which is not necessarily the number of printed/composed characters
            range.length = CFStringGetLength(applicationBundleIdentifier);
            UniChar charBuf[range.length];
            CFStringGetCharacters(applicationBundleIdentifier, range, charBuf);
            jstring applicationBundleIdentifierJavaStr = (*env)->NewString(env, (jchar *)charBuf, (jsize)range.length);

            // set the Java string in the java string array
            (*env)->SetObjectArrayElement(env, handlerArray, counter, applicationBundleIdentifierJavaStr);
            
            (*env)->DeleteLocalRef(env, applicationBundleIdentifierJavaStr);            
    
        }

        (*env)->ReleaseStringUTFChars(env, urlScheme, urlSchemeCStr);

        CFRelease(handlers);
        CFRelease(urlSchemeCFStr);

        return handlerArray;
    }
}

/**
* This object encapsulates the code for opening a file dialog.
* It's necessary to do this so that we can run this code on the main
* thread.  All the OSX user interface classes must be used from the
* main thread (just as Swing classes must be used by the AWT thread)
* in order to avoid concurrency problems.
*/
@interface RunnableForShowingFileDialogOnMainThread : NSObject
{
    NSString* title;
    NSString* directoryPath;
    bool canChooseFiles;
    bool canChooseDirectories;
    bool allowMultipleSelections;
    NSArray* urls;
}

- (void) run;
- (void) setTitle: (NSString*) argTitle;
- (void) setDirectoryPath: (NSString*) argDirectoryPath;
- (void) setCanChooseFiles: (bool) argCanChooseFiles;
- (void) setCanChooseDirectories: (bool) argCanChooseDirectories;
- (void) setAllowMultipleSelections: (bool) argAllowMultipleSelections;
- (NSArray*) getFileURLs;

@end

@implementation RunnableForShowingFileDialogOnMainThread
-(void) run {
    // Create the File Open Dialog class.
    NSOpenPanel* openDlg = [NSOpenPanel openPanel];
    
    // Set the dialogs title
    [openDlg setTitle:title];
    
    // Enable / disable the selection of files in the dialog.
    [openDlg setCanChooseFiles:canChooseFiles];
    
    // Enable / disable the selection of directories in the dialog.
    [openDlg setCanChooseDirectories:canChooseDirectories];

    // Enable / disable the selection of multiple files in the dialog.
    [openDlg setAllowsMultipleSelection:allowMultipleSelections];

    // Display the dialog.  If the OK button was pressed,
    // process the files.
    if ( [openDlg runModalForDirectory:directoryPath file:nil] == NSOKButton ) {
        // Get an array containing the full filenames of all
        // files and directories selected.
        urls = [[openDlg URLs] copy];
    } else {
        urls = nil;
    }
}

-(void) setTitle: (NSString*) argTitle {
    title = argTitle;
}

-(void) setDirectoryPath: (NSString*) argDirectoryPath {
    directoryPath = argDirectoryPath;
}

-(void) setCanChooseFiles: (bool) argCanChooseFiles {
    canChooseFiles = argCanChooseFiles;
}

-(void) setCanChooseDirectories: (bool) argCanChooseDirectories {
    canChooseDirectories = argCanChooseDirectories;
}

-(void) setAllowMultipleSelections: (bool) argAllowMultipleSelections {
    allowMultipleSelections = argAllowMultipleSelections;
}

-(NSArray*) getFileURLs {
    return urls;
}
@end

/**
* This method opens up a native file dialog.  It was implemented in objective C
* and uses Cocoa, because the Carbon API for opening up file dialogs is 32 bit only.
*/
JNIEXPORT jobjectArray JNICALL OS_NATIVE(OpenNativeFileDialog)
(JNIEnv *env, jobject this, jstring title, jstring directoryPath, jboolean canChooseFiles, 
 jboolean canChooseDirectories, jboolean allowMultipleSelections)
{
    // we create an auto release pool to manage our objective c objects
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    // We create a runnable object for opening up the file dialog so that we can
    // run this code from the main thread.
    RunnableForShowingFileDialogOnMainThread* runnable = [[RunnableForShowingFileDialogOnMainThread alloc] init]; // Obj-C class

    // let's pass over the arguments to the runnable object

    // convert the Java string for the title into an NSString object
    const jchar *titleChars = (*env)->GetStringChars(env, title, NULL);
    NSString* titleNSString = [NSString stringWithCharacters:(UniChar *)titleChars
                            length:(*env)->GetStringLength(env, title)];
    (*env)->ReleaseStringChars(env, title, titleChars);

    [runnable setTitle:titleNSString];

    // convert the Java string for the directoryPath into an NSString object
    if (directoryPath != NULL) {
        const jchar *directoryPathChars = (*env)->GetStringChars(env, directoryPath, NULL);
        NSString* directoryPathNSString = [NSString stringWithCharacters:(UniChar *)directoryPathChars
                                           length:(*env)->GetStringLength(env, directoryPath)];
        (*env)->ReleaseStringChars(env, directoryPath, directoryPathChars);
        
        [runnable setDirectoryPath:directoryPathNSString];
    }

    [runnable setCanChooseFiles:canChooseFiles];
    
    [runnable setCanChooseDirectories:canChooseDirectories];
    
    [runnable setAllowMultipleSelections:allowMultipleSelections];
    
    // give the runnable object over to the main thread and wait for the
    // file dialog to open and close
    [runnable performSelectorOnMainThread:@selector(run)
                               withObject:nil
                               waitUntilDone:YES];

    // we get the list of NSURL objects for the files that the user selected
    NSArray* urls = [runnable getFileURLs];
    
    // if the user cancelled the operation, the list of files will be nil
    if ( urls == nil ) {
        [runnable release];
        [pool release];
		
        return nil;
    } else {		
        // if we have a valid list of file URLs, then let's convert them to 
        // a java string array and pass it out.
        jclass strCls = (*env)->FindClass(env,"Ljava/lang/String;");
        jobjectArray strarray = (*env)->NewObjectArray(env, [urls count], strCls, NULL);
    
        // Loop through all the files and process them.
        for( int counter = 0; counter < [urls count]; counter++ )
        {
            NSURL* url = [urls objectAtIndex:counter];
    
            // convert the file URL into a file path and then into a C style string,
            // turn it into a java string, and set it in the java string array
            jstring str = (*env)->NewStringUTF(env, [[url path] UTF8String]);
            (*env)->SetObjectArrayElement(env, strarray, counter, str);
            (*env)->DeleteLocalRef(env, str);            
        }
    
        [runnable release];
        [pool release];
    
        return strarray;
    }
}

#ifdef __cplusplus
}
#endif
