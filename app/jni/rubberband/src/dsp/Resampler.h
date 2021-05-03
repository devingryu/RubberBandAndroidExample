/* -*- c-basic-offset: 4 indent-tabs-mode: nil -*- vi:set ts=8 sts=4 sw=4: */

/*
    Rubber Band Library
    An audio time-stretching and pitch-shifting library.
    Copyright 2007-2021 Particular Programs Ltd.

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License as
    published by the Free Software Foundation; either version 2 of the
    License, or (at your option) any later version.  See the file
    COPYING included with this distribution for more information.

    Alternatively, if you have a valid commercial licence for the
    Rubber Band Library obtained by agreement with the copyright
    holders, you may redistribute and/or modify it under the terms
    described in that licence.

    If you wish to distribute code using the Rubber Band Library
    under terms other than those of the GNU General Public License,
    you must obtain a valid commercial licence before doing so.
*/

#ifndef RUBBERBAND_RESAMPLER_H
#define RUBBERBAND_RESAMPLER_H

#include "system/sysutils.h"

namespace RubberBand {

class Resampler
{
public:
    enum Quality { Best, FastestTolerable, Fastest };
    enum Exception { ImplementationError };

    struct Parameters {

        /**
         * Resampler filter quality level.
         */
        Quality quality;

        /** 
         * Rate of expected input prior to resampling: may be used to
         * determine the filter bandwidth for the quality setting. If
         * you don't know what this will be, you can provide an
         * arbitrary rate (such as the default) and the resampler will
         * work fine, but quality may not be as designed.
         */
        double initialSampleRate;

        /** 
         * Bound on the maximum incount size that may be passed to the
         * resample function before the resampler needs to reallocate
         * its internal buffers. Default is zero, so that buffer
         * allocation will happen on the first call and any subsequent
         * call with a greater incount.
         */
        int maxBufferSize;

        /**
         * Debug output level, from 0 to 3. Controls the amount of
         * debug information printed to stderr.
         */
        int debugLevel;

        Parameters() :
            quality(FastestTolerable),
            initialSampleRate(44100),
            maxBufferSize(0),
            debugLevel(0) { }
    };
    
    /**
     * Construct a resampler to process the given number of channels,
     * with the given quality level, initial sample rate, and other
     * parameters.
     */
    Resampler(Parameters parameters, int channels);
    
    ~Resampler();

    /**
     * Resample the given multi-channel buffers, where incount is the
     * number of frames in the input buffers and outspace is the space
     * available in the output buffers. Generally you want outspace to
     * be at least ceil(incount * ratio).
     *
     * Returns the number of frames written to the output
     * buffers. This may be smaller than outspace even where the ratio
     * suggests otherwise, particularly at the start of processing
     * where there may be a filter tail to allow for.
     */
#ifdef __GNUC__
    __attribute__((warn_unused_result))
#endif
    int resample(float *const R__ *const R__ out,
                 int outspace,
                 const float *const R__ *const R__ in,
                 int incount,
                 double ratio,
                 bool final = false);

    /**
     * Resample the given interleaved buffer, where incount is the
     * number of frames in the input buffer (i.e. it has incount *
     * getChannelCount() samples) and outspace is the space available
     * in frames in the output buffer (i.e. it has space for at least
     * outspace * getChannelCount() samples). Generally you want
     * outspace to be at least ceil(incount * ratio).
     *
     * Returns the number of frames written to the output buffer. This
     * may be smaller than outspace even where the ratio suggests
     * otherwise, particularly at the start of processing where there
     * may be a filter tail to allow for.
     */
#ifdef __GNUC__
    __attribute__((warn_unused_result))
#endif
    int resampleInterleaved(float *const R__ out,
                            int outspace,
                            const float *const R__ in,
                            int incount,
                            double ratio,
                            bool final = false);

    int getChannelCount() const;

    void reset();

    class Impl;

protected:
    Impl *d;
    int m_method;
};

}

#endif
