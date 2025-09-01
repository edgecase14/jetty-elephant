/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.coplanar.app.GenericController;
import net.coplanar.ents.TsCell;
import net.coplanar.updatemsg.*;
import org.hibernate.Session;

/**
 *
 * @author jjackson
 */
public class TsCellElement {
    private static Map<Integer, TsCellElement> tscells = new HashMap<>();
    private TsCell tscell;
    
    public TsCellElement(TsCell tscell) {
        // not sure we should hold a reference to a Hibernate entity
        this.tscell = tscell;
    }
    
    public void sendToRemote(GenericController out) {
        CellAdd cel = new CellAdd();
        cel.projid = tscell.getProject().getProj_id();
        cel.date = tscell.getDate().toString();
        cel.cellid = tscell.getId();
        tscells.put(cel.cellid, this);
        out.addResponseObj(cel);
        
        // should it be CellInit (before) ?
        CellUpdate cu = new CellUpdate();
        cu.id = tscell.getId();
        cu.contents = tscell.getEntry();
        cu.note = tscell.getNote();
        out.addResponseObj(cu);


    }
    
    public static UpdateMessage onMessage(UpdateMessage event, Session hsession) {

        // validate message type(s) with switch/default error
        CellUpdate ev = (CellUpdate) event;
        return tscells.get(ev.id).onMyMessage(event, hsession);
    }
    
    public UpdateMessage onMyMessage(UpdateMessage event, Session hsession) {
        CellUpdate ev = (CellUpdate) event;
        TsCell tc = hsession.find(TsCell.class, ev.id);
        System.out.println("CellUpdate: " + ev.id);
        tc.setEntry(ev.contents);
        tc.setNote(ev.note);
        hsession.persist(tc);
        CellAck msg = new CellAck();
        msg.id = ev.id;
        return msg;

    }

    public void removeFromRemote(GenericController out) {
        // send remove message for it, then remove from Map
    }
    
    public static void removeAllCells(GenericController out) {
        // iterate tscells and send remove message for each, then remove from Map
        for (Map.Entry<Integer, TsCellElement> te : tscells.entrySet()) {
            TsCellElement rem = te.getValue();
                    rem.removeFromRemote(out);
        }
        //clearAllCells();
    }
    
    public static void clearAllCells() {
        // clear the tscells map
        tscells.clear();
    }
}
