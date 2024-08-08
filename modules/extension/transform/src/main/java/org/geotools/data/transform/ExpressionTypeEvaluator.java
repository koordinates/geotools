/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.transform;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Add;
import org.geotools.api.filter.expression.BinaryExpression;
import org.geotools.api.filter.expression.Divide;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.ExpressionVisitor;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.Multiply;
import org.geotools.api.filter.expression.NilExpression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.expression.Subtract;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.function.FilterFunction_Convert;

import org.geotools.filter.AttributeExpressionImpl;

import java.util.List;
import org.geotools.referencing.CRS;

/**
 * Utility class that tries to figure out the resulting type of an expression against a given
 * feature type by using static analysis.
 *
 * @author Andrea Aime - GeoSolutions
 */
class ExpressionTypeEvaluator implements ExpressionVisitor {

    private SimpleFeatureType schema;
    private CoordinateReferenceSystem crs;

    public ExpressionTypeEvaluator(SimpleFeatureType schema) {
        this.schema = schema;
    }

    /**
     * Returns the coordinate reference system of the last encontered geometry property. Unless a
     * filter function that reprojects geometries is used, that's also the crs of the eventual
     * output, in case it's a Geometry, that is.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return this.crs;
    }

    @Override
    public Object visit(NilExpression expression, Object extraData) {
        return null;
    }

    @Override
    public Object visit(Function f, Object extraData) {
        FunctionName fn = f.getFunctionName();
        if (fn != null && fn.getReturn() != null && fn.getReturn().getType() != Object.class) {
            return fn.getReturn().getType();
        } else if (f instanceof FilterFunction_Convert) {
            // special case for the convert function, which has the return type as
            // a parameter
            return f.getParameters().get(1).evaluate(null, Class.class);
        }

        return null;
    }

    @Override
    public Object visit(Literal expression, Object extraData) {
        if (expression.getValue() == null) {
            return null;
        } else {
            return expression.getValue().getClass();
        }
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
    public Object visit(PropertyName expression, Object extraData) {
        if (expression instanceof AttributeExpressionImpl) {
            ((AttributeExpressionImpl) expression).setLenient(false);
        }
        AttributeDescriptor result = expression.evaluate(schema, AttributeDescriptor.class);
        if (result == null) {
            throw new IllegalArgumentException(
                    "Original feature type does not have a property named "
                            + expression.getPropertyName() + "\n" + toString(schema));
        }

        if (result instanceof GeometryDescriptor) {
            this.crs = ((GeometryDescriptor) result).getCoordinateReferenceSystem();
        }
        return result.getType().getBinding();
    }

    private Object visitMathExpression(BinaryExpression expression) {
        Expression ex1 = expression.getExpression1();
        Expression ex2 = expression.getExpression2();

        Class c1 = getMathOperandType(ex1);
        Class c2 = getMathOperandType(ex2);

        if (c1 == Integer.class && c2 == Integer.class) {
            return Integer.class;
        } else if ((c1 == Integer.class || c1 == Long.class)
                && (c2 == Integer.class || c2 == Long.class)) {
            return Long.class;
        } else {
            return Double.class;
        }
    }

    private Class getMathOperandType(Expression expression) {
        Class result = (Class) expression.accept(this, null);

        // not a number, if a literal see if its contents can be cast to one
        if (!(Number.class.isAssignableFrom(result)) && expression instanceof Literal) {
            Double value = expression.evaluate(null, Double.class);
            if (value != null) {
                if (value.longValue() == value.doubleValue()) {
                    // integer type, keep it simple, int or long
                    if ((value < Integer.MAX_VALUE) && value > Integer.MIN_VALUE) {
                        result = Integer.class;
                    } else {
                        result = Long.class;
                    }
                }
            } else {
                result = Double.class;
            }
        }

        return result;
    }

    @Override
    public Object visit(Multiply expression, Object extraData) {
        return visitMathExpression(expression);
    }

    @Override
    public Object visit(Add expression, Object extraData) {
        return visitMathExpression(expression);
    }

    @Override
    public Object visit(Divide expression, Object extraData) {
        return visitMathExpression(expression);
    }

    @Override
    public Object visit(Subtract expression, Object extraData) {
        return visitMathExpression(expression);
    }
}
