package org.openimaj.ml.clustering.spectral;



import java.util.Iterator;

import gov.sandia.cognition.math.ComplexNumber;
import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.mtj.DenseMatrix;
import gov.sandia.cognition.math.matrix.mtj.DenseMatrixFactoryMTJ;
import gov.sandia.cognition.math.matrix.mtj.SparseMatrix;
import gov.sandia.cognition.math.matrix.mtj.decomposition.EigenDecompositionRightMTJ;

import org.openimaj.ml.clustering.SimilarityClusterer;
import org.openimaj.ml.clustering.SpatialClusters;
import org.openimaj.ml.clustering.TrainingIndexClusters;
import org.openimaj.util.pair.DoubleObjectPair;
import org.openimaj.util.queue.BoundedPriorityQueue;

/**
 * Built from a mixture of this tutorial: 
 * 	- http://www.kyb.mpg.de/fileadmin/user_upload/files/publications/attachments/Luxburg07_tutorial_4488%5B0%5D.pdf
 * And this implementation:
 *  - https://github.com/peterklipfel/AutoponicsVision/blob/master/SpectralClustering.java
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class DoubleNormalisedSpectralClustering implements SimilarityClusterer<Clusters>{
	
	private SpectralClusteringConf<double[]> conf;
	
	/**
	 * @param conf  
	 * cluster the eigen vectors
	 */
	public DoubleNormalisedSpectralClustering(SpectralClusteringConf<double[]> conf) {
		this.conf = conf;
	}

	@Override
	public Clusters cluster(SparseMatrix data, boolean distanceMode) {
		// Compute the laplacian of the graph
		DenseMatrix laplacian = DenseMatrixFactoryMTJ.INSTANCE.copyMatrix(conf.laplacian.laplacian(data));
		// Calculate the eigvectors
		EigenDecompositionRightMTJ eig = EigenDecompositionRightMTJ.create(laplacian);
		// Use the lowest eigen valued cols as the features, each row is a data item in the reduced feature space
		// Also normalise each row
		double[][] lowestCols = lowestCols(eig);
		
		// Cluster the rows with the internal spatial clusterer
		SpatialClusters<double[]> cluster = conf.internal.cluster(lowestCols);
		// if the clusters contain the cluster indexes of the training examples use those
		if(cluster instanceof TrainingIndexClusters){
			return new Clusters(((TrainingIndexClusters)cluster).clusters());
		}
		// Otherwise attempt to assign values to clusters
		int[] clustered = cluster.defaultHardAssigner().assign(lowestCols);
		// done!
		return new Clusters(clustered);
	}
	private double[][] lowestCols(EigenDecompositionRightMTJ eig) {
		
		ComplexNumber[] vals = eig.getEigenValues();
		int eigenVectorSelect = conf.eigenChooser.nEigenVectors(this.conf.laplacian.eigenIterator(eig),vals.length);
		
		
		Matrix vects = eig.getEigenVectorsRealPart();
		int nrows = vects.getColumn(0).getDimensionality();
		double[][] ret = new double[nrows][eigenVectorSelect];
		double[] retSum = new double[nrows];
		
		int col = 0;
		// Calculate U matrix (containing n smallests eigen valued columns)
		for (Iterator<DoubleObjectPair<Vector>> iterator = this.conf.laplacian.eigenIterator(eig); iterator.hasNext();) {
			DoubleObjectPair<Vector> v = iterator.next();
			System.out.println(v.first);
			for (int i = 0; i < v.second.getDimensionality(); i++) {
				double elColI = v.second.getElement(i);
				ret[i][col] = elColI;
				retSum[i] += elColI * elColI;
			}
			col++;
			if(col == eigenVectorSelect) break;
		}
		
		// normalise rows
		for (int i = 0; i < ret.length; i++) {
			double[] row = ret[i];
			for (int j = 0; j < row.length; j++) {
				row[j] /= Math.sqrt(retSum[i]);
			}
		}
		
		return ret;
	}

}