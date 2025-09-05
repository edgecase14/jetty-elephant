/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.coplanar.ents.TsUser;
import net.coplanar.updatemsg.UpdateMessage;
import net.coplanar.updatemsg.CellList;
import net.coplanar.updatemsg.Reload;
import net.coplanar.app.GenericThread;
import org.eclipse.jetty.util.MultiMap;
import org.hibernate.Session;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author jjackson
 */
public abstract class GenericController {
    
    public enum FEState {
        LOADING,
        RUNNING
    }
    
    protected Session hsession;
    protected GenericThread parent_thread;
    TsUser user = null;
    private FEState frontend_state = FEState.LOADING;
    private List<Runnable> stateResetCleanupCallbacks = new ArrayList<>();

    // Getters and setters for frontend_state
    public FEState getFrontendState() {
        return frontend_state;
    }

    public void setFrontendState(FEState state) {
        this.frontend_state = state;
    }

    // Method to add cleanup callbacks
    public void addStateResetCleanupCallback(Runnable callback) {
        stateResetCleanupCallbacks.add(callback);
    }

    // State reset method - common structure for all controllers
    protected void stateReset() throws JsonProcessingException {
        System.out.println("state RESET");

        // clicked reload in browser, reset session state
        // reset output queue
        // remove customElements sent to remote
        for (Runnable callback : stateResetCleanupCallbacks) {
            callback.run();
        }
        
        var rl = new Reload();
        rl.id = 42;
        addResponseObj(rl);
        writeObjList();
        setFrontendState(FEState.LOADING);
    }

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

    // Abstract methods for handling different frontend states
    public abstract void handleLoadingState() throws JsonProcessingException;
    public abstract void handleRunningState(UpdateMessage event) throws JsonProcessingException;


    public final void controller(UpdateMessage event) throws JsonProcessingException {
        // Handle CellList for browser side reset - common to all controllers
        if (event.getClass() == CellList.class) { 
            if (getFrontendState() != FEState.LOADING) {
                stateReset();
                return;
            }
        }
        
        // Handle frontend state transitions
        switch (getFrontendState()) {
            case LOADING -> handleLoadingState();
            case RUNNING -> handleRunningState(event);
            default -> throw new AssertionError(getFrontendState().name());
        }
    }
}
