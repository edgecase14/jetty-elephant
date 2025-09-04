export class PointAdd {
    proj_id;
    point;
}

import proj4 from 'proj4';
//import { mrSock } from '../mrsock.js'
import { BaseElement } from './base-element.js'

import Map from 'ol/Map.js';
import View from 'ol/View.js';
import OSM from 'ol/source/OSM.js';
import TileLayer from 'ol/layer/Tile.js';
import { useGeographic } from 'ol/proj.js';
import VectorSource from 'ol/source/Vector.js';
import VectorLayer from 'ol/layer/Vector.js';
import Feature from 'ol/Feature.js';
import Point from 'ol/geom/Point.js';
import { fromLonLat, get as getProjection, addProjection, transform } from 'ol/proj.js';
import { register } from 'ol/proj/proj4.js';
// for adding points
import { Draw } from 'ol/interaction.js';


export class ProjectMap extends BaseElement {
    
    static vectorSource;
    static epsg22717;
    map;
    view;
    static sock;
    
    static {  // static block seemed to run before mrsock got imported in main.js
        
        // this CRS definition seems wrong
        proj4.defs('EPSG:22717', '+proj=utm +zone=17 +datum=WGS84 +units=m +no_defs');
        register(proj4);

        // Add the projection to OpenLayers
        this.epsg22717 = getProjection('EPSG:22717');
        addProjection(ProjectMap.epsg22717);
        
        // Create a vector source
        this.vectorSource = new VectorSource();
        //console.log("vectorSource set:" + this.vectorSource.toString());
    }
    
    static init(mrsock) {
        mrsock.registerCallback("PointAdd", this.mymessage);
        this.sock=mrsock;
    }

    constructor() {
        super();  // can we parameterize: getsSock, getsEntity, getsShared, and factor out the setup of those things?


        //const link = document.createElement('link');
        //link.setAttribute('rel', 'stylesheet');
        //link.setAttribute('href', 'theme/ol.css');
        //this.shadowRoot.appendChild(link);

        const style = document.createElement('style');
        style.innerText = `
            :host {
                display: block;
            }
        `;
        this.shadowRoot.appendChild(style);

        const div = document.createElement('div');
        div.style.width = '100%';
        div.style.height = '100%';
        this.shadowRoot.appendChild(div);

        //useGeographic();
        
        // Function to add a feature
        //function addFeature(lon, lat) {
        //    const feature = new Feature({
        //        geometry: new Point(fromLonLat([lon, lat], ProjectMap.epsg22717))
//                geometry: new Point([lon, lat])
        //    });
        //    this.vectorSource.addFeature(feature);
        //}
        function addFeature17N(lon, lat, vs) {
            const feature = new Feature({
                geometry: new Point([lon, lat]) // set CRS?
//                geometry: new Point([lon, lat])
            });
            vs.addFeature(feature);
        }

        // Example: Adding features to the vector source
        //addFeature(-0.12755, 51.50722); // London
        //addFeature(2.3522, 48.8566); // Paris
        //addFeature(-79.3832, 43.6532); // Toronto
        //addFeature17N(630380.229 , 4834628.614, this.constructor.vectorSource ); // Toronto
        //
        // Create a vector layer
        const vectorLayer = new VectorLayer({
            source: this.constructor.vectorSource
        });

        
        this.view = new View({
//            center: fromLonLat([ -79.3832, 43.6532]),
            center: [630380.229 , 4834628.614],
//            center: [ 0, 0],
            zoom: 7,
            projection: 'EPSG:22717',
        });
        this.map = new Map({
            target: div,
            layers: [
                new TileLayer({
                    source: new OSM(),
                }),
                vectorLayer,
            ],
            view: this.view,
        });
        
        const draw = new Draw({
            source: this.constructor.vectorSource,
            type: 'Point',
        });
        this.map.addInteraction(draw);
        
        draw.on('drawend', function(event) {
            const feature = event.feature; // The drawn feature
            const coordinates = feature.getGeometry().getCoordinates(); // The coordinates of the point
            
            const srid = 22717;
  
            // Construct the geolatte.geom.Point JSON
            const geolattePoint = {
                "type": "Point",
                "coordinates": coordinates,
                "crs": {
                 "type": "name",
                 "properties": {
                    "name": `EPSG:${srid}`
                        }
                }
            };
            
            const pa = new PointAdd();
            pa.proj_id = 38730;  // testing only haha
            pa.point = geolattePoint;
            console.log('Point added:', JSON.stringify(coordinates));
            ProjectMap.sock.send(pa);

        });
        
        //addFeature17N(538924.960, 4821168.712, this.constructor.vectorSource); // Conestogo
        //this.map.addLayer(vectorLayer);
        
   }
    
    static mymessage(payload) {
        //console.log("PointAdd mymessage: " + JSON.stringify(payload));
        if (payload.type === "PointAdd") {
            const feature = new Feature({
                geometry: new Point(payload.point.coordinates),
                labelPoint: new Point(payload.point.coordinates),
                name: 'My Project Site',
            });
            //console.log("coords: " + JSON.stringify(payload.point.coordinates));
            //console.log("vectorSource is:" + ProjectMap.vectorSource);
            ProjectMap.vectorSource.addFeature(feature);
        }
    }
}
customElements.define('proj-map', ProjectMap);
