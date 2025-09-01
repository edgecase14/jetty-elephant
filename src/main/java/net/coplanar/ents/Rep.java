package net.coplanar.ents;

import java.io.Serializable;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

/**
 * Entity implementation class for Entity: Rep
 *
 */
@Entity

public class Rep implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int rep_id;

    @NaturalId
    @Column(nullable = false, length = 5)
    private String rep_name;

    @Column(nullable = true, length = 25)
    private String rep_qb_list_id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rep_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "user_id_fk"))
    private TsUser rep_user_id; // prod db missing unique constraint - TODO

    public Rep() {
        super();
    }

    public int getRep_id() {
        return this.rep_id;
    }

    public void setRep_id(int rep_id) {
        this.rep_id = rep_id;
    }

    public String getRep_name() {
        return this.rep_name;
    }

    public void setRep_name(String rep_name) {
        this.rep_name = rep_name;
    }

}
