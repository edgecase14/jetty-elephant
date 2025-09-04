/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.controller;

// out damned spot!
import com.fasterxml.jackson.core.JsonProcessingException;
import net.coplanar.annotations.Controller;
import java.util.List;
import net.coplanar.ents.*;
import net.coplanar.updatemsg.*;
import net.coplanar.elements.*;
import org.eclipse.jetty.util.MultiMap;
import org.hibernate.query.Query;

/**
 *
 * @author jjackson
 */
@Controller
public final class Timesheet extends GenericController {

    private enum TState {
        LOADING,
        EDITING
    }
    private TState timesheet_state = TState.LOADING;
//    private final AtomicInteger messageId = new AtomicInteger(0);
//    private final Deque<SseMessage> messageQueue = new ArrayDeque<>();
    public String pay_period = null;
    public String view_user = null;
    private TsUser vu = null;

    @Override
    public void init() {
        // setup initial pay_period?
        if (vu == null) {
            if (view_user == null) {
                view_user = parent_thread.username;
		System.out.println("init() username: " + view_user);
            }
            vu = hsession.bySimpleNaturalId(TsUser.class).load(view_user);
            if (vu == null) {
                System.out.println("Timesheet init() view_user lookup error: " + view_user);
            } else {
                System.out.println("Timesheet view_user: " + vu.getEmail());
            }
       }
    }
    
    // runs in handler
    public void querySetup(MultiMap queryMap) {

        String url_pay_period;
        String url_user;

        url_pay_period = (String) queryMap.getValue("pay_period");
        url_user = (String) queryMap.getValue("user");
        System.out.println("setup - pay_period = " + url_pay_period + " user = " + url_user);
        if (url_user != null) {
            this.view_user = url_user;
        }
        if (url_pay_period != null) {
            this.pay_period = url_pay_period;
        } else {
            this.pay_period = "2024-06-P1"; // generate from calendar - current pay period
        }

    }

    @Override
    public UpdateMessage queryNavigated(MultiMap<String> queryMap) {

        String url_pay_period;
        String url_user;

        url_pay_period = (String) queryMap.getValue("pay_period");
        url_user = (String) queryMap.getValue("user");
        System.out.println("nav - user = " + url_user);
        var su = new ShowUsername();
        if (url_user != null) {
            su.username = url_user;
            return su;
        }
//        if (url_pay_period != null) {
//            this.pay_period = url_pay_period;
//        } else {
//            this.pay_period = "2024-06-P1"; // generate from calendar - current pay period
//        }
        return null;
    }
    
    // should refactor into superclass
    private void stateReset() throws JsonProcessingException {
        System.out.println("state RESET");

        // clicked reload in browser, reset session state
        // reset output queue
        // remove customElements sent to remote
        TsCellElement.removeAllCells(this);
        // delete them from local storage
        TsCellElement.clearAllCells();
        var rl = new Reload();
        rl.id = 42;
        addResponseObj(rl);
        writeObjList();
        timesheet_state = TState.LOADING;
    }

