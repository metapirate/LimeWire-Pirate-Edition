package org.limewire.nio.channel;

import java.nio.channels.ScatteringByteChannel;

/**
 * Defines an interface that is an interest read channel and supports
 * scattering reads.
 */
public interface InterestScatteringByteChannel extends InterestReadableByteChannel,
ScatteringByteChannel{}
