APP_STL := gnustl_static
APP_CPPFLAGS := -frtti -fexceptions
APP_CFLAGS := 	-O3 \
				-mcpu=cortex-a8 \
				-mfloat-abi=softfp \
				-fPIC \
				-march=armv7-a \
				-ffunction-sections \
				-funwind-tables \
				-fstack-protector \
				-fno-short-enums \
				-fno-exceptions \
				-fno-rtti
APP_ABI := armeabi-v7a
APP_PLATFORM := android-8