    @Override
    public void controller(UpdateMessage event) throws JsonProcessingException {
        // can we also take another event type here, with optional URL query string,
        // to start prefetching?
        // query string vars: pay_period, view_user (so admin can edit other's)

        if (event.getClass() == NewPeriod.class) {
            var ev = (NewPeriod) event;
            if (ev.pay_period == null ? pay_period != null : !ev.pay_period.equals(pay_period)) {
                pay_period = ev.pay_period;
                // now reset timesheet
                var pp = new ShowPeriod();
                pp.pay_period = pay_period;
                addResponseObj(pp);
                writeObjList();
            }
        }
        if (event.getClass() == CellList.class ) { // browser side reset
            if (timesheet_state != TState.LOADING) {
                stateReset();
                return;
            }
        }
        if (event.getClass() == ShowUsername.class) {
            var ev = (ShowUsername) event;
            System.out.println("ShowUsername to controller: " + ev.username);
            if (!ev.username.equals(vu.getEmail())) {
                var new_vu = hsession.bySimpleNaturalId(TsUser.class).load(ev.username);
                System.out.println("Timesheet view_user lookup: " + vu.getEmail());
                if (new_vu == null) {
                    System.out.println("Timesheet view_user lookup error");
                } else {
                    vu = new_vu;
                    view_user = ev.username;
                    // temporary
                    var su = new ShowUsername();
                    su.username = vu.getEmail();
                    addResponseObj(su);
                    writeObjList();  // race modifying update list - let stateReset do it so it's done ONCE only
                    stateReset();
                }
            }
            return;
        }
        
        switch (timesheet_state) {
            case LOADING -> {
                System.out.println("state: LOADING");
                // testing only - should load all projects and TsCells for current pay period
                // send a project row
                //List<UserProject> prjs = tu.getProjects();
                //UserProject up;
                //for (UserProject userProj : prjs) {
                //    = userProj.getProject();
                //}

                if (pay_period != null) {
                    var pp = new ShowPeriod();
                    pp.pay_period = pay_period;
                    addResponseObj(pp);
                }

                if (vu.getEmail() != null) {
                    var su = new ShowUsername();
                    su.username = vu.getEmail();
                    addResponseObj(su);
                }

                try {
                    //System.out.println("LOADING: vu project close");
                    //hsession.close();
                    //System.out.println("LOADING: vu project new");
                    //hsession = SrvApp.sf.openSession();
                    //System.out.println("LOADING: vu project query starting tx");
                    //Transaction tx = hsession.beginTransaction();
                    System.out.println("LOADING: vu project query starting create2");
                    // SELECT * FROM project WHERE proj_id IN ( SELECT DISTINCT proj_id FROM tscell NATURAL JOIN project WHERE user_id = 1 )
                    Query<Project> q = hsession.createQuery("FROM Project p WHERE p.proj_id IN (SELECT DISTINCT t.project.proj_id FROM TsCell t WHERE t.tsuser.user_id = :userId AND t.pay_period = '2024-06-P1')", Project.class);
                    System.out.println("LOADING: vu project query created");

                    q.setParameter("userId", vu.getUser_id());
                    System.out.println("LOADING: vu project query parameter set");
                    List<Project> pl = q.getResultList();
                    System.out.println("LOADING: vu project query done");
                    for (Project pj : pl) {
                        GotRow gr = new GotRow();
                        gr.projid = pj.getProj_id();
                        gr.pname = pj.getPname();
                        addResponseObj(gr);

                        System.out.println("LOADING: project-site");
                        ProjectSite site;
                        site = pj.getSite();
                        if (site != null) {
                            System.out.println("LOADING: found site");

                            PointAdd pa = new PointAdd();
                            pa.proj_id = site.getProj().getProj_id();
                            pa.point = site.getPoint();
                            addResponseObj(pa);
                        }

                        Query<TsCell> tscs = hsession.createQuery("FROM TsCell t WHERE t.project.proj_id = :projId AND t.tsuser.user_id = :userId AND t.pay_period = '2024-06-P1'", TsCell.class);
                        tscs.setParameter("projId", pj.getProj_id());
                        tscs.setParameter("userId", vu.getUser_id());
                        List<TsCell> tsc = tscs.getResultList();
                        for (TsCell tc : tsc) {
                            // send project cells
                            // delegate this to a TsCell class
                            // can Entity have the appropriate methods and have Hibernate ignore them?
                            // but they might not be persistable so need to still work - use interface?
                            System.out.println("LOADING: sending te");
                            TsCellElement te = new TsCellElement(tc);
                            te.sendToRemote(this);  // "this" is an implementation detail, hide it in a superclass method?
                            // or have it return a List, then merge it into main UpdateList
                        }

                    }

                } catch (Exception e) {
                    System.out.println(e);
                }

                
                
                System.out.println("LOADING: sending objList -> EDITING");
                writeObjList();
                timesheet_state = TState.EDITING;
            }

            case TState.EDITING -> {
                System.out.println("state: EDITING");
                // could @myJson be used to generate the Jackson polymorphic 
                // @JsonTypeInfo(use=Id.MINIMAL_CLASS, include=As.PROPERTY, property="type") // Include Java class simple-name as JSON property "type"
                // @JsonSubTypes({@Type(Car.class), @Type(Aeroplane.class)}) // Required for deserialization only  
                // could it also generate single or multiple Typescript ts.d declarations, for the referenced classes?
                // generate mjs includes for main.js
                if (event.getClass() == CellUpdate.class) {
                    // @myJson
                    // CellUpdate cellUpdate = (CellUpdate) event;
                    // cellUpdate.onmessage(); ?? throws exception if invalid, processes otherwise
                    //   would update TsCell in db transaction, and notify shared observer, send ACK message, flush send buf
                    // which would generate TscThreadCodec class, to be used in decoding received messages from javascript serialization
                    // it would return type specific objects, validating them and rejecting unknown ones
                    hsession.getTransaction().begin();
                    UpdateMessage um = TsCellElement.onMessage(event, hsession);
                    hsession.getTransaction().commit();  // if fails then ?
                    addResponseObj(um);
                    writeObjList();

                }
                if (event.getClass() == PointAdd.class) {
                    var pa = (PointAdd) event;
                    try {
                        ProjectSite ps = null;
                        hsession.getTransaction().begin();
                        var add_pj = hsession.find(Project.class, pa.proj_id);
                        if (add_pj == null) {
                            System.out.println("invalid proj_id: " + pa.proj_id);
                        } else  {
                            ps = add_pj.getSite();
                        }
                        if (ps == null) {
                            ps = new ProjectSite();
                            ps.setProj(add_pj);
                        }
                        System.out.println(pa.point);
                        // {"type":"Point","crs":{"type":"name","properties":{"name":"EPSG:-1"}},"coordinates":[537000.6946750396,4814131.637027588]}}
                        ps.setPoint(pa.point);
                        hsession.persist(ps);
                        hsession.getTransaction().commit();  // if fails then ?
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                // event class for controller itsself? or related mode change customElement propagates events back here?
                // if (event.getClass() == ModeChange.class {
                //     new_mode = modeChange.onmsg();
                //     if new_mode == "submitted for review" { 
                //     this.mode = new_mode;
                //     tell browser no more changes
                //     call BPMN state machine etc.

                // should throw msg on a queue, so it can be re-sent
                // if browser EventSource reconnects, 
                // and track msg# for idempotent retries
            }
            default ->
                throw new AssertionError(timesheet_state.name());
        }
    }
}
