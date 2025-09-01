package net.coplanar.ents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.sql.Types;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;

/**
 * Entity implementation class for Entity: Projects
 *
 */
@Entity
@Table( name = "cxlist" )
public class Project implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int proj_id;

    @NaturalId
    @NotNull
    @Column( nullable = false, length = 20 )
    private String job_id;

    @NotNull
    @Column(name = "job_name", nullable = false)
    private String pname;

    @ManyToOne
    @JoinColumn(name = "rep_id", foreignKey = @ForeignKey(name = "rep_id_fk"))
    private Rep rep_id;

    @NotNull // not affecting schema generation, use nullable = false, maybe it is validator-only?
    @Column( nullable = false, length = 2, columnDefinition = "CHARACTER(2)" ) // default false
    //@JdbcTypeCode(Types.CHAR)
    private String province;
    
    @Null
    @Column(nullable = true)
    private Boolean ts_active;
    
    @NotNull // not affecting schema generation, use nullable = false, maybe it is validator-only?
    @Column(nullable = false) // default false
    @ColumnDefault("FALSE")
    private boolean qb_active;

    //@OneToMany(
    //        mappedBy = "proj_id",
    //        cascade = CascadeType.ALL,
    //        orphanRemoval = true
    //)
    //private final List<UserProject> projectUsers = new ArrayList<>();

    @OneToOne(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private ProjectSite proj_site;


    public int getProj_id() {
        return this.proj_id;
    }

    public void setProj_id(int proj_id) {
        this.proj_id = proj_id;
    }

    public String getPname() {
        return this.pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getJobId() {
        return this.job_id;
    }

    public void setJobId(String job_id) {
        this.job_id = job_id;
    }

    public Rep getRep() {
        return this.rep_id;
    }

    public void setRep(Rep rep) {
        this.rep_id = rep;
    }

    public ProjectSite getSite() {
        return this.proj_site;
    }

    public void addSite(ProjectSite site) {
        proj_site.setProj(this);
        this.proj_site = site;
    }

    public void removeSite() {
        if (proj_site != null) {
            proj_site.setProj(null);
            this.proj_site = null;
        }
    }
}
