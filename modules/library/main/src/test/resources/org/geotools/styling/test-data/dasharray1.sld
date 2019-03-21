<?xml version="1.0" encoding="UTF-8"?>
   <!--
      edited with XMLSPY v5 rel. 3 U (http://www.xmlspy.com) by Robert
      Hibbert (Penn State Univ)
   -->
   <!--
      Sample XML file generated by XMLSPY v5 rel. 3 U
      (http://www.xmlspy.com)
   -->
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
   xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.opengis.net/sld
F:\projects\schema\sld\1.0.0\StyledLayerDescriptor.xsd"
   version="1.0.0">
   <Name>My Layer</Name>
   <Title>A layer by me</Title>
   <Abstract>this is a sample layer</Abstract>
   <UserLayer>
      <LayerFeatureConstraints>
         <FeatureTypeConstraint />
      </LayerFeatureConstraints>
      <UserStyle>
         <Name>My User Style</Name>
         <Title>A style by me</Title>
         <Abstract>this is a sample style</Abstract>
         <IsDefault>true</IsDefault>
         <FeatureTypeStyle>
            <Rule>
               <LineSymbolizer>
                  <Stroke>
                     <CssParameter name="stroke">#000044</CssParameter>
                     <CssParameter name="stroke-width">3</CssParameter>
                     <CssParameter name="stroke-offset">0</CssParameter>
                     <CssParameter name="stroke-dasharray">
                         2.0  1.0  4.0  1.0 
                     </CssParameter>
                  </Stroke>
               </LineSymbolizer>

            </Rule>
         </FeatureTypeStyle>
      </UserStyle>
   </UserLayer>
</StyledLayerDescriptor>