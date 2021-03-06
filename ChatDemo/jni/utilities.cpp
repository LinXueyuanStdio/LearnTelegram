#include <jni.h>
#include <sys/stat.h>
#include <climits>
#include <unistd.h>

thread_local static char buf[PATH_MAX + 1];

extern "C" JNIEXPORT jstring Java_com_demo_chat_messager_Utilities_readlink(JNIEnv *env, jclass clazz, jstring path) {
    const char *fileName = env->GetStringUTFChars(path, NULL);
    ssize_t result = readlink(fileName, buf, PATH_MAX);
    jstring value = 0;
    if (result != -1) {
        buf[result] = '\0';
        value = env->NewStringUTF(buf);
    }
    env->ReleaseStringUTFChars(path, fileName);
    return value;
}
