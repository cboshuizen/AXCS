LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libwebp
LOCAL_CFLAGS    := -Werror -Wall -DANDROID -DHAVE_MALLOC_H -DHAVE_PTHREAD -finline-functions -frename-registers -ffast-math -s -fomit-frame-pointer -std=c99
LOCAL_SRC_FILES := analysis.c bit_writer.c config.c cost.c dsp.c filter.c frame.c iterator.c picture.c quant.c syntax.c tree.c webpenc.c webpWrapper.c

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -L$(LOCAL_PATH)/../libs/armeabi -llog -lpng

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_STATIC_LIBRARY)

#include $(LOCAL_PATH)/../external/jpeg/Android.mk
#include $(LOCAL_PATH)/../external/zlib/Android.mk
include $(LOCAL_PATH)/../external/libpng/Android.mk
