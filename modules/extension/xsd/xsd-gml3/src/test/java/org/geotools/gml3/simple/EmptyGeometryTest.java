/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2026, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gml3.simple;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.geotools.geometry.jts.WKTReader2;
import org.geotools.gml3.GML;
import org.junit.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.w3c.dom.Document;

/** Empty geometries are valid and must encode without error, as an element with no positions. */
public class EmptyGeometryTest extends GeometryEncoderTestSupport {

    @Test
    public void testEncodeEmptyLineString() throws Exception {
        LineStringEncoder encoder = new LineStringEncoder(gtEncoder, "gml", GML.NAMESPACE);
        LineString geometry = (LineString) new WKTReader2().read("LINESTRING EMPTY");
        Document doc = encode(encoder, geometry, "empty");
        assertThat(doc, hasXPath("//gml:LineString/gml:posList", equalTo("")));
        assertThat(doc, hasXPath("//gml:LineString/@gml:id", equalTo("empty")));
    }

    @Test
    public void testEncodeEmptyPoint() throws Exception {
        PointEncoder encoder = new PointEncoder(gtEncoder, "gml", GML.NAMESPACE);
        Point geometry = (Point) new WKTReader2().read("POINT EMPTY");
        Document doc = encode(encoder, geometry, "empty");
        assertThat(doc, hasXPath("//gml:Point/gml:pos", equalTo("")));
    }

    @Test
    public void testEncodeEmptyPolygon() throws Exception {
        PolygonEncoder encoder = new PolygonEncoder(gtEncoder, "gml", GML.NAMESPACE);
        Polygon geometry = (Polygon) new WKTReader2().read("POLYGON EMPTY");
        Document doc = encode(encoder, geometry, "empty");
        assertThat(
                doc,
                hasXPath("//gml:Polygon/gml:exterior/gml:LinearRing/gml:posList", equalTo("")));
    }
}
