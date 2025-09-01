package net.coplanar.ents;

import java.io.Serializable;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.NaturalId;
//import org.hibernate.annotations.*;


/**
 * Entity implementation class for Entity: Users
 *
 */
@Entity
// https://vladmihalcea.com/the-best-way-to-map-a-naturalid-business-key-with-jpa-and-hibernate/
//@org.hibernate.annotations.Cache(
//	    usage = CacheConcurrencyStrategy.READ_WRITE
//	)
//@NaturalIdCache
public class TsUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int user_id;
    
    @Column(unique = true, length = 25)
    private String username;
    
    @ColumnDefault("0.04")
    private float vac_rate;

    @ColumnDefault("false")
    private boolean active;
    
    @NaturalId
    @Column(nullable = false, unique = true, length = 100)
    private String qb_email;
    
    @ColumnDefault("false")
    private boolean salaried;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "prov")
    @ColumnDefault("'ON'")
    private Province prov;
    
    @Column(unique = true, length = 25)
    private String qb_emp_list_id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String it_username;
    
    @Column(nullable = true)
    private Integer sv_user_id;
    
    @Column(length = 15)
    private String pref_num;
    
    @ColumnDefault("false")
    private boolean management;
    
    @ColumnDefault("40")
    private float hours_limit;
    
    @ColumnDefault("24")
    private int personal_time;
    
    @Column(unique = true, length = 99)
    private String qb_emp_acct_no;
    
    //@OneToMany(
    //        mappedBy = "user_id",
    //        cascade = CascadeType.ALL,
    //        orphanRemoval = true,
    //        fetch = FetchType.EAGER
    //)
    //private List<UserProject> userProjects = new ArrayList<>();

    public TsUser() {
        super();
    }

    public int getUser_id() {
        return this.user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getQbUsername() {
        return this.username;
    }

    public void setQbUsername(String username) {
        this.username = username;
    }

    public String getItUsername() {
        return this.it_username;
    }

    public void setItUsername(String username) {
        this.username = it_username;
    }

    public String getEmail() {
        return this.qb_email;
    }

    public void setEmail(String email) {
        this.qb_email = email;
    }

    
    
    public float getVacRate() {
        return this.vac_rate;
    }

    public void setVacRate(float vac_rate) {
        this.vac_rate = vac_rate;
    }

    //public List<UserProject> getProjects() {
    //    return this.userProjects;
    //}
   
}
