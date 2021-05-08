
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

include $(LOCAL_PATH)/Android2.mk

$(call import-module,android/cpufeatures)

