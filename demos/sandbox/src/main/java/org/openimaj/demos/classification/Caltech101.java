package org.openimaj.demos.classification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.openimaj.data.DataUtils;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.data.identity.Identifiable;
import org.openimaj.image.Image;
import org.openimaj.image.ImageProvider;
import org.openimaj.image.ImageUtilities;
import org.openimaj.io.InputStreamObjectReader;
import org.openimaj.io.ObjectReader;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Polygon;
import org.openimaj.math.geometry.shape.Rectangle;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLDouble;

public class Caltech101 {
	private static final String IMAGES_ZIP = "Caltech101/101_ObjectCategories.zip";
	private static final String IMAGES_DOWNLOAD_URL = "http://datasets.openimaj.org/Caltech101/101_ObjectCategories.zip";
	private static final String ANNOTATIONS_ZIP = "Caltech101/Annotations.zip";
	private static final String ANNOTATIONS_DOWNLOAD_URL = "http://datasets.openimaj.org/Caltech101/Annotations.zip";

	/**
	 * Get a dataset of the Caltech 101 images. If the dataset hasn't been
	 * downloaded, it will be fetched automatically and stored in the OpenIMAJ
	 * data directory. The images in the dataset are grouped by their class.
	 * 
	 * @see DataUtils#getDataDirectory()
	 * 
	 * @param reader
	 * @return a dataset of images
	 * @throws IOException
	 *             if a problem occurs loading the dataset
	 */
	public static <IMAGE extends Image<?, IMAGE>> VFSGroupDataset<IMAGE> getImages(InputStreamObjectReader<IMAGE> reader)
			throws IOException
	{
		return new VFSGroupDataset<IMAGE>(downloadAndGetImagePath(), reader);
	}

	private static String downloadAndGetImagePath() throws IOException {
		final File dataset = DataUtils.getDataLocation(IMAGES_ZIP);

		if (!(dataset.exists())) {
			dataset.getParentFile().mkdirs();
			FileUtils.copyURLToFile(new URL(IMAGES_DOWNLOAD_URL), dataset);
		}

		return "zip:file:" + dataset.toString() + "!101_ObjectCategories/";
	}

	private static String downloadAndGetAnnotationPath() throws IOException {
		final File dataset = DataUtils.getDataLocation(ANNOTATIONS_ZIP);

		if (!(dataset.exists())) {
			dataset.getParentFile().mkdirs();
			FileUtils.copyURLToFile(new URL(ANNOTATIONS_DOWNLOAD_URL), dataset);
		}

		return "zip:file:" + dataset.toString() + "!Annotations/";
	}

	public static abstract class Record<IMAGE extends Image<?, IMAGE>> implements Identifiable, ImageProvider<IMAGE> {
		private Rectangle bounds;
		private Polygon contour;
		private String id;
		private String objectClass;

		protected Record(FileObject image) throws FileSystemException, IOException {
			final FileSystemManager fsManager = VFS.getManager();
			final FileObject imagesBase = fsManager.resolveFile(downloadAndGetImagePath());
			final FileObject annotationsBase = fsManager.resolveFile(downloadAndGetAnnotationPath());

			// get the id
			id = imagesBase.getName().getRelativeName(image.getName());

			// the class
			objectClass = image.getParent().getName().getBaseName();

			// find the annotation file
			final String annotationFileName = id.replace("image_", "annotation_").replace(".jpg", ".mat");
			final FileObject annotationFile = annotationsBase.resolveFile(annotationFileName);
			parseAnnotations(annotationFile);
		}

		private void parseAnnotations(FileObject annotationFile) throws IOException {
			if (!annotationFile.exists()) {
				return;
			}

			final MatFileReader reader = new MatFileReader(annotationFile.getContent().getInputStream());

			final MLDouble boxes = (MLDouble) reader.getMLArray("box_coord");
			this.bounds = new Rectangle(
					(float) (double) boxes.getReal(2) - 1,
					(float) (double) boxes.getReal(0) - 1,
					(float) (boxes.getReal(3) - boxes.getReal(2)) - 1,
					(float) (boxes.getReal(1) - boxes.getReal(0)) - 1);

			final double[][] contourData = ((MLDouble) reader.getMLArray("obj_contour")).getArray();
			this.contour = new Polygon();
			for (int i = 0; i < contourData[0].length; i++) {
				contour.points.add(
						new Point2dImpl((float) contourData[0][i] + bounds.x - 1,
								(float) contourData[1][i] + bounds.y - 1)
						);
			}
			contour.close();
		}

		@Override
		public String getID() {
			return id;
		}

		/**
		 * @return the bounds
		 */
		public Rectangle getBounds() {
			return bounds;
		}

		/**
		 * @return the contour
		 */
		public Polygon getContour() {
			return contour;
		}

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @return the class
		 */
		public String getObjectClass() {
			return objectClass;
		}
	}

	public static class RecordReader<IMAGE extends Image<?, IMAGE>> implements ObjectReader<Record<IMAGE>, FileObject> {
		private VFSListDataset.FileObjectISReader<IMAGE> imageReader;

		public RecordReader(InputStreamObjectReader<IMAGE> reader) {
			this.imageReader = new VFSListDataset.FileObjectISReader<IMAGE>(reader);
		}

		@Override
		public Record<IMAGE> read(final FileObject source) throws IOException {
			return new Record<IMAGE>(source) {

				@Override
				public IMAGE getImage() {
					try {
						return imageReader.read(source);
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}
			};
		}

		@Override
		public boolean canRead(FileObject source, String name) {
			InputStream stream = null;
			try {
				stream = source.getContent().getInputStream();

				return ImageUtilities.FIMAGE_READER.canRead(stream, source.getName().getBaseName());
			} catch (final FileSystemException e) {
				return false;
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (final IOException e) {
					}
				}
			}
		}
	}

	public static <IMAGE extends Image<?, IMAGE>> VFSGroupDataset<Record<IMAGE>> getData(
			InputStreamObjectReader<IMAGE> reader)
			throws IOException
	{
		return new VFSGroupDataset<Record<IMAGE>>(downloadAndGetImagePath(), new RecordReader<IMAGE>(reader));
	}
}
