/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.coplanar.ents.TsUser;
import net.coplanar.updatemsg.UpdateMessage;
import org.hibernate.Session;

/**
 *
 * @author jjackson
 */
public abstract class GenericController {
    
    Session hsession;
    GenericThread parent_thread;
    TsUser user = null;

    protected abstract void init(); // runs in GenericThread

    protected final void setUser(TsUser user) {
        this.user = user;
    }
    
    protected final void setHsession(Session hsession) {
        this.hsession = hsession;
    }
    
    protected final void setParentThread(GenericThread pt) {
        this.parent_thread = pt;
    }
    
    public final void addResponseObj(UpdateMessage um) {
        parent_thread.addResponseObj(um);
    }
    
    protected final void writeObjList() throws JsonProcessingException {
        parent_thread.writeObjList();
    }

    protected abstract void controller(UpdateMessage event) throws JsonProcessingException;
}
