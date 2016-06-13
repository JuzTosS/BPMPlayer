#include <jni.h>
#include <SuperpoweredAnalyzer.h>
#include <SuperpoweredDecoder.h>
#include <android/log.h>
#include <malloc.h>
#include <SuperpoweredSimple.h>

extern "C" JNIEXPORT jdouble Java_com_juztoss_bpmplayer_audio_BpmDetector_detect(JNIEnv *env,
                                                                              jobject instance,
                                                                              jstring source
                                                                              ) {

    const char *path = env->GetStringUTFChars(source, JNI_FALSE);
    SuperpoweredDecoder *decoder = new SuperpoweredDecoder();
    const char *openError = decoder->open(path, false, 0, 0);

    if (openError) {
        __android_log_print(ANDROID_LOG_ERROR, __func__, " Decoder error, path %s, error: %s", path, openError);
        delete decoder;
        env->ReleaseStringUTFChars(source, path);
        return -1;
    };
    env->ReleaseStringUTFChars(source, path);

    const int samplesToDetect = 2097152; //The pow of 2 in case the fourier transform inside of the detector need it
    int64_t duration = decoder->durationSamples;
    int64_t start = duration / 2 - samplesToDetect / 2;
    int64_t end = start + samplesToDetect;

    if(start <= 0)
    {
        start = 0;
        end = decoder->durationSamples;
    }

    decoder->seekTo(start, false);
    SuperpoweredOfflineAnalyzer * analyzer = new SuperpoweredOfflineAnalyzer(decoder->samplerate, 0, decoder->durationSeconds);

    // Create a buffer for the 16-bit integer samples coming from the decoder.
    short int *intBuffer = (short int *)malloc(decoder->samplesPerFrame * 2 * sizeof(short int) + 16384);
    // Create a buffer for the 32-bit floating point samples required by the effect.
    float *floatBuffer = (float *)malloc(decoder->samplesPerFrame * 2 * sizeof(float) + 1024);

    // Processing.
    while (decoder->samplePosition <= end) {
        // Decode one frame. samplesDecoded will be overwritten with the actual decoded number of samples.
        unsigned int samplesDecoded = decoder->samplesPerFrame;
        if (decoder->decode(intBuffer, &samplesDecoded) == SUPERPOWEREDDECODER_ERROR) break;
        if (samplesDecoded < 1) break;

        // Convert the decoded PCM samples from 16-bit integer to 32-bit floating point.
        SuperpoweredShortIntToFloat(intBuffer, floatBuffer, samplesDecoded);

        // Submit samples to the analyzer.
        analyzer->process(floatBuffer, samplesDecoded);
    };

    // Get the result.
    unsigned char *averageWaveform = NULL, *lowWaveform = NULL, *midWaveform = NULL, *highWaveform = NULL, *peakWaveform = NULL, *notes = NULL;
    int waveformSize, keyIndex;
    char *overviewWaveform = NULL;
    float loudpartsAverageDecibel, peakDecibel, bpm, averageDecibel, beatgridStartMs = 0;
    analyzer->getresults(&averageWaveform, &peakWaveform, &lowWaveform, &midWaveform, &highWaveform, &notes, &waveformSize, &overviewWaveform, &averageDecibel, &loudpartsAverageDecibel, &peakDecibel, &bpm, &beatgridStartMs, &keyIndex);

    // Cleanup.
    delete decoder;
    delete analyzer;
    free(intBuffer);
    free(floatBuffer);

    // Done with the result, free memory.
    if (averageWaveform) free(averageWaveform);
    if (lowWaveform) free(lowWaveform);
    if (midWaveform) free(midWaveform);
    if (highWaveform) free(highWaveform);
    if (peakWaveform) free(peakWaveform);
    if (notes) free(notes);
    if (overviewWaveform) free(overviewWaveform);

    return bpm;
}