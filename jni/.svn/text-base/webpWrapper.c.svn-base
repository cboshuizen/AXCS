#include <jni.h>

#include <stdio.h>

//#include <setjmp.h>   // note: this must be included *after* png.h
//#include "../external/jpeg/jpeglib.h"

//#include <png.h>
#include "../external/libpng/png.h"
#include <android/log.h>

#include "webp/encode.h"

//http://xms.eps.csie.ntut.edu.tw/xms/content/show.php?id=2752
#define LOG_TAG "webpWrapper-JNI"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG,__VA_ARGS__)


JNIEXPORT jint JNICALL
Java_phoneSat_code_ImagePacketizer_encoder(JNIEnv * env, jobject obj, jint width, 
										  jint height, jintArray rgb, jintArray output)
{
	LOGE("@@@@@@@@@@ Start encoder\n");
	int stride = 5;
	float quality = 50;
	int inputSize = (*env)->GetArrayLength(env, rgb);
	jint *rgbArray = (*env)->GetIntArrayElements(env, rgb, 0);
	uint8_t inputRgb[inputSize];
	uint8_t **outputArray = NULL;
	for (int i = 0; i < inputSize; ++i) {
		inputRgb[i] = rgbArray[i];
		LOGE("%d ", rgbArray[i]);
	}
	int outputSize = WebPEncodeRGB(inputRgb, width, height, stride, quality, outputArray);
	return outputSize;
}


static void PNGAPI error_function(png_structp png, png_const_charp dummy) {
	(void)dummy;  // remove variable-unused warning
	longjmp(png_jmpbuf(png), 1);
}

static int ReadPNG(FILE* in_file, WebPPicture* const pic) {
	png_structp png;
	png_infop info;
	int color_type, bit_depth, interlaced;
	int num_passes;
	int p;
	int ok = 0;
	png_uint_32 width, height, y;
	int stride;
	uint8_t* rgb = NULL;
	
	png = png_create_read_struct(PNG_LIBPNG_VER_STRING, 0, 0, 0);
	if (png == NULL) {
		goto End;
	}
	
	png_set_error_fn(png, 0, error_function, NULL);
	if (setjmp(png_jmpbuf(png))) {
	Error:
		png_destroy_read_struct(&png, NULL, NULL);
		if (rgb) free(rgb);
		goto End;
	}
	
	info = png_create_info_struct(png);
	if (info == NULL) goto Error;
	
	png_init_io(png, in_file);
	png_read_info(png, info);
	if (!png_get_IHDR(png, info,
					  &width, &height, &bit_depth, &color_type, &interlaced,
					  NULL, NULL)) goto Error;
	
	png_set_strip_16(png);
	png_set_packing(png);
	if (color_type == PNG_COLOR_TYPE_PALETTE) png_set_palette_to_rgb(png);
	if (color_type == PNG_COLOR_TYPE_GRAY) {
		if (bit_depth < 8) {
			png_set_expand_gray_1_2_4_to_8(png);
		}
		png_set_gray_to_rgb(png);
	}
	if (png_get_valid(png, info, PNG_INFO_tRNS)) {
		png_set_tRNS_to_alpha(png);
	}
	
	// TODO(skal): Strip Alpha for now (till Alpha is supported).
	png_set_strip_alpha(png);
	num_passes = png_set_interlace_handling(png);
	png_read_update_info(png, info);
	stride = 3 * width * sizeof(*rgb);
	rgb = (uint8_t*)malloc(stride * height);
	if (rgb == NULL) goto Error;
	for (p = 0; p < num_passes; ++p) {
		for (y = 0; y < height; ++y) {
			png_bytep row = rgb + y * stride;
			png_read_rows(png, &row, NULL, 1);
		}
	}
	png_read_end(png, info);
	png_destroy_read_struct(&png, &info, NULL);
	
	pic->width = width;
	pic->height = height;
	ok = WebPPictureImportRGB(pic, rgb, stride);
	free(rgb);
	
End:
	return ok;
}


static int MyWriter(const uint8_t* data, size_t data_size,
                    const WebPPicture* const pic) {
	FILE* const out = (FILE*)pic->custom_ptr;
	return data_size ? (fwrite(data, data_size, 1, out) == 1) : 1;
}


JNIEXPORT jint JNICALL
Java_phoneSat_code_ImagePacketizer_encodeWrapper(JNIEnv * env, jobject obj, 
												jstring pngTileName, jstring encodedTileName,
												jint quality, jint targetSize)
{
	// Setup a config, starting form a preset and tuning some additional
	// parameters
	WebPConfig config;
	WebPPicture picture;
	WebPAuxStats stats;
	FILE * out = NULL;
	FILE * in_file = NULL;

	if (!WebPPictureInit(&picture) || !WebPConfigInit(&config)) {
		LOGE("WebP Init VERSION ERROR\n");
		goto Error;
	}
	

	// ... additional tuning
	config.quality = quality;
	config.target_size = targetSize;
	if (!WebPValidateConfig(&config)) {  // not mandartory, but useful
		LOGE("WebP Invalid params\n");
		goto Error;
	}
	
	// Open image to be compressed
	const char *pngStr = (*env)->GetStringUTFChars(env, pngTileName, 0);
	in_file = fopen(pngStr, "rb");
	if (in_file == NULL) {
		LOGE("Error! Cannot open input file %s\n", pngStr);
		(*env)->ReleaseStringUTFChars(env, pngTileName, pngStr);
		goto Error;
	}
	(*env)->ReleaseStringUTFChars(env, pngTileName, pngStr);
	
	// Read in image
	if (!ReadPNG(in_file, &picture)) {
		LOGE("WebP Error: Could read in input image.\n");
		goto Error;
	}
	
	// Open output image
	const char *encodeStr = (*env)->GetStringUTFChars(env, encodedTileName, 0);
	out = fopen(encodeStr, "wb");
	if (!out) {
	
		LOGE("Error! Cannot open input file %s\n", encodeStr);
		(*env)->ReleaseStringUTFChars(env, encodedTileName, encodeStr);
		LOGE("Couldn't open encodeTile image\n");
		goto Error;
	}
	(*env)->ReleaseStringUTFChars(env, encodedTileName, encodeStr);
	
	picture.writer = MyWriter;
	picture.custom_ptr = (void*)out;
	picture.stats = &stats;
	
	// Encode image and write to output file
	if (!WebPEncode(&config, &picture)) {
		LOGE("Error! Cannot encode picture as WebP\n");
		goto Error;
	}
	
Error:
	free(picture.extra_info);
	WebPPictureFree(&picture);
	if (out != NULL) {
		fclose(out);
	}
	
	if (in_file != NULL) {
		fclose(in_file);
	}
	
	return 0;
}
