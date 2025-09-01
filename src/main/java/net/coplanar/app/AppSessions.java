/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jjackson
 */
public class AppSessions {

    private final Map<String, Runnable> sessionThreads = new ConcurrentHashMap<>();

    public final GenericThread getThread(String sid) {
        return (GenericThread) sessionThreads.get(sid);
    }
    
    public final void putThread(String sid, GenericThread thread) {
        sessionThreads.put(sid, thread);
    }
    
    public final void removeThread(String sid) {
        sessionThreads.remove(sid);
    }
}
