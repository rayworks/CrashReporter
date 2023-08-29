#include <stdio.h>
#include <jni.h>

void Crash() {
    volatile int *a = (int *) (NULL);
    *a = 1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_balsikandar_crashreporter_sample_MainActivity_testCrash(JNIEnv *env, jobject thiz) {
    Crash();
}