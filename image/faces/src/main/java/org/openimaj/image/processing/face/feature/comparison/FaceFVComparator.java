/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.image.processing.face.feature.comparison;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.openimaj.feature.FVComparator;
import org.openimaj.feature.FeatureVector;
import org.openimaj.feature.FeatureVectorProvider;
import org.openimaj.image.processing.face.feature.FacialFeature;
import org.openimaj.io.IOUtils;

/**
 * A generic {@link FacialFeatureComparator} for {@link FacialFeature}s that can
 * provide {@link FeatureVector}s through the {@link FeatureVectorProvider}
 * interface. Any compatible {@link FVComparator} can be used to compare
 * features.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 * @param <T>
 *            Type of feature
 * @param <Q>
 *            Type of featurevector
 */
public class FaceFVComparator<T extends FacialFeature & FeatureVectorProvider<Q>, Q extends FeatureVector>
		implements
			FacialFeatureComparator<T>
{
	FVComparator<Q> comp;

	/**
	 * Construct with the given {@link FVComparator}
	 * 
	 * @param comp
	 *            the comparison technique
	 */
	public FaceFVComparator(FVComparator<Q> comp) {
		this.comp = comp;
	}

	@Override
	public double compare(T query, T target) {
		return comp.compare(query.getFeatureVector(), target.getFeatureVector());
	}

	@Override
	public boolean isDistance() {
		return comp.isDistance();
	}

	@Override
	public void readBinary(DataInput in) throws IOException {
		comp = IOUtils.read(in);
	}

	@Override
	public byte[] binaryHeader() {
		return this.getClass().getName().getBytes();
	}

	@Override
	public void writeBinary(DataOutput out) throws IOException {
		IOUtils.write(comp, out);
	}

	@Override
	public String toString() {
		return "FaceFVComparator[distance=" + comp + "]";
	}
}
