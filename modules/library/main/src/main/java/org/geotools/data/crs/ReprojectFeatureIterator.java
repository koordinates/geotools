/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.crs;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

/**
 * ReprojectFeatureReader provides a reprojection for FeatureTypes.
 *
 * <p>ReprojectFeatureReader is a wrapper used to reproject GeometryAttributes to a user supplied
 * CoordinateReferenceSystem from the original CoordinateReferenceSystem supplied by the original
 * FeatureReader.
 *
 * <p>Example Use:
 *
 * <pre><code>
 * ReprojectFeatureReader reader =
 *     new ReprojectFeatureReader( originalReader, reprojectCS );
 *
 * CoordinateReferenceSystem originalCS =
 *     originalReader.getFeatureType().getDefaultGeometry().getCoordinateSystem();
 *
 * CoordinateReferenceSystem newCS =
 *     reader.getFeatureType().getDefaultGeometry().getCoordinateSystem();
 *
 * assertEquals( reprojectCS, newCS );
 * </code></pre>
 *
 * TODO: handle the case where there is more than one geometry and the other geometries have a
 * different CS than the default geometry
 *
 * @author jgarnett, Refractions Research, Inc.
 * @author aaime
 * @author $Author: jive $ (last modification)
 * @version $Id$
 */
public class ReprojectFeatureIterator implements Iterator<SimpleFeature>, SimpleFeatureIterator {
    FeatureIterator<SimpleFeature> reader;
    SimpleFeatureType schema;
    GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();

    public ReprojectFeatureIterator(
            FeatureIterator<SimpleFeature> reader,
            SimpleFeatureType schema,
            MathTransform transform) {
        this.reader = reader;
        this.schema = schema;
        transformer.setMathTransform(transform);

        // set hte target coordinate system
        transformer.setCoordinateReferenceSystem(schema.getCoordinateReferenceSystem());
    }

    /**
     * Implement getFeatureType.
     *
     * <p>Description ...
     *
     * @see FeatureReader#getFeatureType()
     */
    public SimpleFeatureType getFeatureType() {
        if (schema == null) {
            throw new IllegalStateException("Reader has already been closed");
        }

        return schema;
    }

    /**
     * Implement next.
     *
     * <p>Description ...
     *
     * @see FeatureReader#next()
     */
    @Override
    public SimpleFeature next() throws NoSuchElementException {
        if (reader == null) {
            throw new IllegalStateException("Reader has already been closed");
        }

        // grab the next feature
        SimpleFeature next = reader.next();

        Object[] attributes = next.getAttributes().toArray();

        try {
            for (int i = 0; i < attributes.length; i++) {
                if (attributes[i] instanceof Geometry) {
                    attributes[i] = transformer.transform((Geometry) attributes[i]);
                }
            }
        } catch (TransformException e) {
            throw (IllegalStateException)
                    new IllegalStateException(
                                    "A transformation exception occurred while reprojecting data on the fly")
                            .initCause(e);
        }

        try {
            return SimpleFeatureBuilder.build(schema, attributes, next.getID());
        } catch (Exception e) {
            throw new RuntimeException(
                    e.getMessage()
                            + "\n-----\n"
                            + toString(schema)
                            + "\n-----\n"
                            + toString(attributes),
                    e);
        }
    }

    private String toString(Object[] attributes) {
        String result = "[";
        for (Object attr : attributes) {
            result += attr + ",";
        }
        return result.substring(0, result.length() - 1) + "]";
    }

    private static String toString(SimpleFeatureType schema) {
        return String.join("\n", toStringArray(schema.getAttributeDescriptors()));
    }

    private static String[] toStringArray(List<AttributeDescriptor> descriptors) {
        String[] result = new String[descriptors.size()];
        for (int i = 0; i < descriptors.size(); i++) {
            result[i] = toString(descriptors.get(i));
        }
        return result;
    }

    private static String toString(AttributeDescriptor descriptor) {
        if (descriptor instanceof GeometryDescriptor) {
            return toString((GeometryDescriptor) descriptor);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("AttributeDescriptor(");
        sb.append(descriptor.getName() + ", ");
        sb.append(getOccursStr(descriptor) + ", ");
        sb.append("binding=" + descriptor.getType().getBinding().getSimpleName() + ")");
        return sb.toString();
    }

    private static String toString(GeometryDescriptor descriptor) {
        StringBuilder sb = new StringBuilder("GeometryDescriptor(");
        sb.append(descriptor.getName() + ", ");
        sb.append(getOccursStr(descriptor) + ", ");
        sb.append("binding=" + descriptor.getType().getBinding().getSimpleName() + ", ");
        sb.append("CRS=" + CRS.toSRS(descriptor.getCoordinateReferenceSystem()) + ")");
        return sb.toString();
    }

    private static String getOccursStr(AttributeDescriptor d) {
        return String.format(
                "(%d,%d%s)", d.getMinOccurs(), d.getMaxOccurs(), (d.isNillable() ? ",nil" : ""));
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("On the fly reprojection disables remove");
    }
    /**
     * Implement hasNext.
     *
     * <p>Description ...
     *
     * @see FeatureReader#hasNext()
     */
    @Override
    public boolean hasNext() {
        if (reader == null) {
            throw new IllegalStateException("Reader has already been closed");
        }

        return reader.hasNext();
    }

    /**
     * Implement close.
     *
     * <p>Description ...
     *
     * @see FeatureReader#close()
     */
    @Override
    public void close() {
        if (reader == null) {
            return;
        }
        reader.close();
        reader = null;
        schema = null;
    }
}
