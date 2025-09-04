/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.coplanar.ents.TsUser;
import net.coplanar.updatemsg.UpdateMessage;
import net.coplanar.app.GenericThread;
import org.eclipse.jetty.util.MultiMap;
import org.hibernate.Session;

/**
 *
 * @author jjackson
 */
public abstract class GenericController {
    
    protected Session hsession;
    protected GenericThread parent_thread;
    TsUser user = null;

    public abstract void init(); // runs in GenericThread

    public void querySetup(MultiMap<String> queryMap) {
        // Default implementation does nothing, can be overridden by subclasses
    }

    public UpdateMessage queryNavigated(MultiMap<String> queryMap) {
        // Default implementation returns null, can be overridden by subclasses
        return null;
    }

    public final void setUser(TsUser user) {
        this.user = user;
    }
    
    public final void setHsession(Session hsession) {
        this.hsession = hsession;
    }
    
    public final void setParentThread(GenericThread pt) {
        this.parent_thread = pt;
    }
    
    public final void addResponseObj(UpdateMessage um) {
        parent_thread.addResponseObj(um);
    }
    
    protected final void writeObjList() throws JsonProcessingException {
        parent_thread.writeObjList();
    }

    public abstract void controller(UpdateMessage event) throws JsonProcessingException;
}
