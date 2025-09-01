package net.coplanar.ents;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity implementation class for Entity: UserProject
 *
 */
@Entity(name="user_project")

public class UserProject implements Serializable {

	   
	@Id
	@GeneratedValue ( strategy = GenerationType.IDENTITY )
	private int id;
	private static final long serialVersionUID = 1L;
	
	@ManyToOne
	private TsUser user_id;

	@ManyToOne
	private Project proj_id;

	public UserProject() {
		super();
	}   
//	public int getId() {
//		return this.id;
//	}

//	public void setId(int id) {
//		this.id = id;
//	}   
	public TsUser getTsUser() {
		return this.user_id;
	}

	public void setUser(TsUser user) {
		this.user_id = user;
	}   
	public Project getProject() {
		return this.proj_id;
	}

	public void setProj(Project proj) {
		this.proj_id = proj;
	}
   
}
