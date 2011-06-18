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
package org.openimaj.image.processing.convolution;

import org.openimaj.image.FImage;
import org.openimaj.image.processor.KernelProcessor;

/**
 * A set of standard derivative kernels. These kernels help estimate the derivative over various orders at a point in a matrix. 
 * This is approximated by applying a finite difference derivative operation on a gaussian kernel with a very low sigma. i.e. a gaussian
 * kernel that looks like:
 * 
 * [
 * 	[0,0,0],
 *  [0,1,0],
 *  [0,0,0]
 * ]
 * 
 * By successive derivative calculations in the x direction and y direction it is possible to estimate derivatives in both directions as well.
 * 
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>
 *
 */
public class BasicDerivativeKernels {
	static class DxKernel extends AbstractFConvolution {
		public DxKernel() { super(new FImage(new float[][] {{-0.5f,0,0.5f}})); }
	}
	
	static class DyKernel extends AbstractFConvolution {
		public DyKernel() { super(new FImage(new float[][] {{-0.5f}, {0}, {0.5f}})); }
	}

	static class DxxKernel extends AbstractFConvolution {
		public DxxKernel() { super(new FImage(new float[][] {{1,-2,1}})); }
	}
	
	static class DxyKernel extends AbstractFConvolution {
		public DxyKernel() { super(new FImage(new float[][] {{0.25f,0,-0.25f}, {0,0,0}, {-0.25f,0,0.25f}})); }
	}
	
	static class DyyKernel extends AbstractFConvolution {
		public DyyKernel() { super(new FImage(new float[][] {{1}, {-2}, {1}})); }
	}
	
	static class DxxxxKernel extends AbstractFConvolution {
		public DxxxxKernel() { super(new FImage(new float[][] {{1,-4 ,6 ,-4 ,1}})); }
	}
	
	static class DyyyyKernel extends AbstractFConvolution {
		public DyyyyKernel() { super(new FImage(new float[][] {{1}, {-4},{6},{-4},{1}})); }
	}
	
	static class DxxyyKernel extends AbstractFConvolution {
		public DxxyyKernel() { super(new FImage(new float[][] {{1f,-2f,1f},{-2f,4f,-2f},{1f,-2f,1f}})); }
	}
	
	/**
	 * kernel approximating the first derivative of a low-sigma gaussian in the x-direction [-0.5, 0, 0.5]. 
	 * Useful for giving an estimate of the second derivative in x of any given point
	 */
	public static final KernelProcessor<Float, FImage> DX_KERNEL  = new DxKernel();
	
	/**
	 * kernel approximating the first derivative of a low-sigma gaussian in the y-direction [-0.5, 0, 0.5]'. 
	 * Useful for giving an estimate of the second derivative in y of any given point
	 */
	public static final KernelProcessor<Float, FImage> DY_KERNEL  = new DyKernel();
	
	/**
	 * kernel approximating the second derivative of a low sigma gaussian in the x-direction [1, -2, 1]. 
	 * Useful for giving an estimate of the second derivative in x of any given point
	 */
	public static final KernelProcessor<Float, FImage> DXX_KERNEL = new DxxKernel();
	
	/**
	 * kernel approximating the first derivative of a low sigma gaussian in the x-direction and y-direction [[-0.25, 0, 0.25], [0, 0, 0], [0.25, 0, -0.25]] . 
	 * Useful for giving an estimate of the first order derivative in x then y of any given point
	 */
	public static final KernelProcessor<Float, FImage> DXY_KERNEL = new DxyKernel();
	
	/**
	 * kernel approximating the second derivative of a low sigma gaussian in the y-direction [1, -2, 1]'. 
	 * Useful for giving an estimate of the second derivative in y of any given point
	 */
	public static final KernelProcessor<Float, FImage> DYY_KERNEL = new DyyKernel();
	
	
	/**
	 * kernel approximating the fourth derivative of a low sigma gaussian in the x-direction [1,-4,6,-4,1]^T
	 * Useful for giving an estimate of the fourth derivative in y of any given point
	 */
	public static final KernelProcessor<Float, FImage> DXXXX_KERNEL = new DxxxxKernel();
	
	/**
	 * kernel approximating the second derivative of a low sigma gaussian in the x-direction and y-direction [[1,-2,1],[-2,4,-2],[1,-2,1]] . 
	 * Useful for giving an estimate of the second order derivative in x then y of any given point
	 */
	public static final KernelProcessor<Float, FImage> DXXYY_KERNEL = new DxxyyKernel();
	/**
	 * kernel approximating the fourth derivative of a low sigma gaussian in the y-direction [1,-4,6,-4,1]^T
	 * Useful for giving an estimate of the fourth derivative in y of any given point
	 */
	public static final KernelProcessor<Float, FImage> DYYYY_KERNEL = new DyyyyKernel();
}
