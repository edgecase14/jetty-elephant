/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.coplanar.updatemsg.*;
import org.geolatte.geom.C2D;
import org.geolatte.geom.codec.CrsWktDecoder;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.CrsId;
import org.geolatte.geom.crs.CrsRegistry;
import org.geolatte.geom.crs.ProjectedCoordinateReferenceSystem;
import org.geolatte.geom.json.GeolatteGeomModule;
/**
 *
 * @author jjackson
 */
public class JsonCodec {
    // keep your fingers crossed https://stackoverflow.com/questions/3907929/should-i-declare-jacksons-objectmapper-as-a-static-field
    // ThreadLocal makes this an issue for Virtual Threads... can we make sure to only call this from Jetty worker pool (platform) threads?
    private static final ThreadLocal<ObjectMapper> theMapper = new ThreadLocal<ObjectMapper>() {

    @Override
    protected ObjectMapper initialValue() {
        ObjectMapper objectMapper = new ObjectMapper();
    //    objectMapper.registerSubtypes(net.coplanar.updatemsg.CellUpdate.class);
    // might want EPSG:22717
    // CoordinateReferenceSystem<G2D> crs=...;
    
        // not found!
        //CoordinateReferenceSystem<C2D> crs=CrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(22717);
        
                String wkt = "PROJCS[\"NAD83(CSRS)v7 / UTM zone 17N\"," +
    "GEOGCS[\"NAD83(CSRS)v7\"," +
        "DATUM[\"North_American_Datum_of_1983_CSRS_version_7\","+
            "SPHEROID[\"GRS 1980\",6378137,298.257222101,"+
                "AUTHORITY[\"EPSG\",\"7019\"]],"+
            "AUTHORITY[\"EPSG\",\"1198\"]],"+
        "PRIMEM[\"Greenwich\",0,"+
            "AUTHORITY[\"EPSG\",\"8901\"]],"+
        "UNIT[\"degree\",0.0174532925199433,"+
            "AUTHORITY[\"EPSG\",\"9122\"]],"+
        "AUTHORITY[\"EPSG\",\"8255\"]],"+
    "PROJECTION[\"Transverse_Mercator\"],"+
    "PARAMETER[\"latitude_of_origin\",0],"+
    "PARAMETER[\"central_meridian\",-81],"+
    "PARAMETER[\"scale_factor\",0.9996],"+
    "PARAMETER[\"false_easting\",500000],"+
    "PARAMETER[\"false_northing\",0],"+
    "UNIT[\"metre\",1,"+
        "AUTHORITY[\"EPSG\",\"9001\"]],"+
    "AXIS[\"Easting\",EAST],"+
    "AXIS[\"Northing\",NORTH],"+
    "AUTHORITY[\"EPSG\",\"22717\"]]";

        CrsWktDecoder decoder = new CrsWktDecoder();   
        CoordinateReferenceSystem<?> crs = decoder.decode(wkt, 22717);
        
        CrsId crsId = CrsId.parse("EPSG:22717");
        //CoordinateReferenceSystem<C2D> crs = CoordinateReferenceSystem.;
        //CrsDefinition crsDefinition = new CrsDefinition(crsId, wkt);
        CrsRegistry.registerCoordinateReferenceSystem(crsId, crs);
        
        System.out.println("CRS geolatte: " + crs);
  //CoordinateReferenceSystem<C3D> crsZ=crs.addVerticalSystem(LinearUnit.METER,C3D.class);
  //CoordinateReferenceSystem<C3DM> crsZM=crsZ.addLinearSystem(LinearUnit.METER,C3DM.class);
        objectMapper.registerModule(new GeolatteGeomModule(crs));
        return objectMapper;
    }
    };
    
    
    //private static final ThreadLocal<ObjectMapper> theMapper = ThreadLocal.withInitial(ObjectMapper::new); // create once, reuse
    
    static final UpdateMessage decode(String json) throws JsonProcessingException {
        ObjectMapper mapper = theMapper.get();
        UpdateMessage value = mapper.readValue(json, UpdateMessage.class);
        return value;
    }
    
    static final String encode(UpdateMessage msgObj) throws JsonProcessingException {
        ObjectMapper mapper = theMapper.get();
        String jsonString = mapper.writeValueAsString(msgObj);
        return jsonString;
    }
}
