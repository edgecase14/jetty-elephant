package net.coplanar.beanz;

import org.hibernate.Session;

import net.coplanar.ents.*;
import java.io.Serializable;

public class TsUserBean {
    
    /**
     * Default constructor. 
     */
    public TsUserBean() {
        
    }
    public static TsUser getUser(Session hsession, int user_id ) {
    	TsUser user = hsession.find(TsUser.class, user_id);
        //TsUser user = new TsUser();
        //user.setUsername("foobar");
        return user;
    }
    public static TsUser getUserFromUsername(Session hsession, String username) {
    	// thanks to:
    	TsUser user = hsession.bySimpleNaturalId(TsUser.class)
    			.load(username);
    	return user;
    }
}
